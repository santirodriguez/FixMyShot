#!/usr/bin/env python3
import importlib.util
import io
from pathlib import Path
import tempfile
import unittest
import zipfile

spec = importlib.util.spec_from_file_location('licenses', Path(__file__).with_name('audit-runtime-licenses.py'))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


class Licenses(unittest.TestCase):
    def test_wrong_inventory_pin_fails_before_route_or_network(self):
        with self.assertRaises(ValueError):
            m.inventory(b'not metadata', b'not lock', '0'*64, '0'*64)
        with self.assertRaises(ValueError):
            m.origin('unreviewed.publisher', 'name', '1', 'pom')
        with self.assertRaises(ValueError):
            m.origin('androidx.test', '../outside', '1', 'pom')

    def test_parent_only_does_not_invent_an_inherited_license(self):
        report = m.declarations(b'<project><parent><groupId>example</groupId></parent></project>')
        self.assertEqual(report['licenses'], [])
        self.assertEqual(report['status'], 'PARENT_OR_UPSTREAM_LICENSE_REVIEW_REQUIRED')
        self.assertEqual(report['parent']['groupId'], 'example')

    def test_entities_and_non_pom_content_refused(self):
        for data in (b'<!DOCTYPE project [<!ENTITY x "expanded">]><project/>', b'<not-a-pom/>'):
            with self.assertRaises(ValueError):
                m.declarations(data)

    def test_nested_notice_bytes_preserved_without_extracting_archive_paths(self):
        nested = io.BytesIO()
        with zipfile.ZipFile(nested, 'w') as z:
            z.writestr('META-INF/NOTICE.txt', b'Exact upstream notice\n')
        outer = io.BytesIO()
        with zipfile.ZipFile(outer, 'w') as z:
            z.writestr('classes.jar', nested.getvalue())
        with tempfile.TemporaryDirectory() as folder:
            out = Path(folder);(out/'notices').mkdir()
            rows = m.notices(outer.getvalue(), 'fixture.aar', out)
            self.assertEqual(len(rows), 1)
            self.assertEqual((out/'notices'/rows[0]['sha256']).read_bytes(), b'Exact upstream notice\n')
            self.assertEqual(rows[0]['member'], 'fixture.aar!/classes.jar!/META-INF/NOTICE.txt')

    def test_unsafe_zip_path_is_refused_even_if_not_a_notice(self):
        data = io.BytesIO()
        with zipfile.ZipFile(data, 'w') as z:
            z.writestr('../outside', b'data')
        with tempfile.TemporaryDirectory() as folder, self.assertRaises(ValueError):
            m.notices(data.getvalue(), 'fixture.jar', Path(folder))


if __name__ == '__main__':
    unittest.main()
