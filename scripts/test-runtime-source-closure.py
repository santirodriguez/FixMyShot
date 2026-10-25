#!/usr/bin/env python3
"""Test attribution and malformed input boundaries, offline."""
import importlib.util
import io
from pathlib import Path
import struct
import unittest
import zipfile

spec = importlib.util.spec_from_file_location('closure', Path(__file__).with_name('audit-runtime-source-closure.py'))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


def fixture(name='p/Foo', source='Foo.kt'):
    def utf8(s):
        raw = s.encode()
        return b'\x01'+struct.pack('>H', len(raw))+raw
    pool = utf8(name)+b'\x07\x00\x01'+utf8('java/lang/Object')+b'\x07\x00\x03'+utf8('SourceFile')+utf8(source)
    return b'\xca\xfe\xba\xbe\x00\x00\x00\x34\x00\x07'+pool+struct.pack('>7H', 0x21, 2, 4, 0, 0, 0, 1)+struct.pack('>HIH', 5, 2, 6)


def archive(entries):
    b = io.BytesIO()
    with zipfile.ZipFile(b, 'w') as z:
        for name, raw in entries:
            z.writestr(name, raw)
    return b.getvalue()


class Closure(unittest.TestCase):
    def test_sourcefile_and_internal_class_identity(self):
        self.assertEqual(m.class_source(fixture()), ('p/Foo', 'Foo.kt'))
        self.assertEqual(m.archive_classes(archive([('p/Foo.class', fixture())])), {'p/Foo': 'Foo.kt'})
        with self.assertRaises(ValueError):
            m.archive_classes(archive([('p/Other.class', fixture())]))

    def test_truncation_trailing_bytes_and_unknown_constant(self):
        raw = fixture()
        for bad in (raw[:-1], raw+b'extra', raw[:10]+b'\xff'+raw[11:]):
            with self.subTest(size=len(bad)), self.assertRaises(ValueError):
                m.class_source(bad)

    def test_nested_binary_and_unsafe_source_archive(self):
        jar = archive([('p/Foo.class', fixture())])
        self.assertEqual(m.archive_classes(archive([('classes.jar', jar)])), {'p/Foo': 'Foo.kt'})
        with self.assertRaises(ValueError):
            m.archive_classes(archive([('classes.jar', jar), ('libs/duplicate.jar', jar)]))
        with self.assertRaises(ValueError):
            m.source_keys(archive([('../Foo.kt', b'package p')]))

    def test_package_and_source_name_are_both_required(self):
        keys = m.source_keys(archive([('commonMain/p/Foo.kt', b'package p\nclass Foo'), ('jvm/q/Foo.java', b'package q; class Foo {}')]))
        self.assertEqual(keys, {('p', 'Foo.kt'), ('q', 'Foo.java')})
        self.assertNotIn(('q', 'Foo.kt'), keys)

    def test_generated_relation_requires_an_existing_origin(self):
        owners = {'p/Foo': [('x:y:1', 'Foo.kt', True)]}
        self.assertEqual(m.attribution('p/Foo$$ExternalSyntheticLambda0', owners), ('D8_GENERATED_NAME_RELATION', 'p/Foo'))
        self.assertEqual(m.attribution('p/Missing$$ExternalSyntheticLambda0', owners)[0], 'UNATTRIBUTED_REQUIRES_REVIEW')
        self.assertEqual(m.attribution('p/Foo$Other', owners)[0], 'UNATTRIBUTED_REQUIRES_REVIEW')
        self.assertEqual(m.attribution('org/cajapersonal/fixmyshot/core/Foo', {})[0], 'ACCEPTED_PROJECT_NAMESPACE')

    def test_resource_classes_require_exact_manifest_namespace(self):
        namespaces = {'p': {'coordinate': 'x:y:1', 'r_symbols_present': True},
                      'anchor': {'coordinate': 'x:z:1', 'r_symbols_present': False}}
        self.assertEqual(m.attribution('p/R$id', {}, namespaces)[0], 'AAR_NAMESPACE_RESOURCE_CLASS_NAME')
        self.assertEqual(m.attribution('anchor/R', {}, namespaces)[0], 'AAR_NAMESPACE_RESOURCE_CLASS_NAME')
        self.assertEqual(m.attribution('anchor/R$id', {}, namespaces)[0], 'UNATTRIBUTED_REQUIRES_REVIEW')
        self.assertEqual(m.attribution('other/R', {}, namespaces)[0], 'UNATTRIBUTED_REQUIRES_REVIEW')
        self.assertEqual(m.attribution('p/RealCode', {}, namespaces)[0], 'UNATTRIBUTED_REQUIRES_REVIEW')

    def test_jvm_package_override_preserves_both_source_names(self):
        keys = m.source_keys(archive([('Foo.kt', b'@file:kotlin.jvm.JvmPackageName("p.jvm")\npackage p\n')]))
        self.assertEqual(keys, {('p', 'Foo.kt'), ('p/jvm', 'Foo.kt')})


if __name__ == '__main__':
    unittest.main()
