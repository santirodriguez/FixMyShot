#!/usr/bin/env python3
"""Refuse hidden instruction, literal and exception changes in N01 comparison."""
import importlib.util
from pathlib import Path
import unittest
import struct

spec = importlib.util.spec_from_file_location('implementation', Path(__file__).with_name('compare-repackaged-implementation.py'))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

SAMPLE = '''Compiled from "Foo.java"
public class com.google.flatbuffers.Foo {
  public int x();
    descriptor: ()I
    Code:
       0: ldc           #12 // String text #19  preserved
       2: invokevirtual #14 // Method java/lang/String.length:()I
       5: ireturn
    Exception table:
       from    to  target type
           0     5     5   Class java/lang/RuntimeException
}
'''



def bootstrap_fixture(target='metafactory', argument=7, duplicate=False):
    def utf8(value):
        raw = value.encode()
        return b'\x01'+struct.pack('>H', len(raw))+raw
    pool = [utf8('p/Foo'), b'\x07\x00\x01', utf8('java/lang/Object'), b'\x07\x00\x03',
            utf8('BootstrapMethods'), utf8('java/lang/invoke/LambdaMetafactory'), b'\x07\x00\x06',
            utf8(target), utf8('()V'), b'\x0c\x00\x08\x00\x09',
            b'\x0a\x00\x07\x00\x0a', b'\x0f\x06\x00\x0b',
            b'\x03'+struct.pack('>I', argument)]
    table = struct.pack('>4H', 1, 12, 1, 13)
    attr = struct.pack('>HI', 5, len(table))+table
    header = b'\xca\xfe\xba\xbe\x00\x00\x00\x34'+struct.pack('>H', len(pool)+1)
    body = struct.pack('>7H', 0x21, 2, 4, 0, 0, 0, 2 if duplicate else 1)
    return header+b''.join(pool)+body+attr*(2 if duplicate else 1)


class Comparison(unittest.TestCase):
    def test_indices_and_declared_relocations_only(self):
        other = SAMPLE.replace('Compiled from "Foo.java"', 'Compiled from "Different.java"').replace('#12 //', '#99 //').replace('#14 //', '#31 //').replace('com.google.flatbuffers', 'androidx.emoji2.text.flatbuffer')
        self.assertTrue(m.compare(SAMPLE, other)['normalized_disassembly_identical'])
        self.assertIn('text #19  preserved', m.normalize(SAMPLE))
        self.assertFalse(m.compare(SAMPLE, other)['formal_semantic_or_full_binary_source_equivalence'])

    def test_literal_whitespace_and_numbers_are_preserved(self):
        for before, after in [('text #19  preserved', 'text #20  preserved'), ('text #19  preserved', 'text #19 preserved')]:
            self.assertFalse(m.compare(SAMPLE, SAMPLE.replace(before, after))['normalized_disassembly_identical'])

    def test_opcodes_offsets_descriptors_members_and_handlers_remain_visible(self):
        for before, after in [('ireturn', 'areturn'), ('5: ireturn', '6: ireturn'), ('descriptor: ()I', 'descriptor: ()J'), ('public int x', 'public int y'), ('RuntimeException', 'IllegalArgumentException')]:
            self.assertFalse(m.compare(SAMPLE, SAMPLE.replace(before, after))['normalized_disassembly_identical'])

    def test_unresolved_or_dynamic_operands_fail_closed(self):
        for bad in ['0: ldc #3', '0: invokedynamic #7 // InvokeDynamic #0:x:()V', '0: bipush #3 // bad']:
            with self.assertRaises(ValueError):
                m.normalize(bad)

    def test_secondary_operands_preserved(self):
        before = '0: multianewarray #4, 2 // class "[[I"'
        self.assertEqual(m.normalize(before), m.normalize(before.replace('#4', '#9')))
        self.assertNotEqual(m.normalize(before), m.normalize(before.replace(', 2', ', 3')))


    def test_bootstrap_target_and_arguments_not_hidden(self):
        text = '0: invokedynamic #7 // InvokeDynamic #0:run:()Ljava/lang/Runnable;'
        a = m.bootstrap_methods(bootstrap_fixture())
        self.assertTrue(m.compare(text, text.replace('#7 //', '#9 //'), a, a)['normalized_disassembly_identical'])
        for raw in (bootstrap_fixture(target='other'), bootstrap_fixture(argument=8)):
            self.assertFalse(m.compare(text, text, a, m.bootstrap_methods(raw))['normalized_disassembly_identical'])

    def test_bootstrap_duplicate_and_truncation_fail_closed(self):
        for raw in (bootstrap_fixture(duplicate=True), bootstrap_fixture()[:-1], bootstrap_fixture()+b'extra'):
            with self.assertRaises(ValueError):
                m.bootstrap_methods(raw)


if __name__ == '__main__':
    unittest.main()
