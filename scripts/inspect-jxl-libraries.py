#!/usr/bin/env python3
"""Inspect measured Android ELF libraries from a bounded libjxl source-build spike.

ELF segment alignment is a file-level preflight, NOT a 16 KiB device acceptance.
"""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
import struct
import sys
import zlib

ABIS = {
    'arm64-v8a': (183, 2, 16384),
    'armeabi-v7a': (40, 1, 4096),
    'x86_64': (62, 2, 16384),
}
MAX_FILE = 128 * 1024 * 1024


def inspect_elf(blob: bytes, abi: str) -> dict:
    if abi not in ABIS:
        raise ValueError('Unknown Android ABI')
    expected_machine, expected_class, minimum_align = ABIS[abi]
    if len(blob) < 64 or len(blob) > MAX_FILE or blob[:4] != b'\x7fELF':
        raise ValueError('Invalid or oversized ELF library')
    elf_class, encoding = blob[4], blob[5]
    if encoding != 1 or elf_class != expected_class:
        raise ValueError('Unexpected ELF class or byte order')
    if struct.unpack_from('<H', blob, 16)[0] != 3:
        raise ValueError('Expected ET_DYN shared object')
    machine = struct.unpack_from('<H', blob, 18)[0]
    if machine != expected_machine:
        raise ValueError(f'Wrong ELF machine for {abi}: {machine}')
    if elf_class == 2:
        program_offset = struct.unpack_from('<Q', blob, 32)[0]
        header_size = struct.unpack_from('<H', blob, 54)[0]
        count = struct.unpack_from('<H', blob, 56)[0]
        minimum_header = 56
        align_offset = 48
    else:
        program_offset = struct.unpack_from('<I', blob, 28)[0]
        header_size = struct.unpack_from('<H', blob, 42)[0]
        count = struct.unpack_from('<H', blob, 44)[0]
        minimum_header = 32
        align_offset = 28
    if not 0 < count <= 256 or header_size < minimum_header or program_offset + count * header_size > len(blob):
        raise ValueError('Malformed ELF program header bounds')
    load_alignments = []
    for i in range(count):
        offset = program_offset + i * header_size
        if struct.unpack_from('<I', blob, offset)[0] != 1:  # PT_LOAD
            continue
        align = struct.unpack_from('<Q' if elf_class == 2 else '<I', blob, offset + align_offset)[0]
        load_alignments.append(align)
    if not load_alignments:
        raise ValueError('ELF has no load segments')
    if any(x < minimum_align or x & (x - 1) for x in load_alignments):
        raise ValueError(f'ELF load alignment below target for {abi}: {load_alignments}')
    return {'machine': machine, 'elf_class': elf_class, 'pt_load_alignment_bytes': load_alignments,
            'alignment_target_bytes': minimum_align, 'segment_alignment_pass': True}


def inspect_build(root: Path, reference: str, requested: tuple[str, ...] | None = None) -> dict:
    if requested is None:
        requested = tuple(ABIS)
    if not requested or any(abi not in ABIS for abi in requested) or len(set(requested)) != len(requested):
        raise ValueError('Invalid ABI selection')
    output = {}
    for abi in requested:
        folder = root / f'build-{abi}'
        if not folder.is_dir():
            raise ValueError(f'Missing build directory: {abi}')
        libs = sorted(p for p in folder.rglob('*.so*') if p.is_file() and not p.is_symlink())
        if not libs:
            raise ValueError(f'No shared libraries produced for {abi}')
        measured = []
        compressed = 0
        for p in libs:
            raw = p.read_bytes()
            record = inspect_elf(raw, abi)
            measured.append({'path': str(p.relative_to(folder)), 'size_bytes': len(raw),
                             'sha256': hashlib.sha256(raw).hexdigest(), **record})
            # Illustrative compressed library sum, not an APK size delta.
            compressed += len(zlib.compress(raw, level=6))
        output[abi] = {
            'library_count': len(measured), 'total_unstripped_bytes': sum(x['size_bytes'] for x in measured),
            'independent_deflate_bytes': compressed, 'libraries': measured,
        }
    return {
        'status': 'SOURCE_BUILD_AND_ELF_PREFLIGHT_COMPLETE', 'libjxl_git_commit': reference,
        'scope': 'Raw source-built shared-library measurements for selected ABIs. NOT linked into APK, runtime-tested, '
                 'pixel-validated, independently security-audited or measured on 16 KiB device.',
        'abis': output,
    }


def main() -> int:
    if len(sys.argv) not in (3, 4):
        print('Usage: inspect-jxl-libraries.py <build-root> <pinned-commit> [all|ABI]', file=sys.stderr)
        return 2
    root = Path(sys.argv[1]).resolve()
    chosen = sys.argv[3] if len(sys.argv) == 4 else 'all'
    requested = tuple(ABIS) if chosen == 'all' else (chosen,)
    try:
        result = inspect_build(root, sys.argv[2], requested)
    except (OSError, ValueError, struct.error) as exc:
        print(f'FAIL: {exc}', file=sys.stderr)
        return 1
    evidence = root / 'evidence'
    evidence.mkdir(exist_ok=True)
    (evidence / 'jxl-probe-summary.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps({k: v for k, v in result.items() if k != 'abis'} | {
        'abis': {abi: {key: val for key, val in data.items() if key != 'libraries'} for abi, data in result['abis'].items()}
    }, indent=2))
    return 0


if __name__ == '__main__':
    sys.exit(main())
