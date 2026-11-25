"""Select the previous stable Android APK and compare verified signer reports."""

import argparse
import json
from pathlib import Path
import re


APK_NAME = re.compile(r"niamniam-kbjy-vc([1-9][0-9]*)\.apk")
CERTIFICATE = re.compile(r"^Signer #1 certificate SHA-256 digest: ([0-9a-fA-F]{64})$", re.MULTILINE)


def previous_release_asset(pages: list[list[dict]], pending_code: int) -> tuple[int, int] | None:
    if not isinstance(pages, list) or any(not isinstance(page, list) for page in pages):
        raise ValueError("Unexpected GitHub releases response")
    candidates = []
    for page in pages:
        for release in page:
            if release["draft"] or release["prerelease"]:
                continue
            for asset in release["assets"]:
                match = APK_NAME.fullmatch(asset["name"])
                if match is not None:
                    asset_id = asset["id"]
                    if type(asset_id) is not int or asset_id <= 0:
                        raise ValueError("Invalid previous APK asset ID")
                    candidates.append((int(match[1]), asset_id))
    if not candidates:
        return None
    previous_code = max(code for code, _ in candidates)
    if pending_code <= previous_code:
        raise ValueError("Release versionCode must be greater than the previous stable APK")
    newest = [candidate for candidate in candidates if candidate[0] == previous_code]
    if len(newest) != 1:
        raise ValueError("Previous stable APK is ambiguous for the latest version code")
    return newest[0]


def certificate_sha256(report: str) -> str:
    digests = CERTIFICATE.findall(report)
    if "Number of signers: 1" not in report.splitlines() or len(digests) != 1:
        raise ValueError("The APK must have exactly one verified signing certificate")
    return digests[0].lower()


def verify_signers(current: str, previous: str | None = None) -> None:
    digest = certificate_sha256(current)
    if previous is not None and digest != certificate_sha256(previous):
        raise ValueError("The APK has a different signing certificate from the previous stable release")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    select = commands.add_parser("previous")
    select.add_argument("pages", type=Path)
    select.add_argument("version_code", type=int)
    verify = commands.add_parser("verify")
    verify.add_argument("current", type=Path)
    verify.add_argument("previous", type=Path, nargs="?")
    args = parser.parse_args()
    try:
        if args.command == "previous":
            selected = previous_release_asset(json.loads(args.pages.read_text("utf-8")), args.version_code)
            if selected is not None:
                print(*selected)
        else:
            previous = args.previous.read_text("utf-8") if args.previous is not None else None
            verify_signers(args.current.read_text("utf-8"), previous)
            print("Verified continuity with the previous stable APK." if previous is not None
                  else "Verified a single signer for the first release.")
    except (OSError, KeyError, TypeError, ValueError) as error:
        parser.exit(1, f"Release signing check failed: {error}\n")


if __name__ == "__main__":
    main()
