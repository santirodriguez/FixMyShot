#!/usr/bin/env python3
"""Validate only synthetic debug-emulator 16 KiB and privacy evidence.

This is an independent PNG/WebP container and EXIF parser (stdlib only), not
proof of a signed APK/device test or a complete image-decoder audit.
"""
import json
from pathlib import Path
import struct
import sys
import zlib

MAX_BYTES = 16 * 1024 * 1024
PNG_MAGIC = b"\x89PNG\r\n\x1a\n"
PROHIBITED = (b"PRIVATE-MAKER", b"PRIVATE-COMMENT", b"PRIVATE-SERIAL",
              b"PRIVATE-EDITOR", b"PRIVATE-PATH", b"PRIVATE-ID", b"PRIVATE-XMP")


def require(condition, message):
    if not condition:
        raise ValueError(message)


def bounded(path):
    require(path.is_file() and not path.is_symlink(), f"Missing/symlink evidence: {path.name}")
    require(0 < path.stat().st_size <= MAX_BYTES, f"Invalid evidence size: {path.name}")
    return path.read_bytes()


def png_chunks(data):
    require(data.startswith(PNG_MAGIC), "Bad PNG signature")
    pos, chunks, end = len(PNG_MAGIC), [], False
    while pos + 12 <= len(data):
        size = struct.unpack_from(">I", data, pos)[0]
        require(size <= MAX_BYTES and pos + 12 + size <= len(data), "PNG chunk exceeds bounds")
        name = data[pos + 4:pos + 8]
        content = data[pos + 8:pos + 8 + size]
        crc = struct.unpack_from(">I", data, pos + 8 + size)[0]
        require((zlib.crc32(name + content) & 0xffffffff) == crc, "PNG chunk CRC mismatch")
        chunks.append((name, content))
        pos += 12 + size
        if name == b"IEND":
            end = True
            break
    require(end and pos == len(data), "PNG missing IEND or has trailing bytes")
    require(chunks[0][0] == b"IHDR" and len(chunks[0][1]) == 13, "PNG IHDR missing")
    require(any(name == b"IDAT" for name, _ in chunks), "PNG IDAT missing")
    return chunks


def webp_chunks(data):
    require(len(data) >= 20 and data[:4] == b"RIFF" and data[8:12] == b"WEBP", "Bad WebP header")
    require(struct.unpack_from("<I", data, 4)[0] + 8 == len(data), "WebP RIFF size mismatch")
    pos, chunks = 12, []
    while pos + 8 <= len(data):
        name = data[pos:pos + 4]
        size = struct.unpack_from("<I", data, pos + 4)[0]
        require(size <= MAX_BYTES and pos + 8 + size + (size & 1) <= len(data), "WebP chunk exceeds bounds")
        chunks.append((name, data[pos + 8:pos + 8 + size]))
        pos += 8 + size + (size & 1)
    require(pos == len(data), "WebP trailing/incomplete chunk")
    require(any(name in (b"VP8 ", b"VP8L", b"ANIM") for name, _ in chunks), "WebP image data missing")
    return chunks


