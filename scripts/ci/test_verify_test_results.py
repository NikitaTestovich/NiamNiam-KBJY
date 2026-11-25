from pathlib import Path
import tempfile
import unittest
from verify_test_results import verify


class VerifyResultsTest(unittest.TestCase):
    def report(self, text):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        directory = Path(temporary.name)
        (directory / "TEST-example.xml").write_text(text, encoding="utf-8")
        return directory

    def test_no_results_fails(self):
        with tempfile.TemporaryDirectory() as temporary:
            with self.assertRaises(ValueError):
                verify(Path(temporary))

    def test_empty_or_all_skipped_results_fail(self):
        for xml in ('<testsuite tests="0"/>', '<testsuite><testcase><skipped/></testcase></testsuite>'):
            with self.subTest(xml=xml), self.assertRaises(ValueError):
                verify(self.report(xml))

    def test_nested_junit_counts_each_case_once(self):
        directory = self.report('<testsuites><testsuite><testcase/><testcase/></testsuite></testsuites>')
        self.assertEqual((2, 0), verify(directory))

    def test_case_and_runner_failures_fail(self):
        for xml in ('<testsuite><testcase><failure/></testcase></testsuite>', '<testsuite errors="1"><testcase/></testsuite>', '<testsuites errors="1"><testsuite><testcase/></testsuite></testsuites>'):
            with self.subTest(xml=xml), self.assertRaises(ValueError):
                verify(self.report(xml))

    def test_malformed_report_fails(self):
        with self.assertRaises(Exception):
            verify(self.report('not xml'))

    def test_failure_names_the_test_without_printing_its_payload(self):
        directory = self.report(
            '<testsuites failures="1"><testsuite failures="1">'
            '<testcase classname="com.niamniam.ScannerTest" name="readsLabel[private input]">'
            '<failure type="java.lang.AssertionError" message="private label">private data</failure>'
            '</testcase></testsuite></testsuites>'
        )
        with self.assertRaises(ValueError) as failure:
            verify(directory)
        message = str(failure.exception)
        self.assertEqual(1, message.count("com.niamniam.ScannerTest#readsLabel"))
        self.assertIn("java.lang.AssertionError", message)
        self.assertNotIn("private", message)

    def test_report_identifies_runner_only_errors(self):
        directory = self.report('<testsuites errors="1"><testsuite/></testsuites>')
        with self.assertRaisesRegex(ValueError, "Runner error in TEST-example.xml"):
            verify(directory)


if __name__ == "__main__":
    unittest.main()
