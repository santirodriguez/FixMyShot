#!/usr/bin/env python3
"""Reject incomplete instrumentation evidence, including successful shell exits."""
import json
from pathlib import Path
import re
import sys


def validate(directory: Path) -> dict:
    log = (directory / "instrumentation.txt").read_text(encoding="utf-8")
    if not re.search(r"(?m)^OK \(4 tests\)\s*$", log):
        raise ValueError("Expected all four instrumentation tests to pass")
    if not re.search(r"(?m)^INSTRUMENTATION_CODE: -1\s*$", log):
        raise ValueError("Missing successful instrumentation completion")
    if any(marker in log for marker in ("FAILURES!!!", "INSTRUMENTATION_FAILED", "Process crashed")):
        raise ValueError("Instrumentation reported a failure")
    report = json.loads((directory / "g1-smoke.json").read_text(encoding="utf-8"))
    if report.get("schema") != 2 or report.get("status") != "PASS_SYNTHETIC_SMOKE_ONLY":
        raise ValueError("Device report is missing, incomplete or unsupported")
    api = report["api"]
    if type(api) is not int or api < 26:
        raise ValueError("Invalid Android API")
    page = report["page_size_bytes"]
    if type(page) is not int or page <= 0 or page & (page - 1):
        raise ValueError("Invalid measured page size")
    heap = report["process_heap_limit_bytes"]
    if type(heap) is not int or heap <= 0:
        raise ValueError("Missing measured process heap limit")
    expected = {(fixture, fmt, iteration) for fixture in range(20)
                for fmt in ("PNG", "WEBP_LOSSLESS") for iteration in range(2)}
    seen = set()
    for sample in report["samples"]:
        key = (sample["fixture"], sample["format"], sample["iteration"])
        if key not in expected or key in seen:
            raise ValueError("Unexpected or duplicated sample")
        seen.add(key)
        fixture, fmt, _ = key
        dimensions = (1344, 2992) if fixture == 0 else (127 + fixture * 7, 241 + fixture * 11)
        if (sample["width"], sample["height"]) != dimensions or sample["pixels"] != "PASS":
            raise ValueError("Missing pixel equality or wrong fixture dimensions")
        route = "ANDROID_PNG" if fmt == "PNG" else ("ANDROID_WEBP_LOSSLESS" if api >= 30 else "LIBWEBP")
        if sample["route"] != route or sample["bytes"] <= 0 or sample["elapsed_ns"] < 0:
            raise ValueError("Invalid encoder result")
    if seen != expected:
        raise ValueError("Missing fixture results; expected 80 encodes")
    for extension, magic in (("png", b"\x89PNG\r\n\x1a\n"), ("webp", b"RIFF")):
        with (directory / f"g1-fixture-0.{extension}").open("rb") as stream:
            if stream.read(len(magic)) != magic:
                raise ValueError(f"Missing synthetic {extension} output bytes")
    return report


if __name__ == "__main__":
    try:
        report = validate(Path(sys.argv[1] if len(sys.argv) > 1 else "evidence"))
    except (OSError, ValueError, KeyError, TypeError) as error:
        sys.exit(f"FAIL: {error}")
    print(f"PASS: four tests, 80 real encodes; API {report['api']}; "
          f"page size {report['page_size_bytes']} bytes (not a physical-device gate)")
