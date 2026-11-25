"""Fail closed when Gradle did not actually execute successful tests."""
import argparse
from pathlib import Path
import xml.etree.ElementTree as ET


def verify(directory: Path) -> tuple[int, int]:
    files = list(directory.rglob("*.xml")) if directory.is_dir() else []
    executed = skipped = failures = 0
    for path in files:
        root = ET.parse(path).getroot()
        cases = list(root.iter("testcase"))
        executed += sum(case.find("skipped") is None for case in cases)
        skipped += sum(case.find("skipped") is not None for case in cases)
        failures += sum(case.find("failure") is not None or case.find("error") is not None for case in cases)
        # Some runner/setup failures are recorded only in suite attributes.
        for suite in root.iter("testsuite"):
            if int(suite.get("failures", "0")) or int(suite.get("errors", "0")):
                failures += 1
        if root.tag == "testsuites" and (int(root.get("failures", "0")) or int(root.get("errors", "0"))):
            failures += 1
    if not files or executed == 0:
        raise ValueError(f"No executed tests found in {directory}")
    if failures:
        raise ValueError(f"Failed tests or runner errors found in {directory}")
    return executed, skipped


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directory", type=Path)
    args = parser.parse_args()
    count, skipped_count = verify(args.directory)
    print(f"Verified {count} executed tests; {skipped_count} skipped.")
