#!/usr/bin/env python3
"""Verify a one-off synthetic AVIF Base64 handoff and extract bytes for host decoding."""
import argparse
import base64
import binascii
import hashlib
import json
from pathlib import Path
import re
import sys

FIELDS = {"schema", "kind", "acceptance", "format", "source_width", "source_height",
          "api", "manufacturer", "model", "app_version", "version_code",
          "base_apk_sha256", "cancelled", "reason", "encoded_bytes",
          "encoded_sha256", "encoded_base64"}
HEX = re.compile(r"[0-9a-f]{64}\Z")
MAX_FILE = 16384
MAX_AVIF = 8192


def require(ok, why):
    if not ok:
        raise ValueError(why)


def unique_pairs(pairs):
    value = {}
    for name, part in pairs:
        require(name not in value, "Duplicate JSON field")
        value[name] = part
    return value


def read(path):
    require(path.stat().st_size <= MAX_FILE, "Report exceeds 16 KiB")
    return json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=unique_pairs,
                      parse_constant=lambda _: (_ for _ in ()).throw(ValueError("Non-finite JSON constant")))


def validate(document, expected_apk):
    require(type(document) is dict and set(document) == FIELDS, "Unexpected report fields")
    require(type(document["schema"]) is int and document["schema"] == 1, "Invalid schema")
    require(document["kind"] == "SYNTHETIC_AVIF_BYTES_V1", "Wrong diagnostic kind")
    require(document["acceptance"] == "DIAGNOSTIC_ONLY__NOT_EXPORT_QUALIFICATION", "Wrong scope")
    require(document["format"] == "AVIF", "Wrong format")
    for name, expected in (("source_width", 65), ("source_height", 67)):
        require(type(document[name]) is int and document[name] == expected, "Wrong fixture geometry")
    require(type(document["api"]) is int and document["api"] >= 34, "Wrong API")
    require(type(document["version_code"]) is int and document["version_code"] >= 1, "Wrong app code")
    for name in ("manufacturer", "model", "app_version"):
        value = document[name]
        require(isinstance(value, str) and 0 < len(value) <= 64
                and all(32 <= ord(c) <= 126 for c in value), "Invalid identity field")
    require(type(document["cancelled"]) is bool and not document["cancelled"],
            "Cancelled or incomplete capture")
    require(document["reason"] is None, "Capture reported a failure")
    require(isinstance(expected_apk, str) and HEX.fullmatch(expected_apk),
            "Expected APK digest must be lowercase SHA-256")
    require(document["base_apk_sha256"] == expected_apk,
            "Installed APK digest missing or mismatched")
    size = document["encoded_bytes"]
    require(type(size) is int and 16 <= size <= MAX_AVIF, "Invalid encoded length")
    digest = document["encoded_sha256"]
    require(isinstance(digest, str) and HEX.fullmatch(digest), "Invalid sample digest")
    encoded = document["encoded_base64"]
    require(isinstance(encoded, str) and len(encoded) <= 10924, "Invalid Base64 length")
    try:
        raw = base64.b64decode(encoded, validate=True)
    except (binascii.Error, ValueError) as exc:
        raise ValueError("Invalid Base64") from exc
    require(len(raw) == size and hashlib.sha256(raw).hexdigest() == digest,
            "Decoded bytes do not match encoded-size/digest receipts")
    require(len(raw) >= 20 and raw[4:8] == b"ftyp", "Missing AVIF ftyp header")
    box = int.from_bytes(raw[:4], "big")
    require(20 <= box <= min(len(raw), 4096) and box % 4 == 0,
            "Malformed ftyp box")
    brands = [raw[8:12]] + [raw[i:i+4] for i in range(16, box, 4)]
    require(b"avif" in brands, "No AVIF brand")
    return raw


def main():
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument("report", type=Path)
    cli.add_argument("--expected-apk-sha256", required=True)
    cli.add_argument("--output", type=Path, help="Write verified synthetic bytes to a new .avif file")
    args = cli.parse_args()
    try:
        raw = validate(read(args.report), args.expected_apk_sha256)
        if args.output is not None:
            require(args.output.suffix.lower() == ".avif", "Output must end in .avif")
            with args.output.open("xb") as file:
                file.write(raw)
    except (ValueError, OSError, TypeError, UnicodeError) as exc:
        print(f"INVALID_SYNTHETIC_AVIF: {exc}", file=sys.stderr)
        return 1
    print(f"VERIFIED_SYNTHETIC_AVIF: {len(raw)} bytes, SHA-256 {hashlib.sha256(raw).hexdigest()}")
    print("EXPORT_QUALIFIED: NO; compare independent host decoder and visible color/geometry")
    return 0


if __name__ == "__main__":
    sys.exit(main())
