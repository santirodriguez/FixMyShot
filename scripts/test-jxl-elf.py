#!/usr/bin/env python3
"""Host regressions: reject incorrect ABI, unsafe headers and insufficient alignment."""
import importlib.util
from pathlib import Path
import struct
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('jxl_elf', Path(__file__).with_name('inspect-jxl-libraries.py'))
codec = importlib.util.module_from_spec(spec)
spec.loader.exec_module(codec)


def synthetic(abi, alignment=16384, bad_machine=None, size=None, headers=2):
    machine, elf_class, _ = codec.ABIS[abi]
    byte_count = (size if size is not None else (64 + 64 * headers + 8))
    result = bytearray(byte_count)
    result[:6] = b'\x7fELF' + bytes((elf_class, 1))
    struct.pack_into('<HH', result, 16, 3, bad_machine or machine)
    if elf_class == 2:
        struct.pack_into('<Q', result, 32, 64)
        struct.pack_into('<HH', result, 54, 56, headers)
        for i in range(headers):
            off = 64 + 56 * i
            if off + 56 <= len(result):
                struct.pack_into('<I', result, off, 1)
                struct.pack_into('<Q', result, off + 48, alignment)
    else:
        struct.pack_into('<I', result, 28, 64)
        struct.pack_into('<HH', result, 42, 32, headers)
        for i in range(headers):
            off = 64 + 32 * i
            if off + 32 <= len(result):
                struct.pack_into('<I', result, off, 1)
                struct.pack_into('<I', result, off + 28, alignment)
    return bytes(result)


class ELFTests(unittest.TestCase):
    def test_arm64_valid(self):
        self.assertTrue(codec.inspect_elf(synthetic('arm64-v8a'), 'arm64-v8a')['segment_alignment_pass'])

    def test_x86_64_valid(self):
        self.assertEqual(62, codec.inspect_elf(synthetic('x86_64'), 'x86_64')['machine'])

    def test_arm32_4k_valid_under_android32_gate(self):
        self.assertEqual(4096, codec.inspect_elf(synthetic('armeabi-v7a', 4096), 'armeabi-v7a')['alignment_target_bytes'])

    def test_arm64_4k_is_not_16k_compatible(self):
        with self.assertRaises(ValueError):
            codec.inspect_elf(synthetic('arm64-v8a', 4096), 'arm64-v8a')

    def test_x86_64_4k_is_not_16k_compatible(self):
        with self.assertRaises(ValueError):
            codec.inspect_elf(synthetic('x86_64', 4096), 'x86_64')

    def test_wrong_machine_is_not_source_abi(self):
        with self.assertRaises(ValueError):
            codec.inspect_elf(synthetic('arm64-v8a', bad_machine=62), 'arm64-v8a')

    def test_truncated_elf_program_header(self):
        with self.assertRaises(ValueError):
            codec.inspect_elf(synthetic('x86_64', size=80), 'x86_64')

    def test_no_load_segments(self):
        x = bytearray(synthetic('arm64-v8a'))
        for index in (64, 120):
            struct.pack_into('<I', x, index, 0)
        with self.assertRaises(ValueError):
            codec.inspect_elf(bytes(x), 'arm64-v8a')

    def test_wrong_class(self):
        with self.assertRaises(ValueError):
            codec.inspect_elf(synthetic('arm64-v8a'), 'armeabi-v7a')

    def test_report_has_real_per_abi_files(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            for abi in codec.ABIS:
                folder = root / f'build-{abi}'
                folder.mkdir()
                (folder / 'libjxl.so').write_bytes(synthetic(abi, 4096 if abi == 'armeabi-v7a' else 16384))
            report = codec.inspect_build(root, 'pinned-sha')
            self.assertEqual('SOURCE_BUILD_AND_ELF_PREFLIGHT_COMPLETE', report['status'])
            self.assertEqual(3, len(report['abis']))
            self.assertEqual(1, report['abis']['arm64-v8a']['library_count'])
            (root / 'build-x86_64/libjxl.so').write_bytes(b'missing binary')
            with self.assertRaises(ValueError):
                codec.inspect_build(root, 'pinned-sha')


if __name__ == '__main__':
    unittest.main()
