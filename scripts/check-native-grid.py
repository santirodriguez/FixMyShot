#!/usr/bin/env python3
"""Fail-closed source-bound grid/alignment experiment validator; not a product capability probe."""
import json
import math
from pathlib import Path
import re
import struct
import sys

MODES = ((64, 64, 64, 64, False), (65, 67, 65, 67, False),
         (66, 68, 65, 67, False), (66, 68, 65, 67, True),
         (96, 68, 65, 67, False), (128, 96, 65, 67, False))
FORMATS = {"HEIC": (29, "heic"), "AVIF": (34, "avif")}
REASONS = {"ENCODE_FAILED", "NATIVE_UNAVAILABLE", "INVALID_OUTPUT", "IO_ERROR", "TIMEOUT"}


def brands(file: Path) -> set[str]:
    data = file.read_bytes()
    if len(data) < 16 or len(data) > 2_097_152 or data[4:8] != b"ftyp":
        raise ValueError("Native output is not a bounded ISOBMFF file")
    count = struct.unpack(">I", data[:4])[0]
    if count < 16 or count > min(4096, len(data)) or (count - 16) % 4:
        raise ValueError("Invalid ftyp box size")
    return {data[offset:offset + 4].decode("ascii") for offset in [8, *range(16, count, 4)]}


def expected_filename(fmt: str, width: int, height: int, grid: bool) -> str:
    ext = FORMATS[fmt][1]
    return f"g1-grid-{ext}-{width}x{height}-{'on' if grid else 'off'}.{ext}"


