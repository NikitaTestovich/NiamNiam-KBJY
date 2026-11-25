"""Fail closed when Gradle did not actually execute successful tests."""
import argparse
from pathlib import Path
import re
import xml.etree.ElementTree as ET


def identifier(value: str, fallback: str) -> str:
    return value if re.fullmatch(r"[A-Za-z0-9_.$-]{1,200}", value) else fallback


def test_failure(case: ET.Element, result: ET.Element) -> str:
    class_name = identifier(case.get("classname", ""), "unknown-class")
    method = case.get("name", "").split("[", 1)[0].split("(", 1)[0]
    name = f"{class_name}#{identifier(method, 'unnamed-test')}"
    # Gradle sometimes includes the exception class only at the start of the failure text.
    exception = result.get("type") or (result.text or "").strip().partition(":")[0]
    exception = identifier(exception, result.tag) if exception.endswith(("Exception", "Error")) else result.tag
    return f"{name} ({exception})"


def verify(directory: Path) -> tuple[int, int]:
    files = sorted(directory.rglob("*.xml")) if directory.is_dir() else []
    executed = skipped = 0
    failures = []
    for path in files:
        root = ET.parse(path).getroot()
        cases = list(root.iter("testcase"))
        executed += sum(case.find("skipped") is None for case in cases)
        skipped += sum(case.find("skipped") is not None for case in cases)
        file_failures = []
        for case in cases:
            result = case.find("failure")
            if result is None:
                result = case.find("error")
            if result is not None:
                file_failures.append(test_failure(case, result))
        # Some runner/setup failures are recorded only in suite attributes.
        suites = [root, *root.iter("testsuite")]
        if not file_failures and any(int(suite.get("failures", "0")) or int(suite.get("errors", "0")) for suite in suites):
            file_failures.append(f"Runner error in {identifier(path.name, 'test report')}")
        failures.extend(file_failures)
    if failures:
        details = "\n".join(f"- {failure}" for failure in failures[:20])
        if len(failures) > 20:
            details += f"\n- {len(failures) - 20} more entries in the test reports"
        raise ValueError(f"Failed tests or runner errors found in {directory}:\n{details}")
    if not files or executed == 0:
        raise ValueError(f"No executed tests found in {directory}")
    return executed, skipped


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directory", type=Path)
    args = parser.parse_args()
    try:
        count, skipped_count = verify(args.directory)
    except (OSError, ValueError, ET.ParseError) as error:
        parser.exit(1, f"{error}\n")
    print(f"Verified {count} executed tests; {skipped_count} skipped.")
