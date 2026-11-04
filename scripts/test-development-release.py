#!/usr/bin/env python3
"""Exercise publication refusals and recoverable drafts without network or signing."""
import hashlib
import json
from pathlib import Path
import runpy
import tempfile
import unittest
from urllib.parse import unquote, urlparse, parse_qs

ROOT = Path(__file__).resolve().parents[1]
M = runpy.run_path(str(ROOT / "scripts/publish-development-release.py"))
CONTROL, MAIN = "a" * 40, "b" * 40
API = M["API"]


class FakeGitHub:
    def __init__(self, files):
        self.files, self.assets, self.writes = files, [], []
        self.main, self.base, self.tag, self.release = MAIN, CONTROL, None, None
        self.corrupt, self.fail_upload = False, False

    def json(self, method, path, value=None, missing=False):
        if method != "GET":
            self.writes.append((method, path))
        if path.endswith("/git/ref/heads/main"):
            return {"object": {"sha": self.main}}
        if "/compare/" in path:
            return {"status": "ahead", "merge_base_commit": {"sha": self.base}}
        if "/git/ref/tags/" in path:
            return self.tag
        if path.endswith("/git/refs"):
            self.tag = {"object": {"type": "commit", "sha": value["sha"]}}
            return self.tag
        if path.endswith("/releases?per_page=100"):
            return [self.release] if self.release else []
        if method == "POST" and path.endswith("/releases"):
            self.release = dict(value, id=1, html_url="https://github.com/" + M["REPO"] + "/releases/tag/v0.1.0-alpha.2")
            return self.release
        if path.endswith("/assets?per_page=100"):
            return self.assets
        if method == "PATCH":
            self.release.update(value)
            return self.release
        raise AssertionError((method, path))

    def upload(self, release_id, path):
        if self.fail_upload:
            self.fail_upload = False
            raise ValueError("Interrupted upload")
        self.writes.append(("UPLOAD", path.name))
        asset = {"id": len(self.assets) + 1, "name": path.name,
                 "size": path.stat().st_size, "state": "uploaded"}
        self.assets.append(asset)
        return asset

    def download(self, asset_id, path):
        name = next(a["name"] for a in self.assets if a["id"] == asset_id)
        path.write_bytes(b"changed" if self.corrupt else self.files[name])


class PublicationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.kit = Path(self.temp.name)
        (self.kit / "assets").mkdir()
        (self.kit / "review").mkdir()
        self.request = json.loads((ROOT / ".ci/development-release-request.json").read_text())
        data = {self.request["signed_apk"]["filename"]: b"synthetic APK fixture",
                self.request["source_materials"]["filename"]: b"synthetic source fixture"}
        for key in ("signed_apk", "source_materials"):
            value = data[self.request[key]["filename"]]
            self.request[key].update(bytes=len(value), sha256=hashlib.sha256(value).hexdigest())
        limits = {"functional_0_1_0_acceptance": "INCOMPLETE", "obtainium_install_update": "NOT_RUN"}
        info = {"proposed_tag_target_commit": CONTROL, "compiled_source_commit": self.request["compiled_source"],
                "compiled_source_tree": self.request["compiled_tree"], "application_id": self.request["application_id"],
                "version_name": self.request["version_name"], "version_code": self.request["version_code"],
                "signed_apk": self.request["signed_apk"], "coverage_limits": limits}
        data["BUILD-INFO.json"] = json.dumps(info).encode()
        data["SHA256SUMS.txt"] = "".join(hashlib.sha256(v).hexdigest() + "  " + k + "\n"
                                        for k, v in sorted(data.items())).encode()
        rows = [{"filename": k, "bytes": len(v), "sha256": hashlib.sha256(v).hexdigest()} for k, v in sorted(data.items())]
        for name, value in data.items():
            (self.kit / "assets" / name).write_bytes(value)
        self.plan = {"assets": rows, "prerelease": True, "make_latest": False,
                     "tag_name": self.request["tag_name"], "proposed_tag_target_commit": CONTROL,
                     "compiled_source_commit": self.request["compiled_source"], "coverage_limits": limits}
        self.write_plan()
        (self.kit / "review/staging-receipt.json").write_text(json.dumps({"control_commit": CONTROL, "assets": rows}))
        (self.kit / "review/RELEASE-NOTES.md").write_text("Caja Personal; incomplete acceptance; " + CONTROL)
        self.client = FakeGitHub(data)

    def write_plan(self):
        (self.kit / "review/release-plan.json").write_text(json.dumps(self.plan))

    def publish(self, confirmation="PUBLISH v0.1.0-alpha.2"):
        return M["publish"](self.client, self.kit, CONTROL, MAIN, self.request, confirmation)

    def test_verified_publish_and_idempotent_readback(self):
        self.assertEqual(self.publish()["assets_verified"], 4)
        self.assertFalse(self.client.release["draft"])
        self.assertTrue(self.client.release["prerelease"])
        self.assertEqual(self.client.release["make_latest"], "false")
        writes = self.client.writes.copy()
        self.publish()
        self.assertEqual(self.client.writes, writes)

    def test_changed_local_bytes_refused_before_network_writes(self):
        path = self.kit / "assets/BUILD-INFO.json"
        path.write_text(path.read_text() + "\n")
        with self.assertRaises(ValueError):
            self.publish()
        self.assertEqual(self.client.writes, [])

    def test_confirmation_main_ancestry_and_tag_refusals(self):
        with self.assertRaises(ValueError):
            self.publish("Go")
        for field in ("main", "base"):
            old = getattr(self.client, field)
            setattr(self.client, field, "c" * 40)
            with self.assertRaises(ValueError):
                self.publish()
            setattr(self.client, field, old)
        self.client.tag = {"object": {"type": "commit", "sha": "d" * 40}}
        with self.assertRaises(ValueError):
            self.publish()
        self.assertEqual(self.client.writes, [])

    def test_interrupted_upload_retains_draft_and_resumes_without_overwrite(self):
        self.client.fail_upload = True
        with self.assertRaises(ValueError):
            self.publish()
        self.assertTrue(self.client.release["draft"])
        self.publish()
        self.assertEqual(sum(m == "UPLOAD" for m, p in self.client.writes), 4)

    def test_remote_byte_change_never_publishes(self):
        self.client.corrupt = True
        with self.assertRaises(ValueError):
            self.publish()
        self.assertTrue(self.client.release["draft"])
        self.assertFalse(any(m == "PATCH" for m, p in self.client.writes))

    def test_unexpected_remote_asset_is_preserved_and_refused(self):
        self.client.fail_upload = True
        with self.assertRaises(ValueError):
            self.publish()
        self.client.assets.append({"id": 90, "name": "extra.apk", "size": 1, "state": "uploaded"})
        with self.assertRaises(ValueError):
            self.publish()
        self.assertTrue(self.client.release["draft"])
        self.assertEqual(self.client.assets[0]["name"], "extra.apk")

    def test_staging_cannot_claim_stable_or_resolved_coverage(self):
        self.plan["prerelease"] = False
        self.write_plan()
        with self.assertRaises(ValueError):
            self.publish()

    def test_staging_run_requires_trusted_push_and_unexpired_exact_artifact(self):
        run = {"conclusion": "success", "event": "push", "head_branch": "release/0.1.0",
               "path": ".github/workflows/prepare-development-release.yml",
               "repository": {"full_name": M["REPO"]}, "head_sha": CONTROL}
        artifact = {"name": "development-release-kit-" + CONTROL, "expired": False}
        class Runs:
            def json(self, method, path):
                return {"artifacts": [artifact]} if "/artifacts?" in path else run
        self.assertEqual(M["select_run"](Runs(), 1)["control"], CONTROL)
        for key, bad in (("event", "pull_request"), ("head_branch", "unreviewed"),
                         ("path", ".github/workflows/unrelated.yml"), ("conclusion", "failure")):
            old = run[key]
            run[key] = bad
            with self.assertRaises(ValueError):
                M["select_run"](Runs(), 1)
            run[key] = old
        artifact["expired"] = True
        with self.assertRaises(ValueError):
            M["select_run"](Runs(), 1)

    def test_obtainium_button_has_exact_source_prerelease_and_filter(self):
        import html
        readme = (ROOT / "README.md").read_text()
        match = __import__("re").search(r'href="([^"]+)"[^>]*><img src="docs/assets/badges/obtainium.svg"', readme)
        self.assertIsNotNone(match)
        link = html.unescape(match.group(1))
        deep = parse_qs(urlparse(link).query)["r"][0]
        config = json.loads(unquote(deep.split("obtainium://app/", 1)[1]))
        self.assertEqual(config["id"], "org.cajapersonal.fixmyshot")
        self.assertEqual(config["url"], "https://github.com/" + M["REPO"])
        self.assertTrue(config["additionalSettings"]["includePrereleases"])
        regex = config["additionalSettings"]["apkFilterRegEx"]
        re = __import__("re")
        self.assertIsNotNone(re.fullmatch(regex, self.request["signed_apk"]["filename"]))
        for name in ("app-debug.apk", "BUILD-INFO.json", "FixMyShot-source-materials.tar.gz"):
            self.assertIsNone(re.fullmatch(regex, name))


if __name__ == "__main__":
    unittest.main()
