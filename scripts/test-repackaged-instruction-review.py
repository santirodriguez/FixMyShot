#!/usr/bin/env python3
"""Keep width/label presentation review from hiding behavioral differences."""
import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('review', Path(__file__).with_name('review-repackaged-instructions.py'))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

NARROW = '''Code:
0: iconst_0
1: ifeq 7
4: ldc #CP // String value #9  retained
6: areturn
7: aconst_null
8: areturn
'''
WIDE = '''Code:
0: iconst_0
1: ifeq 8
4: ldc_w #CP // String value #9  retained
7: areturn
8: aconst_null
9: areturn
'''


class Review(unittest.TestCase):
    def test_width_and_relocated_branch_target_keep_same_instruction(self):
        self.assertEqual(m.canonical(NARROW), m.canonical(WIDE))

    def test_wrong_branch_literal_and_extra_instruction_are_not_ignored(self):
        for bad in (WIDE.replace('ifeq 8', 'ifeq 7'),
                    WIDE.replace('value #9  retained', 'value #9 retained'),
                    WIDE.replace('areturn', 'ireturn', 1),
                    WIDE.replace('7: areturn', '7: nop')):
            self.assertNotEqual(m.canonical(NARROW), m.canonical(bad))

    def test_switch_case_values_and_targets_remain_visible(self):
        text = 'Code:\n0: iconst_0\n1: lookupswitch {\n2: 9\ndefault: 10\n}\n9: aconst_null\n10: areturn\n'
        self.assertNotEqual(m.canonical(text), m.canonical(text.replace('2: 9', '3: 9')))
        self.assertNotEqual(m.canonical(text), m.canonical(text.replace('2: 9', '2: 10')))
        with self.assertRaises(ValueError):
            m.canonical(text.replace('2: 9', '2: 8'))

    def test_constructor_display_requires_verified_real_layout(self):
        a = 'p.Table$1();\ndescriptor: (Lp/Table;Ljava/nio/ByteBuffer;)V\n'
        b = 'p.Table$1(p.Table, java.nio.ByteBuffer);\ndescriptor: (Lp/Table;Ljava/nio/ByteBuffer;)V\n'
        self.assertNotEqual(m.canonical(a), m.canonical(b))
        self.assertEqual(m.canonical(a, True), m.canonical(b, True))
        self.assertNotEqual(m.canonical(a, True), m.canonical(b.replace('ByteBuffer;', 'Buffer;'), True))

    def test_handlers_and_invalid_instruction_targets_fail_closed(self):
        for text in (NARROW.replace('ifeq 7', 'ifeq 5'), NARROW+'Exception table:\n'):
            with self.assertRaises(ValueError):
                m.canonical(text)


if __name__ == '__main__':
    unittest.main()
