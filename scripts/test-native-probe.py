#!/usr/bin/env python3
"""Host regression tests for synthetic native probe report collection."""
import importlib.util
import json
from pathlib import Path
import struct
import tempfile
import unittest

SPEC = importlib.util.spec_from_file_location("native_report", Path(__file__).with_name("check-native-probe.py"))
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)
LOG = "INSTRUMENTATION_RESULT: stream=\n\nOK (1 test)\n\nINSTRUMENTATION_CODE: -1\n"

def report(api=36):
    return dict(schema=1, status="PROBE_COMPLETE", api=api, size=[65, 67], samples=[
        dict(format=fmt, route=route, status="UNAVAILABLE", reason=reason)
        for fmt, route, reason in (
            ("HEIC", "NATIVE_HEIC", "ENCODE_FAILED" if api >= 29 else "UNSUPPORTED_API"),
            ("AVIF", "NATIVE_AVIF", "ENCODE_FAILED" if api >= 34 else "UNSUPPORTED_API"))])

class ProbeTests(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        (self.root / "native-instrumentation.txt").write_text(LOG)
        self.data = report()
    def check(self):
        (self.root / "g1-native-probe.json").write_text(json.dumps(self.data))
        return MODULE.validate(self.root)
    def test_missing_device_encoder_is_an_explicit_failure(self):
        self.assertEqual("UNAVAILABLE", self.check()["samples"][0]["status"])
    def test_old_api_requires_unavailable(self):
        self.data = report(26)
        self.check()
        self.data["samples"][0]["status"] = "ENCODED_AND_REOPENED"
        with self.assertRaises(ValueError): self.check()
    def test_success_needs_actual_brand_and_length(self):
        self.data["samples"][1] = dict(format="AVIF", route="NATIVE_AVIF", status="ENCODED_AND_REOPENED",
                                       bytes=20, elapsed_ns=100)
        (self.root / "g1-native-fixture.avif").write_bytes(
            struct.pack(">I", 20) + b"ftypavif" + struct.pack(">I", 0) + b"mif1")
        self.check()
        (self.root / "g1-native-fixture.avif").write_bytes(b"NOT_AN_AVIF_FILE_____")
        with self.assertRaises(ValueError): self.check()
    def test_incomplete_duplicate_or_deceptive(self):
        self.data["status"] = "INCOMPLETE"
        with self.assertRaises(ValueError): self.check()
        self.data = report()
        self.data["samples"][1] = self.data["samples"][0].copy()
        with self.assertRaises(ValueError): self.check()
        self.data = report()
        self.data["samples"][0]["reason"] = "NOT_INTEGRATED"
        with self.assertRaises(ValueError): self.check()
    def test_instrumentation_failure_is_not_a_probe(self):
        (self.root / "native-instrumentation.txt").write_text("OK (0 tests)\nINSTRUMENTATION_CODE: -1\n")
        with self.assertRaises(ValueError): self.check()

if __name__ == "__main__":
    unittest.main()
