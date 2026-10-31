#!/usr/bin/env python3
"""Offline refusal and sample integrity tests for one synthetic AVIF handoff."""
import base64
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

path = Path(__file__).with_name("check-avif-byte-sample.py")
spec = importlib.util.spec_from_file_location("avif_sample", path)
checker = importlib.util.module_from_spec(spec)
spec.loader.exec_module(checker)
APK = "a" * 64
RAW = b"\x00\x00\x00\x14ftypavif\x00\x00\x00\x00mif1"


def report():
    return {"schema": 1, "kind": "SYNTHETIC_AVIF_BYTES_V1",
            "acceptance": "DIAGNOSTIC_ONLY__NOT_EXPORT_QUALIFICATION",
            "format": "AVIF", "source_width": 65, "source_height": 67,
            "api": 37, "manufacturer": "Google", "model": "Pixel 9 Pro XL",
            "app_version": "0.1.0-alpha.1", "version_code": 1,
            "base_apk_sha256": APK, "cancelled": False, "reason": None,
            "encoded_bytes": len(RAW), "encoded_sha256": hashlib.sha256(RAW).hexdigest(),
            "encoded_base64": base64.b64encode(RAW).decode("ascii")}


class Checks(unittest.TestCase):
    def test_valid_sample(self):
        self.assertEqual(checker.validate(report(), APK), RAW)

    def test_wrong_apk_or_changed_bytes(self):
        data = report()
        data["base_apk_sha256"] = "b" * 64
        with self.assertRaises(ValueError): checker.validate(data, APK)
        data = report()
        data["encoded_sha256"] = "b" * 64
        with self.assertRaises(ValueError): checker.validate(data, APK)
        data = report()
        data["encoded_base64"] = "not-base64"
        with self.assertRaises(ValueError): checker.validate(data, APK)

    def test_reject_non_avif_and_oversize(self):
        for malformed in (b"garbage-encoded", b"\x00\x00\x00\x14ftypheic\x00\x00\x00\x00mif1"):
            data = report()
            data["encoded_bytes"] = len(malformed)
            data["encoded_sha256"] = hashlib.sha256(malformed).hexdigest()
            data["encoded_base64"] = base64.b64encode(malformed).decode("ascii")
            with self.assertRaises(ValueError): checker.validate(data, APK)
        data = report()
        data["encoded_bytes"] = 9000
        with self.assertRaises(ValueError): checker.validate(data, APK)

    def test_refuse_private_fields_and_partial(self):
        data = report()
        data["source_uri"] = "content://private"
        with self.assertRaises(ValueError): checker.validate(data, APK)
        data = report()
        data["cancelled"] = True
        with self.assertRaises(ValueError): checker.validate(data, APK)
        data = report()
        data["reason"] = "ENCODE_FAILED"
        with self.assertRaises(ValueError): checker.validate(data, APK)

    def test_duplicate_or_nonfinite_json(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = Path(tmp) / "sample.json"
            out.write_text('{"schema":1,"schema":1}', encoding="utf-8")
            with self.assertRaises(ValueError): checker.read(out)
            out.write_text('{"psnr":NaN}', encoding="utf-8")
            with self.assertRaises(ValueError): checker.read(out)


class HeicChecks(unittest.TestCase):
    @staticmethod
    def sample(raw=b"\x00\x00\x00\x14ftypheic\x00\x00\x00\x00mif1"):
        data=report()
        data.update(kind="SYNTHETIC_HEIC_BYTES_V1", format="HEIC", api=29,
                    encoded_bytes=len(raw), encoded_sha256=hashlib.sha256(raw).hexdigest(),
                    encoded_base64=base64.b64encode(raw).decode("ascii"))
        return data,raw

    def test_heic_requires_explicit_format_and_matching_kind(self):
        data,raw=self.sample()
        self.assertEqual(checker.validate(data,APK,"HEIC"),raw)
        with self.assertRaises(ValueError): checker.validate(data,APK)
        with self.assertRaises(ValueError): checker.validate(report(),APK,"HEIC")
        data["api"]=28
        with self.assertRaises(ValueError): checker.validate(data,APK,"HEIC")

    def test_heic_rejects_avif_generic_and_malformed_brands(self):
        for raw in (RAW,b"\x00\x00\x00\x14ftypmif1\x00\x00\x00\x00mif1",
                    b"\x00\x00\x00\x13ftypheic\x00\x00\x00\x00mif1"):
            data,_=self.sample(raw)
            with self.assertRaises(ValueError): checker.validate(data,APK,"HEIC")

    def test_heic_rejects_digest_private_fields_and_cancel(self):
        for field,value in (("encoded_sha256","b"*64),("source_uri","content://private"),
                            ("cancelled",True),("encoded_bytes",8193)):
            data,_=self.sample();data[field]=value
            with self.assertRaises(ValueError): checker.validate(data,APK,"HEIC")


if __name__ == "__main__":
    unittest.main()
