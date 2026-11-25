from pathlib import Path
import subprocess
import tempfile
import unittest

from verify_public_files import inspect_content, verify_public_files


class VerifyPublicFilesTest(unittest.TestCase):
    def repository(self) -> Path:
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        root = Path(directory.name)
        subprocess.run(["git", "init", "--quiet", str(root)], check=True)
        return root

    def test_detects_private_endpoint_and_token_without_returning_values(self):
        endpoint = b"https://script.google.com/macros/s/" + b"x" * 40 + b"/exec"
        token = b"ghp_" + b"1" * 36
        self.assertEqual(
            [("apps-script-deployment", 1), ("github-token", 2)],
            sorted(inspect_content(endpoint + b"\n" + token)),
        )

    def test_detects_utf16_credentials_and_allows_documentation_placeholder(self):
        key = ("-----BEGIN " + "PRIVATE KEY-----").encode("utf-16")
        self.assertEqual([("private-key", 1)], inspect_content(key))
        example = "https://script.google.com/macros/s/…/exec\n${{ secrets.SIGNING_STORE_PASSWORD }}"
        self.assertEqual([], inspect_content(example.encode()))

    def test_detects_forced_tracked_artifact_and_ignores_local_export(self):
        root = self.repository()
        (root / ".gitignore").write_text("*.xlsx\ncaptures/\n", encoding="utf-8")
        (root / "personal.xlsx").write_bytes(b"local export")
        (root / "captures").mkdir()
        screenshot = root / "captures" / "screenshot.png"
        screenshot.write_bytes(b"local screenshot")
        subprocess.run(["git", "add", "-f", "captures/screenshot.png"], cwd=root, check=True)
        _, findings = verify_public_files(root)
        self.assertEqual(["captures/screenshot.png"], [item.path for item in findings])

    def test_detects_staged_secret_removed_from_working_copy(self):
        root = self.repository()
        file = root / "config.txt"
        file.write_bytes(b"ghp_" + b"2" * 36)
        subprocess.run(["git", "add", "config.txt"], cwd=root, check=True)
        file.write_text("clean", encoding="utf-8")
        _, findings = verify_public_files(root)
        self.assertEqual([("index", "github-token")], [(item.scope, item.rule) for item in findings])

    def test_blocks_a_database_sidecar_when_the_ignore_rule_is_missing(self):
        root = self.repository()
        (root / "diary.sqlite3-wal").write_bytes(b"\x00")
        _, findings = verify_public_files(root)
        self.assertEqual([("diary.sqlite3-wal", "local-artifact")], [(item.path, item.rule) for item in findings])
