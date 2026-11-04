#!/usr/bin/env python3
"""Check native ABI inventory and ELF/ZIP 16 KiB alignment on actual APK bytes."""
import argparse
import json
from pathlib import Path
import struct
import zipfile

def inspect(path: Path) -> dict:
    libraries = []
    expected = {"arm64-v8a", "armeabi-v7a", "x86_64"}
    with zipfile.ZipFile(path) as apk, path.open("rb") as raw:
        for member in apk.infolist():
            if not (member.filename.startswith("lib/") and member.filename.endswith(".so")):
                continue
            abi = member.filename.split("/")[1]
            data = apk.read(member)
            assert data[:4] == b"\x7fELF" and data[5] == 1, member.filename
            bits = data[4]
            if bits == 2:
                phoff = struct.unpack_from("<Q", data, 32)[0]
                entsize, count = struct.unpack_from("<HH", data, 54)
                offset_field, vaddr_field, align_field, fmt = 8, 16, 48, "<Q"
            else:
                assert bits == 1, member.filename
                phoff = struct.unpack_from("<I", data, 28)[0]
                entsize, count = struct.unpack_from("<HH", data, 42)
                offset_field, vaddr_field, align_field, fmt = 4, 8, 28, "<I"
            loads = 0
            for i in range(count):
                base = phoff + i * entsize
                if struct.unpack_from("<I", data, base)[0] != 1:
                    continue
                offset = struct.unpack_from(fmt, data, base + offset_field)[0]
                virtual = struct.unpack_from(fmt, data, base + vaddr_field)[0]
                alignment = struct.unpack_from(fmt, data, base + align_field)[0]
                assert alignment >= 16384 and (virtual - offset) % 16384 == 0, member.filename
                loads += 1
            assert loads > 0, member.filename
            raw.seek(member.header_offset)
            header = raw.read(30)
            assert header[:4] == b"PK\x03\x04"
            name_len, extra_len = struct.unpack_from("<HH", header, 26)
            data_offset = member.header_offset + 30 + name_len + extra_len
            if member.compress_type == zipfile.ZIP_STORED:
                assert data_offset % 16384 == 0, member.filename
            libraries.append({"path": member.filename, "abi": abi, "bytes": member.file_size,
                              "compressed_bytes": member.compress_size, "data_offset": data_offset})
    assert {item["abi"] for item in libraries} == expected, "Missing or unexpected ABI"
    for abi in expected:
        assert any(item["path"] == f"lib/{abi}/libfixmyshot_webp.so" for item in libraries), abi
    return {"apk": path.name, "apk_bytes": path.stat().st_size, "elf_zip_alignment": "PASS", "libraries": libraries}

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path)
    args = parser.parse_args()
    print(json.dumps(inspect(args.apk), indent=2))
