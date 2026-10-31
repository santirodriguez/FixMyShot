#!/usr/bin/env python3
"""Source-bound isolated Android AVIF JNI inputs and real APK-delta verifier."""
import argparse
import hashlib
import json
import math
from pathlib import Path
import re
import shutil
import statistics
import zipfile

ABIS = ("arm64-v8a", "armeabi-v7a", "x86_64")
LICENSE_FILES = ("libavif-LICENSE", "libaom-LICENSE", "libaom-PATENTS")
SOURCE_PINS = {
    "libavif_sha": "c5240fc79fe5c2407e10afd35f5505ef6333ea49",
    "libaom_sha": "af3dc9aadc793c1b00edc003e3fc19a44fdeb574",
    "ndk": "28.2.13676358",
}


def digest(data):
    return hashlib.sha256(data).hexdigest()


def read_digests(path):
    checks = {}
    for line in path.read_text().splitlines():
        pair = line.strip().split(maxsplit=1)
        if len(pair) != 2 or not re.fullmatch(r"[0-9a-f]{64}", pair[0]):
            raise ValueError("Invalid source digest entry")
        name = pair[1].lstrip("*")
        if name in checks:
            raise ValueError("Duplicate hash receipt")
        checks[name] = pair[0]
    return checks


def prepare(artifacts, output, source_sha):
    if output.exists():
        raise ValueError("Output directory exists; refuse overwrite")
    if not re.fullmatch(r"[0-9a-f]{40}", source_sha):
        raise ValueError("Invalid source identity")
    expected = {"libavif_probe.so", *(f"LICENSES/{item}" for item in LICENSE_FILES)}
    common_licenses = {}
    binaries = {}
    for abi in ABIS:
        source = artifacts / f"avif-jni-{abi}-{source_sha}"
        if not source.is_dir():
            raise ValueError(f"Missing compiled ABI artifact {abi}")
        got = read_digests(source / "sha256.txt")
        if set(got) != expected:
            raise ValueError(f"Missing or unexpected hashed input: {abi}")
        versions = dict(
            line.split("=", 1)
            for line in (source / "source-versions.txt").read_text().splitlines()
        )
        if versions != {**SOURCE_PINS, "abi": abi}:
            raise ValueError(f"Source provenance mismatch: {abi}")
        for name, expected_sha in got.items():
            path = source / name
            if not path.is_file() or path.is_symlink():
                raise ValueError(f"Unsafe or missing artifact: {abi}/{name}")
            data = path.read_bytes()
            if digest(data) != expected_sha:
                raise ValueError(f"Content digest mismatch: {abi}/{name}")
            if name.startswith("LICENSES/"):
                if name in common_licenses and common_licenses[name] != data:
                    raise ValueError(f"Inconsistent upstream notices: {name}")
                common_licenses[name] = data
            else:
                if not data.startswith(b"\x7fELF"):
                    raise ValueError(f"Invalid native library for {abi}")
                binaries[abi] = data
        elf = (source / "elf.txt").read_text()
        needed = set(re.findall(r"\(NEEDED\).*?\[(.*?)\]", elf))
        if not needed.issubset({"libc.so", "libm.so", "libdl.so"}):
            raise ValueError(f"Unexpected linked runtime in {abi}: {needed}")
        if "JNI" not in elf and not binaries[abi]:
            raise ValueError("Unusable binary")
    if b"Alliance for Open Media Patent License 1.0" not in common_licenses["LICENSES/libaom-PATENTS"]:
        raise ValueError("Missing AOM patent grant notice")
    output.mkdir(parents=True)
    for abi in ABIS:
        destination = output / "jniLibs" / abi / "libavif_probe.so"
        destination.parent.mkdir(parents=True)
        destination.write_bytes(binaries[abi])
    for name, data in common_licenses.items():
        destination = output / "assets" / "legal" / "avif-software" / Path(name).name
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(data)
    report = {
        "kind": "SOURCE_PINNED_AVIF_JNI_INPUTS__NOT_PRODUCTION",
        "source_sha": source_sha,
        "pins": SOURCE_PINS,
        "library_sha256": {abi: digest(binaries[abi]) for abi in ABIS},
        "license_sha256": {k: digest(v) for k, v in common_licenses.items()},
    }
    (output / "input-receipt.json").write_text(json.dumps(report, indent=2) + "\n")
    return report


