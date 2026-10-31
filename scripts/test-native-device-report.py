#!/usr/bin/env python3
"""Offline synthetic-only regression tests for the shared native-device report validator."""
import copy
import importlib.util
from pathlib import Path
import tempfile
import unittest

CHECK = Path(__file__).with_name("check-native-device-report.py")
spec = importlib.util.spec_from_file_location("native_device_report", CHECK)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
DIGEST = "1b526d18191363657ea0d8eadbc7c8a6f61d5a930c4b41d4838ef312390c7743"


def fixture():
    samples = []
    for fmt, width, height in module.CASES:
        samples.append({"format": fmt, "width": width, "height": height,
                        "status": "ENCODED_AND_REOPENED", "reason": None, "bytes": 400,
                        "adapter_elapsed_ns": 123456, "exact_pixels": False, "psnr_db": 40.0,
                        "patch_median_channel_error_max": 2, "meets_numeric_targets": True})
    return {"schema": 1, "kind": "SYNTHETIC_NATIVE_CHECK_V1",
            "acceptance": "DIAGNOSTIC_ONLY__NOT_EXPORT_QUALIFICATION",
            "measurement": "ONE_ADAPTER_ATTEMPT_PER_CASE__NOT_P95_OR_PEAK_MEMORY",
            "api": 36, "manufacturer": "Google", "model": "Pixel 9 Pro XL",
            "app_version": "0.1.0-alpha.1", "version_code": 1,
            "base_apk_sha256": DIGEST, "cancelled": False, "failed": False,
            "samples": samples}


class ReportChecks(unittest.TestCase):
    def test_complete_report_and_numeric_counts(self):
        value = module.validate(fixture(), DIGEST)
        self.assertTrue(value["complete"])
        self.assertEqual(value["encoded"], {"HEIC": 3, "AVIF": 3})
        self.assertEqual(value["numeric_targets_met"], {"HEIC": 3, "AVIF": 3})

    def test_unavailable_is_not_an_approval(self):
        data = fixture()
        data["samples"][1] = {k: v for k, v in data["samples"][1].items() if k in module.SAMPLE_FIELDS}
        data["samples"][1].update(status="UNAVAILABLE", reason="INVALID_OUTPUT", bytes=0)
        value = module.validate(data, DIGEST)
        self.assertEqual(value["encoded"], {"HEIC": 3, "AVIF": 2})
        self.assertEqual(value["numeric_targets_met"], {"HEIC": 3, "AVIF": 2})

    def test_bad_hash_or_unbound_report_refused(self):
        for sha in (None, "0" * 64):
            data = fixture()
            data["base_apk_sha256"] = sha
            with self.subTest(sha=sha), self.assertRaises(ValueError):
                module.validate(data, DIGEST)

    def test_missing_duplicate_and_reordered_cases(self):
        for variant in ("missing", "duplicate", "reorder"):
            data = fixture()
            if variant == "missing": data["samples"].pop()
            elif variant == "duplicate": data["samples"][1] = copy.deepcopy(data["samples"][0])
            else: data["samples"][0], data["samples"][1] = data["samples"][1], data["samples"][0]
            with self.subTest(variant=variant), self.assertRaises(ValueError):
                module.validate(data, DIGEST)
        data = fixture()
        data["samples"] = data["samples"][:5]
        data["cancelled"] = True
        self.assertFalse(module.validate(data, DIGEST)["complete"])

    def test_bad_metrics_and_unexpected_private_fields_refused(self):
        for key, value in (("psnr_db", "NaN"), ("psnr_db", 37.0),
                           ("patch_median_channel_error_max", -1), ("exact_pixels", 1)):
            data = fixture()
            data["samples"][0][key] = value
            with self.subTest(key=key, value=value), self.assertRaises(ValueError):
                module.validate(data, DIGEST)
        data = fixture()
        data["samples"][0]["source_uri"] = "content://private/image"
        with self.assertRaises(ValueError): module.validate(data, DIGEST)

    def test_exact_match_psnr_null_and_partial_cancellation(self):
        data = fixture()
        data["samples"][0]["exact_pixels"] = True
        data["samples"][0]["psnr_db"] = None
        self.assertTrue(module.validate(data, DIGEST)["complete"])
        data["samples"] = data["samples"][:2]
        data["cancelled"] = True
        self.assertFalse(module.validate(data, DIGEST)["complete"])

    def test_duplicate_json_fields_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "report.json"
            path.write_text('{"schema":1,"schema":2}')
            with self.assertRaises(ValueError): module.load(path)
            path.write_text('{"psnr_db":NaN}')
            with self.assertRaises(ValueError): module.load(path)


if __name__ == "__main__":
    unittest.main()
