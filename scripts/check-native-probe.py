#!/usr/bin/env python3
"""Validate real native-writer probe evidence without confusing absence with success."""
import json
from pathlib import Path
import re
import struct
import sys

ROUTES = {"HEIC": ("NATIVE_HEIC", 29, "heic"), "AVIF": ("NATIVE_AVIF", 34, "avif")}
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
    if not re.search(r"(?m)^OK \(1 test\)\s*$", log) or not re.search(
            r"(?m)^INSTRUMENTATION_CODE: -1\s*$", log):
        raise ValueError("Native instrumentation did not complete successfully")
    report = json.loads((directory / "g1-native-probe.json").read_text(encoding="utf-8"))
    if report.get("schema") != 1 or report.get("status") != "PROBE_COMPLETE":
        raise ValueError("Native report is absent or incomplete")
    api, size = report["api"], report["size"]
    if type(api) is not int or api < 26 or size != [65, 67]:
        raise ValueError("Unexpected native sample device or dimensions")
    seen = set()
    for sample in report["samples"]:
        fmt = sample["format"]
        if fmt not in ROUTES or fmt in seen:
            raise ValueError("Wrong or repeated native format")
        seen.add(fmt)
        route, min_api, extension = ROUTES[fmt]
        if sample["route"] != route:
            raise ValueError("Unexpected native route")
        file = directory / f"g1-native-fixture.{extension}"
        status = sample["status"]
        if api < min_api:
            if status != "UNAVAILABLE" or sample.get("reason") != "UNSUPPORTED_API" or file.exists():
                raise ValueError("Unsupported API must not produce a candidate")
        elif status == "ENCODED_AND_REOPENED":
            if not file.exists() or type(sample.get("bytes")) is not int or sample["bytes"] < 16 or (
                sample["bytes"] != file.stat().st_size) or type(sample.get("elapsed_ns")) is not int or (
                sample["elapsed_ns"] < 0):
                raise ValueError("Missing or incorrect encoded sample")
            found = brands(file)
            if fmt == "AVIF" and "avif" not in found:
                raise ValueError("Missing AVIF brand")
            if fmt == "HEIC" and not found.intersection(
                    {"heic", "heix", "hevc", "hevx", "heim", "heis"}):
                raise ValueError("Missing HEIC brand")
        elif status == "UNAVAILABLE":
            if sample.get("reason") not in FAILURES or file.exists():
                raise ValueError("Native absence is not typed or left a candidate")
        else:
            raise ValueError("Invalid native status")
    if seen != set(ROUTES):
        raise ValueError("Native probe did not cover both formats")
    return report

if __name__ == "__main__":
    try:
        data = validate(Path(sys.argv[1] if len(sys.argv) > 1 else "evidence"))
    except (OSError, ValueError, KeyError, TypeError, UnicodeError) as err:
        sys.exit(f"FAIL: {err}")
    print("PASS: native HEIC/AVIF probes collected and classified at API",
          data["api"], "(not physical-device fidelity acceptance)")