def apk_index(path):
    files = {}
    with zipfile.ZipFile(path) as archive:
        assert archive.testzip() is None, "APK CRC failure"
        for member in archive.infolist():
            if member.filename in files or member.filename.startswith("/") or ".." in Path(member.filename).parts:
                raise ValueError("Duplicate or unsafe APK member")
            files[member.filename] = {
                "sha256": digest(archive.read(member)),
                "bytes": member.file_size,
                "stored": member.compress_type == zipfile.ZIP_STORED,
                "compressed_bytes": member.compress_size,
            }
    return files


def compare(baseline, native):
    before, after = apk_index(baseline), apk_index(native)
    extras = set(after) - set(before)
    absent = set(before) - set(after)
    required = {
        *(f"lib/{abi}/libavif_probe.so" for abi in ABIS),
        *(f"assets/legal/avif-software/{name}" for name in LICENSE_FILES),
    }
    if absent or extras != required:
        raise ValueError(f"Unexpected APK members: missing={absent}, extra={extras}")
    changed = [name for name in before if before[name]["sha256"] != after[name]["sha256"]]
    if changed:
        raise ValueError(f"Baseline existing app ZIP members changed: {changed[:20]}")
    for name in extras:
        if after[name]["bytes"] <= 0:
            raise ValueError(f"Empty native addition: {name}")
    delta = native.stat().st_size - baseline.stat().st_size
    if delta <= 0:
        raise ValueError("Invalid incremental native package result")
    return {
        "kind": "ACTUAL_DIAGNOSTIC_APK_DELTA__SAME_SOURCE_SAME_DEBUG_PACKAGE",
        "baseline_apk_bytes": baseline.stat().st_size,
        "native_apk_bytes": native.stat().st_size,
        "added_apk_bytes": delta,
        "baseline_sha256": digest(baseline.read_bytes()),
        "native_sha256": digest(native.read_bytes()),
        "existing_zip_members_unchanged": len(before),
        "additions": {name: after[name] for name in sorted(extras)},
        "limits": (
            "Diagnostic APK only. No release, signing identity, native app "
            "selector, Pixel PSS/p95, actual 16KiB runtime or user photos tested."
        ),
    }


def nearest_rank(values, proportion):
    return sorted(values)[math.ceil(proportion * len(values)) - 1]


def runtime_metrics(path):
    obj = json.loads(path.read_text())
    if obj.get("kind") != "ISOLATED_AVIF_JNI__NOT_PRODUCTION":
        raise ValueError("Wrong instrumentation report kind")
    result = {"kind": "API36_EMULATOR_RUNTIME__NOT_PIXEL"}
    for name, count in (("small_65x67", 20), ("large_1344x2992", 5)):
        samples = obj.get(name)
        if not isinstance(samples, list) or len(samples) != count:
            raise ValueError(f"Insufficient synthetic trial count: {name}")
        for sample in samples:
            if min(sample.get("total_elapsed_ns", -1), sample.get("encode_ns", -1),
                   sample.get("encoded_bytes", -1), sample.get("pss_after_kib", -1)) <= 0:
                raise ValueError(f"Invalid sample in {name}")
        encode = [sample["encode_ns"] / 1e6 for sample in samples]
        pss = [sample["pss_after_kib"] for sample in samples]
        result[name] = {
            "samples": count,
            "median_encode_ms": round(statistics.median(encode), 3),
            "p95_encode_ms_nearest_rank": round(nearest_rank(encode, 0.95), 3),
            "max_pss_after_kib": max(pss),
            "max_process_ru_maxrss_kib": max(sample["rss_peak_kib"] for sample in samples),
            "encoded_bytes_first": samples[0]["encoded_bytes"],
        }
    return result


def main():
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="action", required=True)
    p = sub.add_parser("prepare")
    p.add_argument("artifacts", type=Path)
    p.add_argument("destination", type=Path)
    p.add_argument("source_sha")
    p = sub.add_parser("compare")
    p.add_argument("baseline", type=Path)
    p.add_argument("native", type=Path)
    p = sub.add_parser("metrics")
    p.add_argument("report", type=Path)
    args = parser.parse_args()
    if args.action == "prepare":
        report = prepare(args.artifacts, args.destination, args.source_sha)
    elif args.action == "compare":
        report = compare(args.baseline, args.native)
    else:
        report = runtime_metrics(args.report)
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
