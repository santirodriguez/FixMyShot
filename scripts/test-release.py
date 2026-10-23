#!/usr/bin/env python3
"""Offline regression checks for release metadata boundaries; no APK/signing acceptance."""
import copy
import json
from pathlib import Path
import runpy
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
MODULE = runpy.run_path(str(ROOT / "scripts/check-release.py"))


class ReleaseChecks(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        for name in ("docs/releases/ledger.json", "docs/releases/TEMPLATE.md", "app/build.gradle.kts", "README.md", "CHANGELOG.md"):
            dest = self.root / name
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_text((ROOT / name).read_text())
        self.ledger = json.loads((self.root / "docs/releases/ledger.json").read_text())

    def write(self):
        (self.root / "docs/releases/ledger.json").write_text(json.dumps(self.ledger))

    def check(self):
        self.write()
        return MODULE["validate"](self.root)

    def test_current_identity_and_unpublished_render(self):
        _, template, fields = self.check()
        output = self.root / "draft.md"
        MODULE["render_notes"](template, fields, output)
        text = output.read_text()
        self.assertNotIn("{{", text)
        self.assertIn("UNPUBLISHED DRAFT", text)
        self.assertIn("EDIT", text)
        self.assertIn("FixMyShot-0.1.0-alpha.1-universal.apk", text)
        with self.assertRaises(FileExistsError):
            MODULE["render_notes"](template, fields, output)
        self.assertEqual(text, output.read_text())

    def test_invalid_and_reused_codes(self):
        for code in (0, -1, True, "1", 2):
            with self.subTest(code=code), self.assertRaises(ValueError):
                self.ledger["releases"][0]["version_code"] = code
                self.check()

    def test_invalid_versions(self):
        for name in ("v0.1.0", "00.1.0", "0.1", "0.1.0-alpha.01", "0.1.0/evil", "0.1.0+untracked"):
            with self.subTest(name=name), self.assertRaises(ValueError):
                self.ledger["releases"][0]["version_name"] = name
                self.check()

    def test_source_version_mismatch(self):
        path = self.root / "app/build.gradle.kts"
        text = path.read_text()
        for old, new in (("versionCode = 1", "versionCode = 2"), ('versionName = "0.1.0-alpha.1"', 'versionName = "0.1.0-alpha.2"'),
                         ('applicationId = "org.cajapersonal.fixmyshot"', 'applicationId = "org.cajapersonal.fixmyshot.debug"')):
            path.write_text(text.replace(old, new))
            with self.subTest(field=old), self.assertRaises(ValueError):
                self.check()

    def test_missing_and_unknown_template_fields(self):
        path = self.root / "docs/releases/TEMPLATE.md"
        original = path.read_text()
        for text in (original.replace("{{CODE}}", "1"), original + "{{UNREVIEWED}}"):
            path.write_text(text)
            with self.assertRaises(ValueError):
                self.check()

    def test_changelog_status_mismatch(self):
        (self.root / "CHANGELOG.md").write_text("# Changelog\n\n## 0.1.0-alpha.1\n")
        with self.assertRaises(ValueError):
            self.check()

    def history(self):
        first = self.ledger["releases"][0]
        first.update(status="distributed", signed_apk_sha256="a" * 64,
                     certificate_sha256="b" * 64, source_commit="c" * 40)
        self.ledger["releases"].append(dict(version_name="0.1.0-alpha.2", version_code=2, status="planned"))
        for name in ("app/build.gradle.kts", "README.md", "CHANGELOG.md"):
            path = self.root / name
            path.write_text(path.read_text().replace("0.1.0-alpha.1", "0.1.0-alpha.2").replace("versionCode = 1", "versionCode = 2"))
        return first

    def test_sequential_history_and_required_receipts(self):
        first = self.history()
        self.check()
        for field in ("source_commit", "signed_apk_sha256", "certificate_sha256"):
            value = first.pop(field)
            with self.subTest(field=field), self.assertRaises(ValueError):
                self.check()
            first[field] = value

    def test_duplicate_or_nonmonotonic_history(self):
        first = self.history()
        candidate = copy.deepcopy(self.ledger["releases"][1])
        for field, value in (("version_code", 1), ("version_name", first["version_name"]), ("status", "unknown")):
            self.ledger["releases"][1] = dict(candidate, **{field: value})
            with self.subTest(field=field), self.assertRaises(ValueError):
                self.check()

    def test_distributed_bytes_or_certificate_cannot_be_reused_or_changed(self):
        first = self.history()
        second = self.ledger["releases"][1]
        second.update(status="distributed", signed_apk_sha256="d" * 64,
                      certificate_sha256="b" * 64, source_commit="e" * 40)
        (self.root / "CHANGELOG.md").write_text("# Changelog\n\n## 0.1.0-alpha.2\n")
        self.check()
        second["signed_apk_sha256"] = first["signed_apk_sha256"]
        with self.assertRaises(ValueError):
            self.check()
        second["signed_apk_sha256"] = "d" * 64
        second["certificate_sha256"] = "f" * 64
        with self.assertRaises(ValueError):
            self.check()

    def test_only_latest_may_be_planned(self):
        self.history()
        self.ledger["releases"][0]["status"] = "planned"
        with self.assertRaises(ValueError):
            self.check()


if __name__ == "__main__":
    unittest.main()
