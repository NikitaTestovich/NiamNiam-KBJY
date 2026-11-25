"""Check publication candidates for local artifacts and common secret formats."""

from codecs import BOM_UTF16_BE, BOM_UTF16_LE
from dataclasses import dataclass
import json
from pathlib import Path, PurePosixPath
import re
import subprocess


RULES = {
    "private-key": re.compile(rb"-----BEGIN (?:[A-Z0-9]+ )*PRIVATE KEY-----"),
    "github-token": re.compile(rb"\b(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{35,})\b"),
    "google-credential": re.compile(rb"\b(?:AIza[0-9A-Za-z_-]{35}|ya29\.[A-Za-z0-9_-]{25,}|GOCSPX-[A-Za-z0-9_-]{24,})\b"),
    "aws-key": re.compile(rb"\b(?:AKIA|ASIA)[A-Z0-9]{16}\b"),
    "slack-token": re.compile(rb"\bxox[baprs]-[0-9A-Za-z-]{15,}\b"),
    "credential-url": re.compile(rb"https?://[^\s:/@]{1,128}:[^\s/@]{4,128}@"),
    "apps-script-deployment": re.compile(rb"script\.google\.com/macros/s/[\w-]{35,}/(?:exec|dev)\b"),
    "google-sheet-id": re.compile(rb"docs\.google\.com/spreadsheets/d/[\w-]{20,}"),
}
LOCAL_FILE = re.compile(
    r"(?:^|/)(?:\.env(?:\.[^/]+)?|local\.properties|keystore\.properties|"
    r"secrets\.properties|credentials\.json|google-services\.json|\.clasp(?:rc)?\.json)$"
    r"|\.(?:apk|aab|apks|idsig|jks|keystore|pem|p12|pfx|key|xlsx|(?:db|sqlite3?)(?:-[^/]+)?)$",
    re.IGNORECASE,
)
LOCAL_DIRECTORIES = {".android", "backups", "captures", "exports", "private"}


@dataclass(frozen=True)
class Finding:
    scope: str
    path: str
    rule: str
    line: int | None = None


def inspect_content(payload: bytes) -> list[tuple[str, int]]:
    if payload.startswith((BOM_UTF16_BE, BOM_UTF16_LE)):
        payload = payload.decode("utf-16", errors="replace").encode("utf-8")
    findings = []
    for rule, pattern in RULES.items():
        match = pattern.search(payload)
        if match is not None:
            findings.append((rule, payload.count(b"\n", 0, match.start()) + 1))
    return findings


def git(root: Path, *args: str) -> bytes:
    return subprocess.check_output(["git", *args], cwd=root)


def paths(raw: bytes) -> set[str]:
    return {part.decode("utf-8", errors="surrogateescape") for part in raw.split(b"\0") if part}


def verify_public_files(root: Path) -> tuple[int, list[Finding]]:
    candidates = paths(git(root, "ls-files", "-co", "--exclude-standard", "-z"))
    ignored_in_index = paths(git(root, "ls-files", "-ci", "--exclude-standard", "-z"))
    staged = paths(git(root, "diff", "--cached", "--name-only", "--diff-filter=ACMR", "-z"))
    findings = []
    for name in sorted(candidates):
        path = root / name
        directories = PurePosixPath(name).parts[:-1]
        if name in ignored_in_index or LOCAL_FILE.search(name) or LOCAL_DIRECTORIES.intersection(directories):
            findings.append(Finding("files", name, "local-artifact"))
            continue
        if path.is_symlink():
            findings.append(Finding("working copy", name, "symlink"))
            continue
        working = path.read_bytes() if path.is_file() else None
        if working is not None:
            findings.extend(Finding("working copy", name, rule, line) for rule, line in inspect_content(working))
        if name in staged:
            indexed = git(root, "show", f":{name}")
            if indexed != working:
                findings.extend(Finding("index", name, rule, line) for rule, line in inspect_content(indexed))
    return len(candidates), findings


if __name__ == "__main__":
    count, findings = verify_public_files(Path(__file__).resolve().parents[2])
    for finding in findings:
        location = json.dumps(finding.path, ensure_ascii=True)
        if finding.line is not None:
            location += f":{finding.line}"
        print(f"{finding.scope}: {finding.rule} in {location}")
    print(f"Checked {count} publication candidates. Findings: {len(findings)}.")
    raise SystemExit(bool(findings))
