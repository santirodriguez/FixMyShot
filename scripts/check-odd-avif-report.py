#!/usr/bin/env python3
"""Validate one source-bound numeric odd-size AVIF report; never qualify a release."""
import argparse
import json
import math
from pathlib import Path
import re
import sys

MODES = ((65, 67, True), (65, 67, False), (66, 68, False),
         (66, 68, True), (96, 68, False))
TOP = {"schema", "kind", "acceptance", "api", "manufacturer", "model",
       "app_version", "version_code", "base_apk_sha256", "cancelled",
       "failed", "source_width", "source_height", "samples"}
SAMPLE = {"width", "height", "grid_requested", "reason", "encoded_bytes",
          "encoded_sha256", "bitmap_factory", "image_decoder"}
METRIC = {"exact_pixels", "psnr_db", "patch_median_channel_error_max"}
HEX = re.compile(r"[a-f0-9]{64}\Z")


def check(condition, message):
    if not condition:
        raise ValueError(message)


def unique_pairs(pairs):
    result = {}
    for key, value in pairs:
        check(key not in result, "Duplicate JSON key")
        result[key] = value
    return result


def load(path):
    check(path.stat().st_size <= 32768, "Report larger than 32 KiB")
    return json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=unique_pairs,
                      parse_constant=lambda _: (_ for _ in ()).throw(ValueError("Invalid JSON constant")))


def validate(data, expected_apk):
    check(isinstance(data, dict) and set(data) == TOP, "Invalid top-level fields")
    check(type(data["schema"]) is int and data["schema"] == 1, "Wrong schema")
    check(data["kind"] == "ODD_AVIF_NUMERIC_DIAGNOSTIC_V1", "Wrong kind")
    check(data["acceptance"] == "DIAGNOSTIC_ONLY__NOT_EXPORT_QUALIFICATION", "Wrong scope")
    check(type(data["api"]) is int and data["api"] >= 26, "Invalid API")
    check(type(data["version_code"]) is int and data["version_code"] >= 1, "Invalid versionCode")
    check(data["source_width"] == 65 and data["source_height"] == 67 and
          type(data["source_width"]) is int and type(data["source_height"]) is int, "Wrong source")
    for key in ("cancelled", "failed"):
        check(type(data[key]) is bool, "Invalid " + key)
    for key in ("manufacturer", "model", "app_version"):
        value = data[key]
        check(isinstance(value, str) and 0 < len(value) <= 64 and
              all(32 <= ord(c) <= 126 for c in value), "Invalid " + key)
    check(isinstance(expected_apk, str) and HEX.fullmatch(expected_apk), "Invalid expected hash")
    check(data["base_apk_sha256"] == expected_apk, "Installed APK mismatch or unavailable")
    cases = data["samples"]
    check(isinstance(cases, list) and len(cases) <= len(MODES), "Wrong case count")
    if not data["cancelled"] and not data["failed"]:
        check(len(cases) == len(MODES), "Incomplete successful run")
    for index, entry in enumerate(cases):
        check(isinstance(entry, dict) and set(entry) == SAMPLE, "Wrong sample fields")
        mode = (entry["width"], entry["height"], entry["grid_requested"])
        check(type(mode[0]) is int and type(mode[1]) is int and type(mode[2]) is bool and
              mode == MODES[index], "Unexpected, repeated or reordered case")
        size, digest, reason = entry["encoded_bytes"], entry["encoded_sha256"], entry["reason"]
        check(type(size) is int and 0 <= size <= 8192, "Invalid output size")
        if size:
            check(isinstance(digest, str) and HEX.fullmatch(digest), "Invalid digest")
            check(reason in (None, "PLATFORM_DECODE_UNAVAILABLE"), "Unexpected encoded reason")
        else:
            check(digest is None and isinstance(reason, str) and
                  re.fullmatch(r"[A-Z][A-Z0-9_]*", reason), "Invalid rejection")
        observed = 0
        for key in ("bitmap_factory", "image_decoder"):
            metric = entry[key]
            if metric is None:
                continue
            observed += 1
            check(isinstance(metric, dict) and set(metric) == METRIC, "Invalid metrics")
            exact, psnr, patch = metric["exact_pixels"], metric["psnr_db"], metric["patch_median_channel_error_max"]
            check(type(exact) is bool and type(patch) is int and 0 <= patch <= 255, "Invalid metric flags")
            if exact:
                check(psnr is None and patch == 0, "Invalid exact metrics")
            else:
                check(type(psnr) in (float, int) and math.isfinite(psnr) and 0 <= psnr <= 100,
                      "Invalid PSNR")
        check(not observed or size > 0, "Metrics without encoded bytes")
        check((reason is None) == (observed > 0) if size else True, "Inconsistent decoder outcome")
    return {"complete": len(cases) == 5 and not data["cancelled"] and not data["failed"],
            "encoded": sum(bool(s["encoded_bytes"]) for s in cases), "cases": len(cases)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("report", type=Path)
    parser.add_argument("--expected-apk-sha256", required=True)
    args = parser.parse_args()
    try:
        summary = validate(load(args.report), args.expected_apk_sha256)
    except (ValueError, OSError, TypeError, UnicodeError) as error:
        print(f"INVALID_ODD_AVIF_REPORT: {error}", file=sys.stderr)
        return 1
    print(f"VALID_DIAGNOSTIC_ONLY: {summary['cases']} cases, {summary['encoded']} source-bound encoded outputs")
    print("NATIVE_EXPORT_QUALIFIED: NO; independent host decoding still required")
    return 0


if __name__ == "__main__":
    sys.exit(main())
