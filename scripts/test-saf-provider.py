#!/usr/bin/env python3
"""Collector rejection checks; no Android storage or picker is executed here."""
import copy
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from urllib.parse import quote

spec = importlib.util.spec_from_file_location('saf_report', Path(__file__).with_name('check-saf-provider.py'))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
SOURCE, TOKEN = 'a' * 40, 'b' * 32


class SafReportTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.data = dict(schema=1, status='PASS_REAL_SAF_EMULATOR', api=26, source_sha=SOURCE,
                         token=TOKEN, provider='com.android.externalstorage.documents',
                         phase1_pid=100, phase2_pid=200)
        self.data.update({flag: True for flag in ('picker_cancelled', 'grant_read', 'grant_write',
            'restart_readable', 'source_hashes_unchanged', 'duplicate_free', 'revoked_reads_denied')})
        parent = 'primary:Download/FixMyShotSaf_' + TOKEN
        self.data['outputs'] = [dict(uri='content://com.android.externalstorage.documents/tree/' +
                                        quote(parent, safe='') + '/document/' + quote(parent + '/' + str(i), safe=''),
                                    sha256='c' * 64, mime=mime, side=side)
                                for i, (mime, side) in enumerate((('image/png', 60), ('image/png', 64),
                                                                  ('image/webp', 60), ('image/webp', 64)))]
        for phase in module.PHASES:
            (self.root / f'saf-{phase}.txt').write_text('OK (1 test)\nINSTRUMENTATION_CODE: -1\n')

    def check(self):
        (self.root / 'saf-provider-smoke.json').write_text(json.dumps(self.data))
        return module.validate(self.root, SOURCE, 26, TOKEN)

    def test_complete_source_bound_receipt(self):
        self.assertEqual(4, len(self.check()['outputs']))

    def test_partial_or_wrong_source_receipt(self):
        for key, value in (('status', 'PHASE1_PASS'), ('source_sha', 'd' * 40), ('api', 28), ('token', 'e' * 32)):
            with self.subTest(key=key):
                original = self.data[key]
                self.data[key] = value
                with self.assertRaises(ValueError): self.check()
                self.data[key] = original

    def test_failed_or_skipped_instrumentation(self):
        for extra in ('FAILURES!!!', 'INSTRUMENTATION_FAILED', 'Process crashed', 'AssumptionViolated'):
            path = self.root / f'saf-{module.PHASES[1]}.txt'
            path.write_text('OK (1 test)\nINSTRUMENTATION_CODE: -1\n' + extra)
            with self.assertRaises(ValueError): self.check()

    def test_missing_phase(self):
        (self.root / f'saf-{module.PHASES[1]}.txt').unlink()
        with self.assertRaises(OSError): self.check()

    def test_no_process_change_or_non_boolean_flags(self):
        self.data['phase2_pid'] = self.data['phase1_pid']
        with self.assertRaises(ValueError): self.check()
        self.data['phase2_pid'] = 200
        self.data['revoked_reads_denied'] = 1
        with self.assertRaises(ValueError): self.check()

    def test_duplicate_or_wrong_geometry(self):
        original = copy.deepcopy(self.data['outputs'])
        self.data['outputs'][1]['uri'] = self.data['outputs'][0]['uri']
        with self.assertRaises(ValueError): self.check()
        self.data['outputs'] = original
        self.data['outputs'][0]['side'] = 36
        with self.assertRaises(ValueError): self.check()

    def test_foreign_provider_and_invalid_digest(self):
        self.data['outputs'][0]['uri'] = 'content://other.invalid/tree/x'
        with self.assertRaises(ValueError): self.check()
        self.data['outputs'][0]['uri'] = 'content://com.android.externalstorage.documents/tree/x'
        self.data['outputs'][0]['sha256'] = 'not-a-sha256'
        with self.assertRaises(ValueError): self.check()


if __name__ == '__main__':
    unittest.main()
