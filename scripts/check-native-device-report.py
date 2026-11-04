#!/usr/bin/env python3
"""Validate one shared on-device HEIC/AVIF diagnostic, never qualify a release.

The report and APK hash are user-supplied evidence, not device attestation.
No images, network, Android SDK or signing material are accessed.
"""
import argparse
import json
import math
from pathlib import Path
import re
import sys

FORMATS = ("HEIC", "AVIF")
SIZES = ((64, 64), (65, 67), (1344, 2992))
CASES = [(fmt, width, height) for width, height in SIZES for fmt in FORMATS]
TOP_FIELDS = {"schema", "kind", "acceptance", "measurement", "api", "manufacturer", "model",
              "app_version", "version_code", "base_apk_sha256", "cancelled", "failed", "samples"}
SAMPLE_FIELDS = {"format", "width", "height", "status", "reason", "bytes", "adapter_elapsed_ns"}
METRIC_FIELDS = {"exact_pixels", "psnr_db", "patch_median_channel_error_max", "meets_numeric_targets"}
HEX_SHA256 = re.compile(r"[0-9a-f]{64}\Z")


def require(value, message):
    if not value:
        raise ValueError(message)


def integer(value, low, message):
    require(type(value) is int and value >= low, message)


def object_no_duplicates(pairs):
    obj = {}
    for key, value in pairs:
        require(key not in obj, "Duplicate JSON property")
        obj[key] = value
    return obj


def load(path):
    require(path.stat().st_size <= 64 * 1024, "Report exceeds 64 KiB")
    return json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=object_no_duplicates,
                      parse_constant=lambda value: (_ for _ in ()).throw(ValueError("Non-finite JSON number")))


def validate(report, expected_apk_sha256):
    require(isinstance(report, dict) and set(report) == TOP_FIELDS, "Unexpected report fields")
    require(report["schema"] == 1 and type(report["schema"]) is int, "Unexpected schema")
    require(report["kind"] == "SYNTHETIC_NATIVE_CHECK_V1", "Wrong report kind")
    require(report["acceptance"] == "DIAGNOSTIC_ONLY__NOT_EXPORT_QUALIFICATION", "Wrong acceptance scope")
    require(report["measurement"] == "ONE_ADAPTER_ATTEMPT_PER_CASE__NOT_P95_OR_PEAK_MEMORY", "Wrong measurement scope")
    integer(report["api"], 26, "Invalid Android API")
    integer(report["version_code"], 1, "Invalid versionCode")
    for key in ("cancelled", "failed"):
        require(type(report[key]) is bool, f"Invalid {key}")
    for key in ("manufacturer", "model", "app_version"):
        value = report[key]
        require(isinstance(value, str) and len(value) <= 64 and all(32 <= ord(c) <= 126 for c in value),
                f"Invalid {key}")
    require(report["app_version"] != "", "Missing app version")
    require(HEX_SHA256.fullmatch(expected_apk_sha256) is not None, "Invalid expected APK hash")
    require(report["base_apk_sha256"] == expected_apk_sha256, "Installed APK hash unavailable or mismatched")
    samples = report["samples"]
    require(isinstance(samples, list) and len(samples) <= 6, "Invalid sample count")
    if not report["cancelled"] and not report["failed"]:
        require(len(samples) == 6, "Incomplete report without stop/failure")
    encoded = dict.fromkeys(FORMATS, 0)
    targets = dict.fromkeys(FORMATS, 0)
    for index, sample in enumerate(samples):
        require(isinstance(sample, dict) and SAMPLE_FIELDS <= set(sample), "Missing sample fields")
        fields = set(sample)
        require(fields in (SAMPLE_FIELDS, SAMPLE_FIELDS | METRIC_FIELDS), "Unexpected sample fields")
        key = (sample["format"], sample["width"], sample["height"])
        require(key == CASES[index] and all(type(x) is int for x in key[1:]), "Wrong, duplicate or reordered case")
        integer(sample["bytes"], 0, "Invalid output length")
        integer(sample["adapter_elapsed_ns"], 0, "Invalid elapsed time")
        if sample["status"] == "UNAVAILABLE":
            require(fields == SAMPLE_FIELDS and sample["bytes"] == 0 and
                    isinstance(sample["reason"], str) and
                    re.fullmatch(r"[A-Z][A-Z0-9_]*", sample["reason"]) is not None,
                    "Invalid unavailable sample")
        else:
            require(sample["status"] == "ENCODED_AND_REOPENED" and fields == SAMPLE_FIELDS | METRIC_FIELDS,
                    "Invalid encoded sample")
            require(sample["reason"] is None and sample["bytes"] > 0 and sample["bytes"] <= 128 * 1024 * 1024,
                    "Invalid encoded output")
            require(type(sample["exact_pixels"]) is bool and type(sample["meets_numeric_targets"]) is bool,
                    "Invalid metric flags")
            integer(sample["patch_median_channel_error_max"], 0, "Invalid patch error")
            require(sample["patch_median_channel_error_max"] <= 255, "Patch error out of range")
            psnr = sample["psnr_db"]
            if sample["exact_pixels"]:
                require(psnr is None, "Exact pixels must omit PSNR")
            else:
                require(type(psnr) in (int, float) and math.isfinite(psnr) and psnr >= 0,
                        "Non-exact pixels require finite PSNR")
            meets = (sample["exact_pixels"] or (psnr or 0) >= 38.0) and sample["patch_median_channel_error_max"] <= 2
            require(sample["meets_numeric_targets"] == meets, "Inconsistent numeric-target flag")
            encoded[sample["format"]] += 1
            targets[sample["format"]] += int(meets)
    return {"complete": len(samples) == 6 and not report["cancelled"] and not report["failed"],
            "encoded": encoded, "numeric_targets_met": targets, "attempts": len(samples)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("report", type=Path, help="JSON text shared from the diagnostic chooser")
    parser.add_argument("--expected-apk-sha256", required=True,
                        help="Known SHA-256 of the exact installed diagnostic APK")
    args = parser.parse_args()
    try:
        result = validate(load(args.report), args.expected_apk_sha256)
    except (ValueError, OSError, UnicodeError, TypeError, KeyError) as error:
        print(f"INVALID_NATIVE_DIAGNOSTIC: {error}", file=sys.stderr)
        return 1
    state = "COMPLETE" if result["complete"] else "PARTIAL"
    print(f"VALID_SYNTHETIC_REPORT ({state}); installed-APK hash matches the supplied expected digest")
    for fmt in FORMATS:
        print(f"{fmt}: encoded/reopened {result['encoded'][fmt]}/3; numeric targets met {result['numeric_targets_met'][fmt]}/3")
    print("RELEASE_QUALIFIED: NO; physical fidelity, cross-device, gallery, source and signing gates remain open")
    return 0


if __name__ == "__main__":
    sys.exit(main())