def read_ifds(exif):
    if exif.startswith(b"Exif\0\0"):
        exif = exif[6:]
    require(len(exif) >= 8 and exif[:2] in (b"II", b"MM"), "EXIF TIFF header missing")
    order = "<" if exif[:2] == b"II" else ">"
    require(struct.unpack_from(order + "H", exif, 2)[0] == 42, "EXIF TIFF magic mismatch")

    def u32(pos):
        require(0 <= pos <= len(exif) - 4, "EXIF pointer outside buffer")
        return struct.unpack_from(order + "I", exif, pos)[0]

    def ifd(offset):
        require(8 <= offset <= len(exif) - 2, "EXIF IFD pointer invalid")
        count = struct.unpack_from(order + "H", exif, offset)[0]
        require(count <= 256 and offset + 2 + count * 12 + 4 <= len(exif), "EXIF IFD exceeds bounds")
        tags = {}
        widths = {1: 1, 2: 1, 3: 2, 4: 4, 5: 8, 7: 1, 9: 4, 10: 8}
        for index in range(count):
            pos = offset + 2 + index * 12
            tag, kind = struct.unpack_from(order + "HH", exif, pos)
            qty = u32(pos + 4)
            require(tag not in tags and kind in widths and qty <= MAX_BYTES // widths[kind], "Invalid EXIF tag")
            size = widths[kind] * qty
            value = exif[pos + 8:pos + 8 + size] if size <= 4 else exif[u32(pos + 8):u32(pos + 8) + size]
            require(len(value) == size, "Truncated EXIF field")
            tags[tag] = (kind, value, u32(pos + 8))
        return tags

    top = ifd(u32(4))
    require(len(top) > 0, "Empty EXIF IFD")
    gps = ifd(top[0x8825][2]) if 0x8825 in top else {}
    detail = ifd(top[0x8769][2]) if 0x8769 in top else {}
    return top, gps, detail


def inspect_output(path, fmt, preserved):
    data = bounded(path)
    chunks = png_chunks(data) if fmt == "png" else webp_chunks(data)
    for marker in PROHIBITED:
        require(marker not in data, f"Forbidden synthetic marker leaked: {marker.decode()}")
    exifs = [value for name, value in chunks if name in (b"eXIf", b"EXIF")]
    require(len(exifs) <= 1, "Duplicate EXIF containers")
    require(not any(name in (b"iTXt", b"tEXt", b"zTXt", b"XMP ") and b"PRIVATE" in content
                    for name, content in chunks), "Unexpected identifying metadata chunk")
    top, gps, detail = read_ifds(exifs[0]) if exifs else ({}, {}, {})
    if preserved:
        require(len(exifs) == 1, "Preservation missing EXIF")
        require(0x010f in top and b"Synthetic camera" in top[0x010f][1], "Preserved camera Make missing")
        require(0x8825 in top and all(tag in gps for tag in (1, 2, 3, 4)), "Preserved GPS fields missing")
    else:
        require(0x8825 not in top and not gps, "Default output retained GPS IFD")
        require(all(tag not in top for tag in (0x010f, 0x0110, 0x013b, 0x8298)),
                "Default output retained identifying camera/author fields")
    require(0x9286 not in detail and 0x927c not in detail, "UserComment/MakerNote EXIF tag retained")
    return {"bytes": len(data), "container": fmt, "exif": bool(exifs), "gps": bool(gps)}


def validate(folder):
    shell = bounded(folder / "page-size-shell.txt").decode("ascii").strip()
    require(shell == "16384", f"Real shell page size not 16 KiB: {shell}")
    api = bounded(folder / "android-api.txt").decode("ascii").strip()
    abi = bounded(folder / "android-abi.txt").decode("ascii").strip()
    require(api == "35" and abi == "x86_64", f"Unexpected emulator: API {api}, ABI {abi}")
    log = bounded(folder / "instrumentation.txt").decode("utf-8")
    require("OK (9 tests)" in log and "INSTRUMENTATION_CODE: -1" in log
            and "FAILURES!!!" not in log and "INSTRUMENTATION_FAILED" not in log,
            "The expected nine instrumentation tests did not pass")
    report = json.loads(bounded(folder / "g1-smoke.json"))
    require(report.get("schema") == 2 and report.get("status") == "PASS_SYNTHETIC_SMOKE_ONLY"
            and report.get("api") == 35 and report.get("page_size_bytes") == 16384,
            "In-app measured 16 KiB page size / smoke status not confirmed")
    expected = {(n, f, it) for n in range(20) for f in ("PNG", "WEBP_LOSSLESS") for it in range(2)}
    observed = set()
    for sample in report.get("samples", []):
        key = (sample["fixture"], sample["format"], sample["iteration"])
        require(key in expected and key not in observed and sample.get("pixels") == "PASS",
                "Missing/duplicate/failed PNG or WebP decoded-pixel test")
        require(sample.get("route") == ("ANDROID_PNG" if key[1] == "PNG" else "ANDROID_WEBP_LOSSLESS"),
                "Unexpected API35 codec route (debug build)")
        observed.add(key)
    require(observed == expected, "Expected 80 real synthetic PNG/WebP encodes")
    png_chunks(bounded(folder / "g1-fixture-0.png"))
    webp_chunks(bounded(folder / "g1-fixture-0.webp"))
    native = webp_chunks(bounded(folder / "native-webp-16k.webp"))
    require(any(name == b"VP8L" for name, _ in native), "JNI WebP was not a lossless VP8L file")
    details = {}
    for fmt in ("png", "webp"):
        details[f"default-{fmt}"] = inspect_output(folder / f"privacy-default.{fmt}", fmt, False)
        details[f"preserve-{fmt}"] = inspect_output(folder / f"privacy-preserve.{fmt}", fmt, True)
    return {"status": "PASS_SYNTHETIC_DEBUG_ONLY", "api": 35, "page_size_bytes": 16384,
            "abi": abi, "smoke_encodes": len(observed), "jni_lossless_webp": True,
            "synthetic_privacy_outputs": details,
            "signed_apk_tested": False, "corresponding_source_legal_approval": False}


def main():
    try:
        folder = Path(sys.argv[1] if len(sys.argv) == 2 else "evidence/16k-privacy")
        outcome = validate(folder)
        (folder / "host-validation.json").write_text(json.dumps(outcome, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        print("PASS: 16 KiB shell+process, 80 image encodes, direct JNI WebP, four synthetic metadata outputs (debug build)")
    except (ValueError, OSError, KeyError, TypeError, struct.error, UnicodeError) as error:
        sys.exit(f"FAIL 16k-privacy: {error}")


if __name__ == "__main__":
    main()
