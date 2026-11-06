#!/usr/bin/env python3
"""Offline synthetic regression for the opt-in CI evidence checker."""
import importlib.util
import json
from pathlib import Path
import struct
import tempfile
import unittest
import zlib

HERE = Path(__file__).parent
spec = importlib.util.spec_from_file_location("check16k", HERE / "check-16k-privacy-evidence.py")
checker = importlib.util.module_from_spec(spec)
spec.loader.exec_module(checker)


def chunk(name, payload):
    return struct.pack(">I", len(payload)) + name + payload + struct.pack(">I", zlib.crc32(name + payload) & 0xffffffff)


def exif(preserve):
    # Tiny correctly bounded fake TIFF: Make + GPS-IFD, or only technical width.
    if not preserve:
        return b"II" + struct.pack("<HIH", 42, 8, 1) + struct.pack("<HHII", 0x100, 4, 1, 64) + b"\0" * 4
    camera = b"Synthetic camera\0"
    offset_camera = 8 + 2 + 24 + 4
    offset_gps = offset_camera + len(camera)
    top = (struct.pack("<H", 2) + struct.pack("<HHII", 0x10f, 2, len(camera), offset_camera)
           + struct.pack("<HHII", 0x8825, 4, 1, offset_gps) + b"\0" * 4)
    gps = struct.pack("<H", 4) + b"".join(struct.pack("<HHI4s", tag, 2, 2, value)
        for tag, value in [(1, b"N\0\0\0"), (2, b"1\0\0\0"),
                           (3, b"W\0\0\0"), (4, b"1\0\0\0")]) + b"\0" * 4
    return b"II" + struct.pack("<HI", 42, 8) + top + camera + gps


def png(preserve):
    ihdr = struct.pack(">IIBBBBB", 1, 1, 8, 2, 0, 0, 0)
    return (checker.PNG_MAGIC + chunk(b"IHDR", ihdr) + (chunk(b"eXIf", exif(preserve)) if preserve else b"")
            + chunk(b"IDAT", zlib.compress(b"\x00\x00\x00\x00")) + chunk(b"IEND", b""))


def webp(preserve):
    encoded = b"VP8L" + struct.pack("<I", 5) + b"/\x00\x00\x00\x00\x00"
    if preserve:
        tiff = exif(True)
        encoded += b"EXIF" + struct.pack("<I", len(tiff)) + tiff + (b"\x00" if len(tiff) & 1 else b"")
    return b"RIFF" + struct.pack("<I", 4 + len(encoded)) + b"WEBP" + encoded


class EvidenceTest(unittest.TestCase):
    def make_folder(self, folder):
        (folder / "page-size-shell.txt").write_text("16384\n")
        (folder / "android-api.txt").write_text("35\n")
        (folder / "android-abi.txt").write_text("x86_64\n")
        (folder / "instrumentation.txt").write_text("OK (9 tests)\nINSTRUMENTATION_CODE: -1\n")
        samples = []
        for n in range(20):
            for fmt in ("PNG", "WEBP_LOSSLESS"):
                for iteration in range(2):
                    samples.append({"fixture": n, "format": fmt, "iteration": iteration,
                        "pixels": "PASS", "route": "ANDROID_PNG" if fmt == "PNG" else "ANDROID_WEBP_LOSSLESS"})
        (folder / "g1-smoke.json").write_text(json.dumps({"schema": 2, "status": "PASS_SYNTHETIC_SMOKE_ONLY",
            "api": 35, "page_size_bytes": 16384, "samples": samples}))
        (folder / "g1-fixture-0.png").write_bytes(png(False))
        (folder / "g1-fixture-0.webp").write_bytes(webp(False))
        (folder / "native-webp-16k.webp").write_bytes(webp(False))
        for ext, generator in (("png", png), ("webp", webp)):
            (folder / f"privacy-default.{ext}").write_bytes(generator(False))
            (folder / f"privacy-preserve.{ext}").write_bytes(generator(True))

    def test_synthetic_good_and_tampering_fail_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            folder = Path(tmp)
            self.make_folder(folder)
            self.assertEqual("PASS_SYNTHETIC_DEBUG_ONLY", checker.validate(folder)["status"])
            (folder / "page-size-shell.txt").write_text("4096\n")
            with self.assertRaisesRegex(ValueError, "not 16 KiB"):
                checker.validate(folder)
            (folder / "page-size-shell.txt").write_text("16384\n")
            old = (folder / "privacy-default.png").read_bytes()
            (folder / "privacy-default.png").write_bytes(old + b"PRIVATE-MAKER")
            with self.assertRaises(ValueError):
                checker.validate(folder)
            (folder / "privacy-default.png").write_bytes(old)
            (folder / "privacy-preserve.webp").write_bytes(webp(False))
            with self.assertRaisesRegex(ValueError, "Preservation missing EXIF"):
                checker.validate(folder)

    def test_png_crc_and_webp_size(self):
        p = bytearray(png(True))
        p[18] ^= 1
        with self.assertRaisesRegex(ValueError, "CRC mismatch"):
            checker.png_chunks(bytes(p))
        w = bytearray(webp(True))
        w[4] ^= 1
        with self.assertRaisesRegex(ValueError, "size mismatch"):
            checker.webp_chunks(bytes(w))


if __name__ == "__main__":
    unittest.main()
