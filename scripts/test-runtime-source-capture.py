#!/usr/bin/env python3
"""Exercise source-staging boundaries with synthetic archives; no network."""
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import stat
import tempfile
import unittest
from unittest.mock import patch
import warnings
import zipfile

spec = importlib.util.spec_from_file_location('source_capture', Path(__file__).with_name('capture-runtime-sources.py'))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


def archive(entries):
    out = io.BytesIO()
    with warnings.catch_warnings(), zipfile.ZipFile(out, 'w') as z:
        warnings.simplefilter('ignore', UserWarning)
        for name, raw in entries:
            z.writestr(name, raw)
    return out.getvalue()


class Sources(unittest.TestCase):
    def test_source_and_empty_are_distinct(self):
        r = m.inspect(archive([('p/Foo.kt', b'package p'), ('META-INF/LICENSE', b'notice')]))
        self.assertEqual(r['source_files'], 1)
        self.assertEqual(r['notice_paths'], ['META-INF/LICENSE'])
        self.assertEqual(m.inspect(archive([('META-INF/MANIFEST.MF', b'v1')]))['source_files'], 0)

    def test_unsafe_members_and_duplicate_are_refused(self):
        for path in ('../bad.kt', '/bad.kt', 'a\\bad.kt', 'C:bad.kt'):
            with self.subTest(path=path), self.assertRaises(ValueError):
                m.inspect(archive([(path, b'x')]))
        with self.assertRaises(ValueError):
            m.inspect(archive([('a.kt', b'x'), ('a.kt', b'y')]))
        link = zipfile.ZipInfo('linked.kt')
        link.create_system = 3
        link.external_attr = (stat.S_IFLNK | 0o777) << 16
        with self.assertRaises(ValueError):
            m.inspect(archive([(link, b'target')]))

    def test_crc_and_limits_are_refused(self):
        data = archive([('a.kt', b'uniquepayload')])
        with self.assertRaises(zipfile.BadZipFile):
            m.inspect(data.replace(b'uniquepayload', b'changedbytes!'))
        with patch.object(m, 'MAX_ARCHIVE', 8), self.assertRaises(ValueError):
            m.inspect(data)
        with patch.object(m, 'MAX_EXPANDED', 8), self.assertRaises(ValueError):
            m.inspect(data)

    def test_urls_and_redirects_are_restricted(self):
        for url in ('http://dl.google.com/a', 'https://evil.test/a', 'https://dl.google.com@evil.test/a',
                    'https://dl.google.com:443/a', 'https://dl.google.com/a?x=1'):
            with self.subTest(url=url), self.assertRaises(ValueError):
                m.checked_url(url)
        with self.assertRaises(ValueError):
            m.Redirect().redirect_request(None, None, 302, '', {}, 'https://evil.test/a')

    def test_selection_is_pinned_and_empty_companion_not_retried(self):
        root = Path(__file__).resolve().parent.parent
        paths = ('docs/feasibility/evidence/gradle-baseline-f9434ab9.xml', 'app/gradle.lockfile', 'LICENSES/runtime-components.json')
        data = [(root/p).read_bytes() for p in paths]
        pins = [hashlib.sha256(b).hexdigest() for b in data]
        rows = m.plan(*data, *pins)
        self.assertEqual(len(rows), 133)
        self.assertEqual(sum('source_url' in r for r in rows), 94)
        self.assertEqual(sum(not r['frozen_binary_sha256'] for r in rows), 38)
        self.assertNotIn('source_url', next(r for r in rows if r['coordinate']==m.KNOWN_EMPTY))
        with self.assertRaises(ValueError):
            m.plan(*data, '0'*64, *pins[1:])
        catalog = json.loads(data[2])
        catalog['components'].pop()
        changed = json.dumps(catalog).encode()
        with self.assertRaises(ValueError):
            m.plan(data[0], data[1], changed, pins[0], pins[1], m.digest(changed))

    def test_capture_failure_is_recorded_without_retry_and_output_reuse_refused(self):
        row = {'coordinate': 'x:y:1', 'source_url': 'https://dl.google.com/a', 'relative_archive': 'archives/a.jar'}
        with tempfile.TemporaryDirectory() as d, patch.object(m.urllib.request, 'build_opener') as opener:
            opener.return_value.open.side_effect = OSError('fixture unavailable')
            result = m.capture([row], Path(d)/'fresh')
            self.assertEqual(result[0]['status'], 'CAPTURE_FAILED_NOT_RETRIED')
            self.assertEqual(opener.return_value.open.call_count, 1)
            with self.assertRaises(FileExistsError):
                m.capture([row], Path(d)/'fresh')


if __name__ == '__main__':
    unittest.main()
