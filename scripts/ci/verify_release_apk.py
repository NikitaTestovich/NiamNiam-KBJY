"""Verify the identity and contents of the APK prepared for GitHub Releases."""

import argparse
import json
from pathlib import Path
import re
import subprocess
from zipfile import BadZipFile, ZipFile

from verify_public_files import LOCAL_FILE, inspect_content


APPLICATION_ID = "com.niamniam.kbjy"
TEST_ASSETS = {"assets/basic-products-ru.tsv", "assets/prepared-dishes-ru.tsv"}
RELEASE_ABIS = {"arm64-v8a", "armeabi-v7a"}


def verify_badging(badging: str, version_name: str, version_code: int) -> None:
    lines = badging.splitlines()
    package_lines = [line for line in lines if line.startswith("package:")]
    if len(package_lines) != 1:
        raise ValueError("Missing or ambiguous APK manifest identity")
    identity = dict(re.findall(r"\b(name|versionCode|versionName)='([^']*)'", package_lines[0]))
    expected = {"name": APPLICATION_ID, "versionName": version_name, "versionCode": str(version_code)}
    if identity != expected:
        raise ValueError("APK package or version does not match the release inputs")
    if any(line.startswith("application-debuggable") for line in lines):
        raise ValueError("Release APK is debuggable")


def verify_archive(path: Path) -> None:
    with ZipFile(path) as archive:
        damaged = archive.testzip()
        if damaged is not None:
            raise ValueError(f"Corrupt APK entry: {json.dumps(damaged)}")
        abis = set()
        for entry in archive.infolist():
            name = entry.filename
            library = re.fullmatch(r"lib/([^/]+)/[^/]+\.so", name)
            if library:
                abis.add(library.group(1))
            if name.startswith("assets/ocr/") or name in TEST_ASSETS or LOCAL_FILE.search(name):
                raise ValueError(f"Unexpected APK entry: {json.dumps(name)}")
            if not entry.is_dir() and (name.startswith("assets/") or name.endswith(".dex")):
                findings = inspect_content(archive.read(entry))
                if findings:
                    rules = ", ".join(rule for rule, _ in findings)
                    raise ValueError(f"Unexpected {rules} in APK entry {json.dumps(name)}")
        if abis != RELEASE_ABIS:
            raise ValueError(f"APK native ABIs must be {sorted(RELEASE_ABIS)}, found {sorted(abis)}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("--aapt2", type=Path, required=True)
    parser.add_argument("--version-name", required=True)
    parser.add_argument("--version-code", type=int, required=True)
    args = parser.parse_args()
    try:
        if args.apk.name != f"niamniam-kbjy-vc{args.version_code}.apk":
            raise ValueError("APK filename does not match the release version code")
        badging = subprocess.check_output(
            [str(args.aapt2), "dump", "badging", str(args.apk)], encoding="utf-8"
        )
        verify_badging(badging, args.version_name, args.version_code)
        verify_archive(args.apk)
    except (OSError, ValueError, BadZipFile, subprocess.CalledProcessError) as error:
        parser.exit(1, f"Release APK verification failed: {error}\n")
    print(f"Verified {APPLICATION_ID} {args.version_name} (versionCode {args.version_code}).")


if __name__ == "__main__":
    main()
