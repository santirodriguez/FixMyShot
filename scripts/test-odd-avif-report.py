#!/usr/bin/env python3
"""Pure host regressions for the synthetic odd-size AVIF report checker."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

p = Path(__file__).with_name("check-odd-avif-report.py")
spec = importlib.util.spec_from_file_location("avif_report", p)
checker = importlib.util.module_from_spec(spec)
spec.loader.exec_module(checker)
DIGEST = "1b526d18191363657ea0d8eadbc7c8a6f61d5a930c4b41d4838ef312390c7743"


def fixture():
    return {"schema": 1, "kind": "ODD_AVIF_NUMERIC_DIAGNOSTIC_V1",
            "acceptance": "DIAGNOSTIC_ONLY__NOT_EXPORT_QUALIFICATION",
            "api": 37, "manufacturer": "Google", "model": "Pixel 9 Pro XL",
            "app_version": "0.1.0-alpha.1", "version_code": 1,
            "base_apk_sha256": DIGEST, "cancelled": False, "failed": False,
            "source_width": 65, "source_height": 67,
            "samples": [{"width": w, "height": h, "grid_requested": grid,
                         "reason": None, "encoded_bytes": 1300, "encoded_sha256": DIGEST,
                         "bitmap_factory": {"exact_pixels": False, "psnr_db": 28.0,
                                            "patch_median_channel_error_max": 1},
                         "image_decoder": None}
                        for w, h, grid in checker.MODES]}


class Checks(unittest.TestCase):
    def test_complete_report(self):
        result = checker.validate(fixture(), DIGEST)
        self.assertEqual((result["encoded"], result["cases"], result["complete"]), (5, 5, True))

    def test_partial_cancel_and_typed_failure(self):
        report = fixture()
        report["samples"] = report["samples"][:1]
        report["cancelled"] = True
        self.assertFalse(checker.validate(report, DIGEST)["complete"])
        report = fixture()
        report["samples"][0].update(reason="ENCODE_FAILED", encoded_bytes=0,
                                    encoded_sha256=None, bitmap_factory=None, image_decoder=None)
        self.assertEqual(checker.validate(report, DIGEST)["encoded"], 4)

    def test_bad_digest_and_case_order(self):
        report = fixture()
        report["base_apk_sha256"] = "0" * 64
        with self.assertRaises(ValueError): checker.validate(report, DIGEST)
        report = fixture()
        report["samples"][0], report["samples"][1] = report["samples"][1], report["samples"][0]
        with self.assertRaises(ValueError): checker.validate(report, DIGEST)

    def test_mismatched_metrics_and_unexpected_sensitive_fields(self):
        for key, value in (("psnr_db", "nan"), ("patch_median_channel_error_max", -1),
                           ("exact_pixels", 1)):
            report = fixture()
            report["samples"][0]["bitmap_factory"][key] = value
            with self.subTest(key=key), self.assertRaises(ValueError): checker.validate(report, DIGEST)
        report = fixture()
        report["samples"][0]["uri"] = "content://private/photo"
        with self.assertRaises(ValueError): checker.validate(report, DIGEST)

    def test_duplicate_properties_and_large_report_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "report.json"
            path.write_text('{"kind":"x","kind":"y"}')
            with self.assertRaises(ValueError): checker.load(path)
            path.write_text(json.dumps(fixture()) + " " * 32768)
            with self.assertRaises(ValueError): checker.load(path)


if __name__ == "__main__":
    unittest.main()
