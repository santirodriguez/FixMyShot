#!/usr/bin/env python3
"""Check symbol attribution across index changes without claiming bytecode equivalence."""
import importlib.util
from pathlib import Path
import struct
import unittest

spec = importlib.util.spec_from_file_location('symbols', Path(__file__).with_name('compare-repackaged-symbols.py'))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


def fixture(reverse=False, field='x', number=7, descriptor='I', source='Foo.java'):
    values = {'name': (1, 'p/Foo'), 'class': (7, 'name'), 'object_name': (1, 'java/lang/Object'),
              'object': (7, 'object_name'), 'field': (1, field), 'descriptor': (1, descriptor),
              'constant_name': (1, 'ConstantValue'), 'constant': (3, number),
              'source_name': (1, 'SourceFile'), 'source': (1, source)}
    order = list(values)
    if reverse:
        order.reverse()
    index = {name: i+1 for i, name in enumerate(order)}
    pool = b''
    for name in order:
        tag, value = values[name]
        pool += bytes([tag])
        if tag == 1:
            raw = value.encode()
            pool += struct.pack('>H', len(raw))+raw
        elif tag == 7:
            pool += struct.pack('>H', index[value])
        else:
            pool += struct.pack('>I', value)
    header = b'\xca\xfe\xba\xbe\x00\x00\x00\x34'+struct.pack('>H', len(order)+1)
    body = struct.pack('>5H', 0x21, index['class'], index['object'], 0, 1)
    body += struct.pack('>4H', 0x19, index['field'], index['descriptor'], 1)
    body += struct.pack('>HIH', index['constant_name'], 2, index['constant'])
    body += struct.pack('>2H', 0, 1)+struct.pack('>HIH', index['source_name'], 2, index['source'])
    return header+pool+body


class Symbols(unittest.TestCase):
    def test_index_order_changes_are_not_code_equivalence(self):
        r = m.compare(fixture(), fixture(reverse=True))
        self.assertTrue(r['declaration_layout_identical'])
        self.assertTrue(r['unique_resolved_constant_set_identical'])
        self.assertFalse(r['class_bytes_identical'])
        self.assertFalse(r['method_bodies_compared'])
        self.assertFalse(r['source_binary_equivalence'])

    def test_changed_literal_and_member_type_remain_visible(self):
        for changed in (fixture(number=8), fixture(descriptor='J'), fixture(field='y')):
            r = m.compare(fixture(), changed)
            self.assertFalse(r['declaration_layout_identical'])
            self.assertFalse(r['unique_resolved_constant_set_identical'])

    def test_debug_constant_is_not_silently_dropped(self):
        r = m.compare(fixture(), fixture(source='Other.java'))
        self.assertTrue(r['declaration_layout_identical'])
        self.assertFalse(r['unique_resolved_constant_set_identical'])

    def test_malformed_or_cyclic_input_is_refused(self):
        raw = fixture()
        for bad in (raw[:-1], raw+b'extra', raw[:10]+b'\xff'+raw[11:]):
            with self.assertRaises(ValueError):
                m.symbols(bad)
        # The first class reference points to itself rather than a UTF8 item.
        first_class = raw.index(b'\x07\x00\x01')
        bad = raw[:first_class]+b'\x07\x00\x02'+raw[first_class+3:]
        with self.assertRaises(ValueError):
            m.symbols(bad)


if __name__ == '__main__':
    unittest.main()
