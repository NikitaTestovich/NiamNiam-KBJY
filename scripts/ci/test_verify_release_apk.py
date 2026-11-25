from pathlib import Path
import tempfile
import unittest
from zipfile import ZipFile

from verify_release_apk import verify_archive, verify_badging


BADGING = "package: name='com.niamniam.kbjy' versionCode='1' versionName='0.5' compileSdkVersion='37'\n"
ARM_LIBRARIES = {
    "lib/arm64-v8a/libscanner.so": b"arm64 library",
    "lib/armeabi-v7a/libscanner.so": b"arm library",
}


class VerifyReleaseApkTest(unittest.TestCase):
    def apk(self, files: dict[str, bytes]) -> Path:
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        path = Path(directory.name) / "niamniam-kbjy-vc1.apk"
        with ZipFile(path, "w") as archive:
            archive.writestr("AndroidManifest.xml", b"compiled manifest")
            archive.writestr("classes.dex", b"code")
            for name, value in (ARM_LIBRARIES | files).items():
                archive.writestr(name, value)
        return path

    def test_badging_matches_the_publish_inputs(self):
        verify_badging(BADGING, "0.5", 1)
        for badging in (
            "",
            BADGING.replace("com.niamniam.kbjy", "com.niamniam.kbjy.devicetest"),
            BADGING.replace("versionCode='1'", "versionCode='2'"),
            BADGING.replace("versionName='0.5'", "versionName='0.6'"),
            BADGING + "application-debuggable\n",
        ):
            with self.subTest(badging=badging), self.assertRaises(ValueError):
                verify_badging(badging, "0.5", 1)

    def test_clean_archive_allows_offline_models_and_licenses(self):
        verify_archive(self.apk({"assets/tessdata/rus.traineddata": b"\x00model",
                                 "assets/licenses/Apache-2.0.txt": b"license"}))

    def test_archive_rejects_extra_native_abi(self):
        with self.assertRaisesRegex(ValueError, "APK native ABIs"):
            verify_archive(self.apk({"lib/x86_64/libscanner.so": b"x86 library"}))

    def test_archive_requires_both_arm_abis(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "arm64-only.apk"
            with ZipFile(path, "w") as archive:
                archive.writestr("lib/arm64-v8a/libscanner.so", b"arm64 library")
            with self.assertRaisesRegex(ValueError, "APK native ABIs"):
                verify_archive(path)

    def test_archive_rejects_test_or_private_payload(self):
        for name in ("assets/ocr/label.png", "assets/basic-products-ru.tsv", "assets/diary.sqlite-wal"):
            with self.subTest(name=name), self.assertRaisesRegex(ValueError, "Unexpected APK entry"):
                verify_archive(self.apk({name: b"local data"}))

    def test_archive_rejects_embedded_endpoint_without_showing_it(self):
        endpoint = b"https://script.google.com/macros/s/" + b"x" * 40 + b"/exec"
        with self.assertRaisesRegex(ValueError, "apps-script-deployment") as failed:
            verify_archive(self.apk({"classes2.dex": b"\x00" + endpoint}))
        self.assertNotIn(endpoint.decode(), str(failed.exception))

    def test_archive_rejects_damaged_payload(self):
        path = self.apk({})
        path.write_bytes(path.read_bytes().replace(b"code", b"fail", 1))
        with self.assertRaisesRegex(ValueError, "Corrupt APK entry"):
            verify_archive(path)
