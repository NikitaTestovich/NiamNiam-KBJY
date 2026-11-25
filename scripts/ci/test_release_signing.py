import unittest

from release_signing import previous_release_asset, verify_signers


def release(version_code, asset_id, *, draft=False, prerelease=False):
    return {
        "draft": draft,
        "prerelease": prerelease,
        "assets": [{"id": asset_id, "name": f"niamniam-kbjy-vc{version_code}.apk"}],
    }


def certificate(digest):
    return f"Verifies\nNumber of signers: 1\nSigner #1 certificate SHA-256 digest: {digest}\n"


class ReleaseSigningTest(unittest.TestCase):
    def test_first_release_needs_no_previous_apk(self):
        self.assertIsNone(previous_release_asset([[]], 1))
        self.assertIsNone(previous_release_asset([[release(2, 14, draft=True)]], 1))

    def test_selects_highest_stable_version_across_pages(self):
        pages = [[release(18, 20, prerelease=True), release(2, 21)], [release(10, 22), release(3, 23)]]
        self.assertEqual((10, 22), previous_release_asset(pages, 11))

    def test_rejects_an_old_version_and_an_ambiguous_predecessor(self):
        with self.assertRaisesRegex(ValueError, "greater"):
            previous_release_asset([[release(2, 20)]], 2)
        with self.assertRaisesRegex(ValueError, "ambiguous"):
            previous_release_asset([[release(2, 20), release(2, 21)]], 3)
        with self.assertRaisesRegex(ValueError, "asset ID"):
            previous_release_asset([[release(2, "-not-an-id")]], 3)
        with self.assertRaisesRegex(ValueError, "Unexpected GitHub"):
            previous_release_asset({}, 3)

    def test_accepts_one_signer_matching_the_previous_release(self):
        verify_signers(certificate("a" * 64))
        verify_signers(certificate("a" * 64), certificate("A" * 64))

    def test_rejects_a_different_signer_or_incomplete_certificate_report(self):
        with self.assertRaisesRegex(ValueError, "different"):
            verify_signers(certificate("a" * 64), certificate("b" * 64))
        for report in ("Verifies", certificate("a" * 63), certificate("a" * 64).replace("signers: 1", "signers: 2")):
            with self.subTest(report=report), self.assertRaisesRegex(ValueError, "exactly one"):
                verify_signers(report)
