#!/usr/bin/env python3
"""Fail-closed source receipts and packaged ELF/ZIP checks for an opt-in debug probe."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import struct
import subprocess
import tempfile
import zipfile

ABIS = ("arm64-v8a", "x86_64")
NOTICES = ("libavif-LICENSE", "libaom-LICENSE", "libaom-PATENTS")
PINS = {
    "libavif_sha": "c5240fc79fe5c2407e10afd35f5505ef6333ea49",
    "libaom_sha": "af3dc9aadc793c1b00edc003e3fc19a44fdeb574",
    "ndk": "28.2.13676358",
}


def sha(data):
    return hashlib.sha256(data).hexdigest()


def hashes(path):
    entries = {}
    for line in path.read_text().splitlines():
        match = re.fullmatch(r"([0-9a-f]{64})  (\S+)", line)
        if not match or match[2] in entries:
            raise ValueError("Invalid or repeated receipt")
        entries[match[2]] = match[1]
    return entries


def prepare(artifacts, target, commit, project_license):
    if target.exists() or not re.fullmatch(r"[0-9a-f]{40}", commit):
        raise ValueError("Refuse existing destination or invalid source SHA")
    canonical = {}
    binaries = {}
    expected = {"libavif_physical.so", *(f"LICENSES/{name}" for name in NOTICES)}
    for abi in ABIS:
        origin = artifacts / f"avif-physical-{abi}-{commit}"
        receipt = hashes(origin / "sha256.txt")
        if set(receipt) != expected:
            raise ValueError(f"Unexpected ABI receipt: {abi}")
        versions = dict(row.split("=", 1) for row
                        in (origin / "source-versions.txt").read_text().splitlines())
        if versions != {**PINS, "abi": abi}:
            raise ValueError(f"Unexpected native source pins: {abi}")
        for name, expected_digest in receipt.items():
            file = origin / name
            if not file.is_file() or file.is_symlink():
                raise ValueError(f"Unsafe source artifact {abi}/{name}")
            data = file.read_bytes()
            if sha(data) != expected_digest:
                raise ValueError(f"Tampered native/notice artifact {abi}/{name}")
            if name.startswith("LICENSES/"):
                if name in canonical and canonical[name] != data:
                    raise ValueError("Different source notices between ABIs")
                canonical[name] = data
            else:
                if not data.startswith(b"\x7fELF"):
                    raise ValueError("Not an ELF shared library")
                binaries[abi] = data
        symbols = (origin / "elf.txt").read_text()
        if "LOAD" not in symbols or "libjnigraphics.so" not in symbols:
            raise ValueError("Missing JNI runtime/alignment receipt")
    if b"Alliance for Open Media Patent License 1.0" not in canonical["LICENSES/libaom-PATENTS"]:
        raise ValueError("Patent notice missing")
    target.mkdir(parents=True)
    for abi, data in binaries.items():
        file = target / "jniLibs" / abi / "libavif_physical.so"
        file.parent.mkdir(parents=True)
        file.write_bytes(data)
    for name, data in canonical.items():
        file = target / "assets/legal/avif-software" / Path(name).name
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_bytes(data)
    license_data = project_license.read_bytes()
    if not license_data or b"GNU GENERAL PUBLIC LICENSE" not in license_data:
        raise ValueError("Canonical GPL source missing")
    target_file = target / "assets/legal/project/LICENSE"
    target_file.parent.mkdir(parents=True)
    target_file.write_bytes(license_data)
    return {
        "kind": "PINNED_SOURCE_PHYSICAL_PROBE_INPUTS",
        "repository_commit": commit,
        "abi_binary_sha256": {abi: sha(data) for abi, data in binaries.items()},
        "notice_sha256": {name: sha(data) for name, data in canonical.items()},
        "project_gpl_sha256": sha(license_data),
    }


def inspect_elf(data):
    if len(data) < 64 or data[:4] != b"\x7fELF" or data[4:6] != b"\x02\x01":
        raise ValueError("Expected 64-bit little-endian ELF")
    phoff = struct.unpack_from("<Q", data, 32)[0]
    entry_size, count = struct.unpack_from("<HH", data, 54)
    if count < 2 or count > 80 or entry_size < 56 or phoff + count * entry_size > len(data):
        raise ValueError("ELF header or program header bounds invalid")
    loads = 0
    for i in range(count):
        start = phoff + i * entry_size
        if struct.unpack_from("<I", data, start)[0] != 1:
            continue
        offset, virtual, align = (struct.unpack_from("<Q", data, start + position)[0]
                                  for position in (8, 16, 48))
        if align < 16384 or (virtual - offset) % 16384 or offset >= len(data):
            raise ValueError("Native PT_LOAD 16KiB alignment failure")
        loads += 1
    if loads < 2:
        raise ValueError("No ordinary native LOAD segments")
    return loads


def check_apk(apk, prepared, raw_native_dir, llvm_objcopy):
    if not llvm_objcopy.is_file() or llvm_objcopy.name != "llvm-objcopy":
        raise ValueError("Pinned NDK llvm-objcopy required to verify packaged native bytes")
    required = {
        *(f"lib/{abi}/libavif_physical.so" for abi in ABIS),
        *(f"assets/legal/avif-software/{n}" for n in NOTICES),
        "assets/legal/project/LICENSE",
    }
    detected = {}
    with apk.open("rb") as raw, zipfile.ZipFile(apk) as archive:
        if archive.testzip() is not None:
            raise ValueError("APK ZIP CRC failure")
        seen = set()
        for entry in archive.infolist():
            name = entry.filename
            if name in seen or name.startswith("/") or ".." in Path(name).parts:
                raise ValueError("Unsafe/duplicate APK entry")
            seen.add(name)
            if name.startswith("lib/") and name.endswith(".so") and name not in required:
                raise ValueError("Unexpected native library")
            if name not in required:
                continue
            data = archive.read(entry)
            if not data:
                raise ValueError("Empty required entry")
            if name.startswith("lib/"):
                if entry.compress_type != zipfile.ZIP_STORED:
                    raise ValueError("Native library must remain uncompressed")
                raw.seek(entry.header_offset + 26)
                name_length, extra_length = struct.unpack("<HH", raw.read(4))
                data_offset = entry.header_offset + 30 + name_length + extra_length
                if data_offset % 16384:
                    raise ValueError("Native APK ZIP data misalignment")
                loads = inspect_elf(data)
                detected[name] = {"sha256": sha(data), "elf_loads": loads,
                                  "data_offset": data_offset, "size": len(data)}
            else:
                detected[name] = {"sha256": sha(data), "size": len(data)}
        if not required.issubset(seen):
            raise ValueError(f"Missing expected APK inputs: {required - seen}")
    # Android Gradle Plugin strips native shared objects before APK packaging.
    # First authenticate the raw exact-source binary, then independently
    # reproduce the NDK strip-unneeded transform byte-for-byte. Do not
    # trust hashes alone or relax the 16KiB ELF/ZIP checks above.
    for abi in ABIS:
        native_name = f"lib/{abi}/libavif_physical.so"
        source = raw_native_dir / abi / "libavif_physical.so"
        if not source.is_file() or source.is_symlink():
            raise ValueError(f"Missing/unsafe pinned input for {abi}")
        raw = source.read_bytes()
        if sha(raw) != prepared["abi_binary_sha256"][abi]:
            raise ValueError(f"Raw input source SHA mismatch: {abi}")
        with tempfile.TemporaryDirectory(prefix="fixmyshot-avif-strip-") as tmp:
            candidate = Path(tmp) / "libavif_physical.so"
            shutil.copyfile(source, candidate)
            subprocess.run([str(llvm_objcopy), "--strip-unneeded", str(candidate)],
                           check=True, timeout=45, capture_output=True)
            normalized = candidate.read_bytes()
        expected_sha = sha(normalized)
        if detected[native_name]["sha256"] != expected_sha:
            raise ValueError(f"Packaged native bytes differ from pinned NDK-stripped source: {abi}")
        detected[native_name]["original_source_sha256"] = sha(raw)
        detected[native_name]["source_transform"] = "pinned NDK llvm-objcopy --strip-unneeded"
    for license_name in NOTICES:
        path = f"assets/legal/avif-software/{license_name}"
        if detected[path]["sha256"] != prepared["notice_sha256"][f"LICENSES/{license_name}"]:
            raise ValueError("Packaged source notices differ")
    if detected["assets/legal/project/LICENSE"]["sha256"] != prepared["project_gpl_sha256"]:
        raise ValueError("Project license not packaged exactly")
    return {"kind": "ISOLATED_AVIF_PHYSICAL_APK__NOT_RELEASE", "apk_sha256": sha(apk.read_bytes()),
            "apk_bytes": apk.stat().st_size, "entries_checked": detected,
            "release_approved": False}


def main():
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="action", required=True)
    p = sub.add_parser("prepare")
    p.add_argument("source_artifacts", type=Path)
    p.add_argument("output", type=Path)
    p.add_argument("source_sha")
    p.add_argument("project_license", type=Path)
    p = sub.add_parser("check-apk")
    p.add_argument("apk", type=Path)
    p.add_argument("prepared_receipt", type=Path)
    p.add_argument("raw_native_directory", type=Path)
    p.add_argument("llvm_objcopy", type=Path)
    args = parser.parse_args()
    if args.action == "prepare":
        outcome = prepare(args.source_artifacts, args.output, args.source_sha,
                          args.project_license)
    else:
        outcome = check_apk(args.apk, json.loads(args.prepared_receipt.read_text()),
                            args.raw_native_directory, args.llvm_objcopy)
    print(json.dumps(outcome, indent=2))


if __name__ == "__main__":
    main()
