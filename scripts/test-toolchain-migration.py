#!/usr/bin/env python3
"""Offline source-pin and all-before-mutation boundaries for the migration recipe."""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('migration', Path(__file__).with_name('prepare-toolchain-migration.py'))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


class Migration(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        self.request = json.loads((ROOT / '.ci/toolchain-migration-request.json').read_text())
        for name in self.request['inputs']:
            target = self.root / name
            target.parent.mkdir(parents=True, exist_ok=True)
            # Synthetic old-source fixture, independent of the current migrated checkout.
            data = ('\n'.join(old for old, _ in m.EDITS[name]) if name in m.EDITS else 'fixture version lock\n').encode()
            target.write_bytes(data)
            self.request['inputs'][name] = hashlib.sha256(data).hexdigest()

    def prepare(self, head=m.SOURCE, status=''):
        def git(root, *args):
            return status if args[0] == 'status' else (m.TREE if args[-1] == 'HEAD^{tree}' else head)
        with patch.object(m, 'git', side_effect=git):
            m.prepare(self.root, self.request)

    def test_exact_recipe_uses_builtin_kotlin_and_resets_only_version_locks(self):
        self.prepare()
        self.assertIn('agp = "9.2.1"', (self.root / 'gradle/libs.versions.toml').read_text())
        self.assertNotIn('kotlin.android', (self.root / 'app/build.gradle.kts').read_text())
        self.assertIn('gradle-9.4.1-bin.zip', (self.root / 'gradle/wrapper/gradle-wrapper.properties').read_text())
        self.assertTrue(all(not (self.root / p).exists() for p in m.LOCKS))

    def test_wrong_source_or_dirty_checkout_refused_before_edits(self):
        for kwargs in ({'head': 'a' * 40}, {'status': '?? unexpected'}):
            with self.subTest(kwargs=kwargs), self.assertRaises(ValueError):
                self.prepare(**kwargs)
        self.assertTrue(all((self.root / p).exists() for p in m.LOCKS))

    def test_changed_last_input_does_not_allow_partial_mutation(self):
        target = self.root / m.LOCKS[-1]
        target.write_bytes(target.read_bytes() + b'changed')
        before = (self.root / 'gradle/libs.versions.toml').read_bytes()
        with self.assertRaises(ValueError):
            self.prepare()
        self.assertEqual(before, (self.root / 'gradle/libs.versions.toml').read_bytes())
        self.assertTrue(all((self.root / p).exists() for p in m.LOCKS))

    def test_request_cannot_expand_file_scope_or_select_another_base(self):
        original = copy.deepcopy(self.request)
        self.request['inputs']['unexpected/path'] = '0' * 64
        with self.assertRaises(ValueError):
            self.prepare()
        self.request = original
        self.request['source_commit'] = '0' * 40
        with self.assertRaises(ValueError):
            self.prepare()


if __name__ == '__main__':
    unittest.main()
