#!/usr/bin/env python3
import hashlib
import importlib.util
from pathlib import Path
import struct
import tempfile
import unittest
import warnings
import zipfile
import zlib

spec = importlib.util.spec_from_file_location('exposure', Path(__file__).with_name('audit-apk-tooling-exposure.py'))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


def dex(descriptor):
    # One type and one class, enough to exercise the actual DEX tables.
    data = bytearray(153+len(descriptor))
    data[:8] = b'dex\n039\0'
    for offset, value in [(32,len(data)), (36,112), (40,0x12345678),
                          (56,1), (60,112), (64,1), (68,116), (96,1), (100,120), (112,152)]:
        struct.pack_into('<I', data, offset, value)
    data[152:] = bytes([len(descriptor)])+descriptor+b'\0'
    return seal(data)


def seal(data):
    data = bytearray(data)
    struct.pack_into('<I', data, 32, len(data))
    data[12:32] = hashlib.sha1(data[32:]).digest()
    struct.pack_into('<I', data, 8, zlib.adler32(data[12:]))
    return bytes(data)


class ToolingExposure(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.apk = Path(temp.name)/'fixture.apk'

    def inspect(self, files):
        with zipfile.ZipFile(self.apk, 'w') as archive:
            for name, data in files:
                with warnings.catch_warnings():
                    warnings.simplefilter('ignore', UserWarning)
                    archive.writestr(name, data)
        return m.inspect(self.apk, 'a'*40)

    def test_absence_is_scoped_and_never_whole_security_approval(self):
        result = self.inspect([('classes.dex', dex(b'Lapp/Main;'))])
        self.assertEqual(result['status'], 'NO_MATCHING_PACKAGED_TYPES')
        self.assertFalse(result['build_tool_risk_closed'])
        self.assertFalse(result['whole_security_or_exploitability_approval'])

    def test_positive_family_and_tenth_dex_are_detected(self):
        result = self.inspect([('classes.dex', dex(b'Lapp/Main;')), ('classes10.dex', dex(b'Lio/netty/Channel;'))])
        self.assertEqual(result['status'], 'MATCHES_REQUIRE_REVIEW')
        self.assertEqual(result['families']['netty'], {'class_definitions':1, 'type_references':1})

    def test_package_prefix_boundary_is_exact(self):
        result = self.inspect([('classes.dex', dex(b'Lio/nettyfake/Class;'))])
        self.assertEqual(result['status'], 'NO_MATCHING_PACKAGED_TYPES')

    def test_duplicate_and_missing_dex_are_refused(self):
        for files in [[], [('classes.dex',dex(b'Lapp/A;'))]*2]:
            with self.subTest(files=len(files)), self.assertRaises(ValueError):self.inspect(files)

    def test_bad_integrity_and_out_of_bounds_tables_or_indices_are_refused(self):
        original = dex(b'Lapp/Main;')
        changed = bytearray(original)
        changed[-2] = ord('X')
        with self.assertRaises(ValueError):m.dex_types(bytes(changed))
        for offset, value in [(60,9999), (116,5), (120,5)]:
            changed = bytearray(original)
            struct.pack_into('<I', changed, offset, value)
            with self.subTest(offset=offset), self.assertRaises(ValueError):m.dex_types(seal(changed))

    def test_truncated_or_unsupported_payload_is_refused(self):
        for data in [b'dex\n', b'not DEX', dex(b'Lapp/Main;')[:120]]:
            with self.assertRaises(ValueError):m.dex_types(data)


if __name__ == '__main__':unittest.main()
