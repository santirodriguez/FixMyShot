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
            self.release = dict(value, id=1, html_url="https://github.com/" + M["REPO"] + "/releases/tag/v1.0.1-test.1")
            return self.release
        if path.endswith("/assets?per_page=100"):
            return self.assets
        if method == "GET" and path.endswith("/releases/1"):
            return self.release
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
        self.request = {
            "schema":1,"state":"READY","application_id":"org.cajapersonal.fixmyshot",
            "version_name":"1.0.1-test.1","version_code":2,"tag_name":"v1.0.1-test.1",
            "compiled_source":"c"*40,"compiled_tree":"d"*40,
            "signed_apk":{"filename":"FixMyShot-1.0.1-test.1-universal.apk"},
            "source_materials":{"filename":"FixMyShot-1.0.1-test.1-source-materials.tar.gz"},
        }
        data = {self.request["signed_apk"]["filename"]: b"synthetic APK fixture",
                self.request["source_materials"]["filename"]: b"synthetic source fixture"}
        for key in ("signed_apk", "source_materials"):
            value = data[self.request[key]["filename"]]
            self.request[key].update(bytes=len(value), sha256=hashlib.sha256(value).hexdigest())
        limits = {"functional_acceptance": "INCOMPLETE", "obtainium_install_update": "NOT_RUN"}
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

    def publish(self, confirmation="PUBLISH v1.0.1-test.1"):
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

    def test_asset_added_during_upload_prevents_publication(self):
        original = self.client.download
        def change_after_readback(asset_id, path):
            original(asset_id, path)
            if asset_id == 4:
                self.client.assets.append({"id": 90, "name": "unreviewed.apk", "size": 1, "state": "uploaded"})
        self.client.download = change_after_readback
        with self.assertRaises(ValueError):
            self.publish()
        self.assertTrue(self.client.release["draft"])
        self.assertFalse(any(m == "PATCH" for m, p in self.client.writes))

    def test_staging_cannot_claim_stable_or_resolved_coverage(self):
        self.plan["prerelease"] = False
        self.write_plan()
        with self.assertRaises(ValueError):
            self.publish()

    def test_staging_run_requires_trusted_push_and_unexpired_exact_artifact(self):
        run = {"conclusion": "success", "event": "push", "head_branch": "release/1.0.0",
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

    def test_prepared_request_and_unreviewed_stable_release_are_refused(self):
        self.request["state"] = "PREPARED"
        with self.assertRaises(ValueError): self.publish()
        self.assertEqual(self.client.writes, [])
        self.request["state"] = "READY"
        self.request["version_name"] = "1.0.0"
        self.request["version_code"] = 1
        self.plan.update(prerelease=False, make_latest=True)
        self.write_plan()
        with self.assertRaises(ValueError): self.publish()
        self.assertEqual(self.client.writes, [])

    def test_reviewed_stable_release_is_latest_and_idempotent(self):
        self.request.update(version_name="1.0.0", version_code=1, tag_name="v1.0.0",
                            release_channel="stable", stable_acceptance_reviewed=True,
                            source_distribution_reviewed=True)
        for key, suffix in (("signed_apk", "-universal.apk"), ("source_materials", "-source-materials.tar.gz")):
            original = self.kit / "assets" / self.request[key]["filename"]
            self.request[key]["filename"] = "FixMyShot-1.0.0" + suffix
            original.rename(original.with_name(self.request[key]["filename"]))
        info_path = self.kit / "assets/BUILD-INFO.json"
        info = json.loads(info_path.read_text())
        info.update(version_name="1.0.0", version_code=1, signed_apk=self.request["signed_apk"],
                    coverage_limits={"functional_acceptance": "ACCEPTED",
                                     "obtainium_install_update": "NOT_RUN"},
                    source_distribution_reviewed=True)
        info_path.write_text(json.dumps(info))
        self.plan.update(prerelease=False, make_latest=True, tag_name="v1.0.0",
                         coverage_limits=info["coverage_limits"],
                         source_distribution_reviewed=True)
        assets = self.kit / "assets"
        (assets / "SHA256SUMS.txt").write_text("".join(
            hashlib.sha256(p.read_bytes()).hexdigest() + "  " + p.name + "\n"
            for p in sorted(assets.iterdir()) if p.name != "SHA256SUMS.txt"))
        rows = [{"filename": p.name, "bytes": p.stat().st_size,
                 "sha256": hashlib.sha256(p.read_bytes()).hexdigest()} for p in sorted(assets.iterdir())]
        self.plan["assets"] = rows
        self.write_plan()
        (self.kit / "review/staging-receipt.json").write_text(json.dumps({"control_commit": CONTROL, "assets": rows}))
        self.client.files = {p.name: p.read_bytes() for p in assets.iterdir()}
        self.request["stable_acceptance_reviewed"] = False
        with self.assertRaises(ValueError): self.publish("PUBLISH v1.0.0")
        self.assertEqual(self.client.writes, [])
        self.request["stable_acceptance_reviewed"] = True
        self.request["source_distribution_reviewed"] = False
        with self.assertRaises(ValueError): self.publish("PUBLISH v1.0.0")
        self.assertEqual(self.client.writes, [])
        self.request["source_distribution_reviewed"] = True
        result = self.publish("PUBLISH v1.0.0")
        self.assertEqual(result["assets_verified"], 4)
        self.assertFalse(self.client.release["prerelease"])
        self.assertEqual(self.client.release["make_latest"], "true")
        writes = self.client.writes.copy()
        self.publish("PUBLISH v1.0.0")
        self.assertEqual(self.client.writes, writes)

    def test_version_channel_policy(self):
        self.assertEqual(M["release_policy"]({"version_name": "1.0.0", "version_code": 1}),
                         {"prerelease": False, "make_latest": True})
        with self.assertRaises(ValueError):
            M["release_policy"]({"version_name": "1.0.0", "version_code": 1,
                                 "release_channel": "development-prerelease"})

    def test_public_version_sequence_refuses_private_alpha_and_code_reuse(self):
        for version, code in (("0.1.0-alpha.2", 2), ("0.1.0", 1),
                              ("1.0.0", 2), ("1.0.0-alpha.1", 1),
                              ("1.0.1", 1), ("1.0.1-test.01", 2)):
            with self.subTest(version=version, code=code), self.assertRaises(ValueError):
                M["release_policy"]({"version_name": version, "version_code": code})
        self.request["state"] = "ARCHIVED_PRIVATE_TEST_ONLY"
        with self.assertRaises(ValueError):
            self.publish()
        self.assertEqual(self.client.writes, [])

    def test_first_public_signed_receipt_matches_staging_identity(self):
        request = json.loads((ROOT / ".ci/development-release-request.json").read_text())
        signing = json.loads((ROOT / ".ci/signing-candidate-request.json").read_text())
        self.assertEqual((request["version_name"], request["version_code"]), ("1.0.0", 1))
        self.assertEqual(request["state"], "PREPARED")
        self.assertFalse(request["branding_revision_pending"])
        self.assertEqual(signing["state"], "PREPARED")
        self.assertNotEqual(request["compiled_source"], signing["source_commit"],
                            "Historical old-icon signing request must remain distinct")
        signed = json.loads((ROOT / "docs/releases/1.0.0-7649f0e-signed-build-info.json").read_text())
        self.assertEqual(request["compiled_source"], signed["source_commit"])
        self.assertEqual(request["compiled_tree"], signed["source_tree"])
        self.assertEqual(request["signed_apk"]["unsigned_sha256"], signed["unsigned_apk"]["sha256"])
        self.assertEqual(request["signed_apk"]["sha256"], signed["signed_apk"]["sha256"])
        self.assertEqual(request["signed_apk"]["certificate_sha256"],
                         signed["signing"]["certificate_sha256"])
        self.assertEqual(request["signing"]["run_id"], signed["signing"]["run_id"])
        self.assertEqual(request["sources"]["run_id"], signed["technical_source_materials"]["run_id"])
        self.assertEqual(request["evidence_receipts"]["unsigned"],
                         "1.0.0-7649f0e-unsigned-build-info.json")
        self.assertEqual(request["evidence_receipts"]["source_review"],
                         "1.0.0-7649f0e-static-source-review.json")
        self.assertIs(request["stable_acceptance_reviewed"], False)
        self.assertIs(request["source_distribution_reviewed"], False)
        self.assertEqual(request["coverage_limits"]["functional_acceptance"], "INCOMPLETE")
        self.assertEqual(request["coverage_limits"]["complete_corresponding_source_license_distribution"],
                         "OPEN")

        stage_module = runpy.run_path(str(ROOT / "scripts/prepare-development-release.py"))
        receipts = stage_module["candidate_receipts"](request)
        self.assertEqual(receipts["signed"]["signed_apk"]["sha256"], request["signed_apk"]["sha256"])
        self.assertEqual(receipts["unsigned"]["source_tree"], request["compiled_tree"])
        self.assertFalse(receipts["issuer_risk"]["full_source_distribution_approved"])
        self.assertEqual(receipts["source_review"]["distribution_disposition"]
                         ["complete_corresponding_source_and_license_legal_review"], "OPEN")
        historical = json.loads((ROOT / "docs/releases/1.0.0-signed-build-info.json").read_text())
        self.assertNotEqual(historical["source_commit"], request["compiled_source"])
        self.assertNotEqual(historical["signed_apk_sha256"], request["signed_apk"]["sha256"])

        with self.assertRaises(ValueError):
            stage_module["stage"](request, self.kit / "nonexistent-signed",
                                  self.kit / "nonexistent-source", self.kit / "prepared", CONTROL)

        for key, wrong in (("signed_apk", "0" * 64), ("source_materials", "f" * 64)):
            changed = json.loads(json.dumps(request))
            changed[key]["sha256"] = wrong
            with self.assertRaises(ValueError):
                stage_module["candidate_receipts"](changed)
        changed = json.loads(json.dumps(request))
        changed["evidence_receipts"]["signed"] = "1.0.0-signed-build-info.json"
        with self.assertRaises(ValueError):
            stage_module["candidate_receipts"](changed)
        changed = json.loads(json.dumps(request))
        changed["evidence_receipts"]["signed"] = "../untrusted.json"
        with self.assertRaises(ValueError):
            stage_module["candidate_receipts"](changed)

    def test_exact_icon1_prepared_receipts_reject_forgery(self):
        verifier = runpy.run_path(str(ROOT / "scripts/check-icon1-release-receipts.py"))
        self.assertEqual(verifier["self_test"](), 8)
        records = {key: json.loads((ROOT / path).read_text())
                   for key, path in verifier["FILES"].items()}
        self.assertEqual(verifier["validate"](records),
                         "PASS_EXACT_ICON1_OFFLINE_PROVENANCE__DISTRIBUTION_GATES_OPEN")
        records["active"]["state"] = "READY"
        with self.assertRaises(ValueError):
            verifier["validate"](records)

    def test_canonical_store_branding_and_png_specs(self):
        import struct
        primary = ROOT / "assets/branding/primary"
        alternatives = ROOT / "assets/branding/alternatives"
        listing = ROOT / "fastlane/metadata/android/en-US/images"
        for source, published, dims, color_type in (
            ("app-icon.png", "icon.png", (512, 512), 6),
            ("google-play-feature-graphic.png", "featureGraphic.png", (1024, 500), 2),
        ):
            original = (primary / source).read_bytes()
            self.assertEqual(original, (listing / published).read_bytes())
            self.assertEqual(original[:8], b"\x89PNG\r\n\x1a\n")
            self.assertEqual(original[12:16], b"IHDR")
            width, height, depth, ctype = struct.unpack(">IIBB", original[16:26])
            self.assertEqual((width, height, depth, ctype), (*dims, 8, color_type))
        self.assertLessEqual((listing / "icon.png").stat().st_size, 1024 * 1024)
        for name in (
            "app-icon-02-phone.png", "app-icon-03-editor.png",
            "app-icon-04-framing.png", "app-icon-previous-512.png",
            "app-icon-previous-source-1254.png", "app-icon-previous-192.png",
            "app-icon-previous-64.png",
        ):
            self.assertTrue((alternatives / name).is_file(), name)
        self.assertIn("assets/branding/primary/readme-header.png",
                      (ROOT / "README.md").read_text())
        self.assertIn('file("assets/branding/primary/readme-header.png")',
                      (ROOT / "app/build.gradle.kts").read_text())

    def test_branding_revision_blocks_stale_release_candidate(self):
        current = json.loads((ROOT / ".ci/development-release-request.json").read_text())
        self.assertFalse(current["branding_revision_pending"])
        forged = json.loads(json.dumps(current))
        forged["state"] = "READY"
        forged["branding_revision_pending"] = True
        stage = runpy.run_path(str(ROOT / "scripts/prepare-development-release.py"))["stage"]
        with self.assertRaisesRegex(ValueError, "Branding changed after signature"):
            stage(forged, self.kit / "nonexistent-signed",
                  self.kit / "nonexistent-sources", self.kit / "stale", CONTROL)
        self.request["branding_revision_pending"] = True
        with self.assertRaisesRegex(ValueError, "Branding changed after signature"):
            self.publish()
        self.assertEqual(self.client.writes, [])

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
        self.assertFalse(config["additionalSettings"]["includePrereleases"])
        regex = config["additionalSettings"]["apkFilterRegEx"]
        re = __import__("re")
        self.assertIsNotNone(re.fullmatch(regex, self.request["signed_apk"]["filename"]))
        for name in ("app-debug.apk", "BUILD-INFO.json", "FixMyShot-source-materials.tar.gz"):
            self.assertIsNone(re.fullmatch(regex, name))


if __name__ == "__main__":
    unittest.main()
