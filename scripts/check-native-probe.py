#!/usr/bin/env python3
"""Fail-closed host validation for a source-bound device native still probe."""
import json
from pathlib import Path
import re
import struct
import sys

ROUTES = {"HEIC": ("NATIVE_HEIC", 29, "heic"), "AVIF": ("NATIVE_AVIF", 34, "avif")}
SIZES = ((64, 64), (65, 67))
CODEC_MIMES = ("video/hevc", "video/av01", "image/vnd.android.heic", "image/avif")
FAILURES = {"NATIVE_UNAVAILABLE", "ENCODE_FAILED", "IO_ERROR", "INVALID_OUTPUT",
            "TIMEOUT", "RESOURCE_LIMIT", "CANCELLED", "UNSUPPORTED_ALPHA",
            "METADATA_POLICY_UNAVAILABLE", "COLOR_CONVERSION_REQUIRED"}

def brands(path: Path) -> set[str]:
    data = path.read_bytes()
    if len(data) < 16 or data[4:8] != b"ftyp":
        raise ValueError("Not an ISO BMFF file")
    length = struct.unpack(">I", data[:4])[0]
    if length < 16 or length > min(len(data), 4096) or (length - 16) % 4:
        raise ValueError("Invalid ftyp size")
    return {data[i:i + 4].decode("ascii") for i in [8, *range(16, length, 4)]}

def validate(directory: Path) -> dict:
    log = (directory / "native-instrumentation.txt").read_text(encoding="utf-8")
    if (not re.search(r"(?m)^OK \(1 test\)\s*$", log) or
            not re.search(r"(?m)^INSTRUMENTATION_CODE: -1\s*$", log) or
            "FAILURES!!!" in log or "Process crashed" in log):
        raise ValueError("Native instrumentation did not complete successfully")
    report = json.loads((directory / "g1-native-probe.json").read_text(encoding="utf-8"))
    if report.get("schema") != 2 or report.get("status") != "PROBE_COMPLETE":
        raise ValueError("Native report is absent or incomplete")
    api = report.get("api")
    if type(api) is not int or api < 26 or report.get("sizes") != [list(s) for s in SIZES]:
        raise ValueError("Unexpected native device or fixture sizes")
    inventory = report.get("codec_inventory")
    if not isinstance(inventory, dict) or set(inventory) != set(CODEC_MIMES):
        raise ValueError("Missing or unexpected device codec inventory")
    for mime in CODEC_MIMES:
        entry = inventory[mime]
        if not isinstance(entry, dict) or set(entry) != {"encoders", "decoders"}:
            raise ValueError("Incomplete codec inventory entry")
        for direction in ("encoders", "decoders"):
            names = entry[direction]
            if (not isinstance(names, list) or len(names) > 32 or
                    any(not isinstance(name, str) or not name or len(name) > 128 or
                        name.strip() != name for name in names) or
                    names != sorted(set(names))):
                raise ValueError("Invalid codec inventory names")
    samples = report.get("samples")
    if not isinstance(samples, list) or len(samples) != len(ROUTES) * len(SIZES):
        raise ValueError("Native probe must cover every format and geometry")
    seen, expected_outputs = set(), set()
    for sample in samples:
        fmt = sample["format"]
        if fmt not in ROUTES or type(sample.get("width")) is not int or type(sample.get("height")) is not int:
            raise ValueError("Native sample has invalid identity")
        size = sample["width"], sample["height"]
        key = fmt, size
        if size not in SIZES or key in seen:
            raise ValueError("Wrong or repeated native sample")
        seen.add(key)
        route, minimum_api, extension = ROUTES[fmt]
        if sample["route"] != route:
            raise ValueError("Unexpected native route")
        path = directory / f"g1-native-{size[0]}x{size[1]}.{extension}"
        status = sample["status"]
        if api < minimum_api:
            if status != "UNAVAILABLE" or sample.get("reason") != "UNSUPPORTED_API" or path.exists():
                raise ValueError("Unsupported API must not produce a native candidate")
        elif status == "ENCODED_AND_REOPENED":
            if (not path.is_file() or type(sample.get("bytes")) is not int or
                    sample["bytes"] < 16 or sample["bytes"] != path.stat().st_size or
                    type(sample.get("elapsed_ns")) is not int or sample["elapsed_ns"] < 0):
                raise ValueError("Missing or incorrect native output")
            found = brands(path)
            if fmt == "AVIF" and "avif" not in found:
                raise ValueError("Missing AVIF brand")
            if fmt == "HEIC" and not found.intersection(
                    {"heic", "heix", "hevc", "hevx", "heim", "heis"}):
                raise ValueError("Missing HEIC brand")
            expected_outputs.add(path.name)
        elif status == "UNAVAILABLE":
            if sample.get("reason") not in FAILURES or path.exists():
                raise ValueError("Native failure must be typed and must not retain output")
        else:
            raise ValueError("Unrecognized native result")
    if seen != {(fmt, size) for fmt in ROUTES for size in SIZES}:
        raise ValueError("Missing native probe cases")
    actual_outputs = {p.name for p in directory.glob("g1-native-*.*") if p.suffix in (".heic", ".avif")}
    if actual_outputs != expected_outputs:
        raise ValueError("Unreported or missing native output files")
    return report

if __name__ == "__main__":
    try:
        result = validate(Path(sys.argv[1] if len(sys.argv) > 1 else "evidence"))
    except (OSError, ValueError, KeyError, TypeError, UnicodeError) as error:
        sys.exit(f"FAIL: {error}")
    print("PASS: four real native capability cases", result["api"],
          "(unavailable != supported; not physical-device acceptance)")
