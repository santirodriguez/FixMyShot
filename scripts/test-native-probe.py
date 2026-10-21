#!/usr/bin/env python3
"""Host-only report regressions; they do not test a real Android encoder."""
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
    return dict(schema=2, status="PROBE_COMPLETE", api=api,
                codec_inventory={mime: {"encoders": [], "decoders": []}
                                 for mime in MODULE.CODEC_MIMES},
                sizes=[[64, 64], [65, 67]], samples=[
        dict(format=fmt, route=route, width=w, height=h, status="UNAVAILABLE",
             reason="ENCODE_FAILED" if api >= minimum else "UNSUPPORTED_API")
        for w, h in ((64, 64), (65, 67)) for fmt, route, minimum in
        (("HEIC", "NATIVE_HEIC", 29), ("AVIF", "NATIVE_AVIF", 34))])

class NativeProbeTests(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.root = Path(tmp.name)
        (self.root / "native-instrumentation.txt").write_text(LOG)
        self.data = report()

    def check(self):
        (self.root / "g1-native-probe.json").write_text(json.dumps(self.data))
        return MODULE.validate(self.root)

    def test_four_cases_or_fail(self):
        self.assertEqual(4, len(self.check()["samples"]))
        self.data["samples"].pop()
        with self.assertRaises(ValueError): self.check()

    def test_api26_requires_both_formats_unavailable(self):
        self.data = report(26)
        self.check()
        self.data["samples"][0]["reason"] = "ENCODE_FAILED"
        with self.assertRaises(ValueError): self.check()

    def test_valid_success_needs_real_brand_and_length(self):
        self.data["samples"][1] = dict(format="AVIF", route="NATIVE_AVIF",
            width=64, height=64, status="ENCODED_AND_REOPENED", bytes=20, elapsed_ns=100)
        output = self.root / "g1-native-64x64.avif"
        output.write_bytes(struct.pack(">I", 20) + b"ftypavif" + struct.pack(">I", 0) + b"mif1")
        self.check()
        output.write_bytes(b"NOT_A_VALID_FILE_____")
        with self.assertRaises(ValueError): self.check()

    def test_failed_output_stays_unavailable(self):
        self.data["samples"][0]["reason"] = "INVALID_OUTPUT"
        self.assertEqual("UNAVAILABLE", self.check()["samples"][0]["status"])

    def test_error_text_is_not_evidence(self):
        (self.root / "g1-native-65x67.heic").write_text("cat: no such file\n")
        with self.assertRaises(ValueError): self.check()

    def test_missing_duplicate_and_unreported_cases(self):
        self.data["samples"][0] = self.data["samples"][2].copy()
        with self.assertRaises(ValueError): self.check()
        self.data = report()
        self.data["samples"][0]["width"] = 65
        with self.assertRaises(ValueError): self.check()

    def test_schema_and_fixture_manifest_are_strict(self):
        self.data["sizes"] = [[65, 67], [64, 64]]
        with self.assertRaises(ValueError): self.check()
        self.data = report()
        self.data["schema"] = 1
        with self.assertRaises(ValueError): self.check()

    def test_codec_inventory_is_complete_and_nonassertive(self):
        self.assertEqual([], self.check()["codec_inventory"]["video/hevc"]["encoders"])
        del self.data["codec_inventory"]["video/av01"]
        with self.assertRaises(ValueError): self.check()
        self.data = report()
        self.data["codec_inventory"]["video/hevc"]["encoders"] = ["dup", "dup"]
        with self.assertRaises(ValueError): self.check()
        self.data = report()
        self.data["codec_inventory"]["image/avif"]["decoders"] = ["c2.android.av1.decoder"]
        self.check()  # Advertisement alone must not change the selected route status.

    def test_bad_instrumentation_rejected(self):
        (self.root / "native-instrumentation.txt").write_text("FAILURES!!!\nINSTRUMENTATION_CODE: -1\n")
        with self.assertRaises(ValueError): self.check()

if __name__ == "__main__":
    unittest.main()