def validate(directory: Path) -> dict:
    log = (directory / "grid-instrumentation.txt").read_text(encoding="utf-8")
    if (not re.search(r"(?m)^OK \(1 test\)\s*$", log)
            or not re.search(r"(?m)^INSTRUMENTATION_CODE: -1\s*$", log)
            or any(token in log for token in ("FAILURES!!!", "Process crashed", "INSTRUMENTATION_FAILED"))):
        raise ValueError("Native diagnostic instrumentation failed")
    data = json.loads((directory / "g1-grid-diagnostic.json").read_text(encoding="utf-8"))
    if data.get("schema") != 2 or data.get("status") != "DIAGNOSTIC_COMPLETE" or data.get("kind") != "GRID_AND_PADDING_DIAGNOSTIC":
        raise ValueError("Missing or incomplete experimental report")
    api = data.get("api")
    if type(api) is not int or api < 26:
        raise ValueError("Invalid runtime API")
    samples = data.get("samples")
    if not isinstance(samples, list) or len(samples) != len(MODES) * len(FORMATS):
        raise ValueError(f"Expected exactly {len(MODES) * len(FORMATS)} grid/padding cases")
    seen, files = set(), set()
    for sample in samples:
        fmt = sample.get("format")
        if fmt not in FORMATS:
            raise ValueError("Unknown native output")
        numbers = tuple(sample.get(name) for name in ("width", "height", "source_width", "source_height"))
        grid = sample.get("grid")
        padded = sample.get("padded")
        if any(type(value) is not int for value in numbers) or type(grid) is not bool or type(padded) is not bool:
            raise ValueError("Invalid dimensions or grid identity")
        mode = (*numbers, grid)
        if mode not in MODES or padded != (numbers[:2] != numbers[2:]) or (fmt, mode) in seen:
            raise ValueError("Duplicated/invalid grid configuration")
        seen.add((fmt, mode))
        if sample.get("color_profile_inspection") != "NOT_PERFORMED":
            raise ValueError("Color tagging is not established by an Android pixel decode")
        if fmt == "HEIC" and api >= 29:
            trace = sample.get("encoder_trace")
            if not isinstance(trace, dict):
                raise ValueError("Missing actual HEIC writer introspection status")
            trace_status = trace.get("status")
            if trace_status == "CAPTURED":
                if trace.get("source") != "ANDROIDX_TEST_REFLECTION":
                    raise ValueError("Untrusted HEIC codec trace source")
                name, mime = trace.get("codec_name"), trace.get("configured_mime")
                if (not isinstance(name, str) or not 1 <= len(name) <= 256 or
                        not isinstance(mime, str) or not 1 <= len(mime) <= 128):
                    raise ValueError("Invalid selected codec identity")
                if (trace.get("requested_width"), trace.get("requested_height")) != numbers[:2]:
                    raise ValueError("HEIC writer requested dimensions differ")
                if any(type(trace.get(field)) is not int or not 1 <= trace[field] <= 16384
                       for field in ("configured_width", "configured_height")):
                    raise ValueError("Invalid configured MediaFormat geometry")
                if type(trace.get("effective_grid")) is not bool:
                    raise ValueError("Missing effective grid mode")
                output_status = trace.get("pre_stop_output_status")
                if output_status == "AVAILABLE":
                    if (not isinstance(trace.get("pre_stop_output_mime"), str) or
                            not trace["pre_stop_output_mime"] or
                            any(type(trace.get(field)) is not int or not 1 <= trace[field] <= 16384
                                for field in ("pre_stop_output_width", "pre_stop_output_height"))):
                        raise ValueError("Invalid pre-stop MediaCodec format poll")
                elif output_status != "NOT_READY":
                    raise ValueError("Invalid pre-stop output status")
            elif trace_status == "UNAVAILABLE":
                if trace.get("reason") != "REFLECTION_UNAVAILABLE":
                    raise ValueError("Missing reflection failure reason")
            elif trace_status != "NOT_CREATED":
                raise ValueError("Invalid HEIC codec trace state")
            if sample.get("status") == "ENCODED_AND_REOPENED" and trace_status == "NOT_CREATED":
                raise ValueError("Successful HEIC write without constructed writer")
        file = directory / expected_filename(fmt, *numbers[:2], grid)
        status = sample.get("status")
        if api < FORMATS[fmt][0]:
            if status != "UNAVAILABLE" or sample.get("reason") != "UNSUPPORTED_API" or file.exists():
                raise ValueError("Lower API illegally claimed native capability")
        elif status == "ENCODED_AND_REOPENED":
            if not file.is_file() or type(sample.get("bytes")) is not int or sample["bytes"] != file.stat().st_size:
                raise ValueError("Positive output bytes missing or mismatched")
            if type(sample.get("elapsed_ns")) is not int or sample["elapsed_ns"] <= 0:
                raise ValueError("Missing encode timing")
            if sample.get("decoded_width") != numbers[0] or sample.get("decoded_height") != numbers[1]:
                raise ValueError("Padding/visible dimensions were silently changed")
            if (type(sample.get("decoded_row_bytes")) is not int or
                    sample["decoded_row_bytes"] < numbers[0] * 4 or
                    sample["decoded_row_bytes"] > 16384 * 16 or
                    sample.get("decoded_bitmap_config") not in
                    ("ARGB_8888", "RGB_565", "RGBA_F16", "HARDWARE", "UNKNOWN")):
                raise ValueError("Missing/invalid Android bitmap row layout")
            if type(sample.get("decoded_srgb")) is not bool:
                raise ValueError("Missing decoded sRGB property")
            psnr = sample.get("psnr_rgb_db")
            if type(psnr) not in (int, float) or not math.isfinite(psnr) or psnr < 0 or psnr > 99:
                raise ValueError("Invalid color PSNR observation")
            median = sample.get("median_abs_rgb")
            if not isinstance(median, list) or len(median) != 3 or any(type(x) is not int or not 0 <= x <= 255 for x in median):
                raise ValueError("Invalid color error medians")
            actual = brands(file)
            if fmt == "HEIC" and not actual.intersection({"heic", "heix", "hevc", "hevx", "heim", "heis"}):
                raise ValueError("HEIC brand not verified")
            if fmt == "AVIF" and "avif" not in actual:
                raise ValueError("AVIF brand not verified")
            files.add(file.name)
        elif status == "UNAVAILABLE":
            if sample.get("reason") not in REASONS or file.exists():
                raise ValueError("Unavailability improperly claimed or output leaked")
        else:
            raise ValueError("Unknown diagnostic outcome")
    if seen != {(fmt, mode) for fmt in FORMATS for mode in MODES}:
        raise ValueError("Incomplete geometry comparison")
    existing = {p.name for p in directory.glob('g1-grid-*.*') if p.suffix in ('.heic', '.avif')}
    if existing != files:
        raise ValueError("Unreported or missing diagnostic file")
    return data


if __name__ == '__main__':
    try:
        report = validate(Path(sys.argv[1] if len(sys.argv) > 1 else "evidence"))
    except (OSError, ValueError, KeyError, TypeError, UnicodeError) as exc:
        sys.exit(f"FAIL: {exc}")
    success = sum(s["status"] == "ENCODED_AND_REOPENED" for s in report["samples"])
    print(f"PASS: grid diagnostic API {report['api']}, {success} positive/{len(MODES) * len(FORMATS)} attempts; NOT an output capability guarantee")
