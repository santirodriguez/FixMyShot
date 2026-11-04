#!/usr/bin/env python3
"""Stage an exact signed development prerelease kit; no build, signing or publication."""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import shutil
import tarfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def require(ok, message):
    if not ok:
        raise ValueError(message)


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def filename(value):
    require(isinstance(value, str) and re.fullmatch(r"[A-Za-z0-9_.-]+", value)
            and value not in (".", ".."), "Unsafe asset filename")
    return value


def checked(path, record):
    require(path.is_file() and not path.is_symlink(), "Missing or linked input")
    require(type(record["bytes"]) is int and 0 < record["bytes"] <= 128 * 1024 * 1024,
            "Invalid bounded input size")
    require(re.fullmatch(r"[0-9a-f]{64}", record["sha256"]) is not None, "Invalid input digest")
    require(path.stat().st_size == record["bytes"] and digest(path) == record["sha256"],
            "Pinned input bytes differ: " + path.name)


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def stage(request, signed_root, source_root, output, control):
    require(not output.exists(), "Output directory must be fresh")
    require(request["schema"] == 1 and request["application_id"] == "org.cajapersonal.fixmyshot",
            "Unexpected request identity")
    require(re.fullmatch(r"[0-9a-f]{40}", control or "") is not None, "Invalid control commit")
    require(re.fullmatch(r"[0-9a-f]{40}", request["compiled_source"]) is not None,
            "Invalid compiled source")
    require(re.fullmatch(r"[0-9a-f]{40}", request["compiled_tree"]) is not None,
            "Invalid compiled tree")
    version = request["version_name"]
    require(re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+-[A-Za-z0-9.-]+", version or "") is not None,
            "Development prerelease version required")
    require(request["tag_name"] == "v" + version and type(request["version_code"]) is int
            and request["version_code"] > 0, "Version/tag mismatch")
    apk_record, source_record = request["signed_apk"], request["source_materials"]
    require(apk_record["filename"] == "FixMyShot-" + version + "-universal.apk", "APK name mismatch")
    apk = signed_root / filename(apk_record["filename"])
    sources = source_root / filename(source_record["input_filename"])
    checked(apk, apk_record)
    checked(sources, source_record)
    with zipfile.ZipFile(apk) as archive:
        names = archive.namelist()
        require(len(names) == len(set(names)) and archive.testzip() is None, "Invalid APK ZIP")
    original = json.loads((signed_root / "BUILD-INFO.json").read_text())
    for key, expected in {
        "source_commit": request["compiled_source"], "version_name": version,
        "version_code": request["version_code"], "package": request["application_id"],
        "signed_apk_sha256": apk_record["sha256"], "signed_apk_bytes": apk_record["bytes"],
        "unsigned_apk_sha256": apk_record["unsigned_sha256"],
        "certificate_sha256": apk_record["certificate_sha256"],
    }.items():
        require(original.get(key) == expected, "Signed receipt differs: " + key)
    with tarfile.open(sources) as archive:
        members = archive.getmembers()
        names = [m.name for m in members]
        require(len(names) == len(set(names)), "Duplicate source member")
        require(sum(m.size for m in members) <= 128 * 1024 * 1024, "Oversized source members")
        for member in members:
            path = PurePosixPath(member.name)
            require(member.isfile() and not path.is_absolute() and ".." not in path.parts
                    and "\\" not in member.name, "Unsafe source member")
        manifest = archive.extractfile("MANIFEST.sha256").read().decode().splitlines()
        verified = set()
        for line in manifest:
            expected, name = line.split("  ", 1)
            require(name not in verified and name in names, "Unexpected source manifest member")
            actual = hashlib.sha256(archive.extractfile(name).read()).hexdigest()
            require(actual == expected, "Source member digest differs")
            verified.add(name)
        require(verified == set(names) - {"MANIFEST.sha256"}, "Incomplete source manifest")
        record = json.load(archive.extractfile("SOURCE-MATERIALS.json"))
        require(record["compiled_source"] == request["compiled_source"], "Source binding differs")
        kit = archive.extractfile("project-and-webp-source.tar.gz").read()
        require(hashlib.sha256(kit).hexdigest() == source_record["project_kit_sha256"],
                "Nested project/WebP kit differs")
    signed_receipt = json.loads((ROOT / "docs/releases" / (version + "-signed-build-info.json")).read_text())
    unsigned_receipt = json.loads((ROOT / "docs/releases" / (version + "-unsigned-build-info.json")).read_text())
    require(signed_receipt["signed_apk_sha256"] == apk_record["sha256"], "Repository receipt differs")
    require(unsigned_receipt["source_commit"] == request["compiled_source"], "Unsigned source differs")
    limitations = {
        "functional_0_1_0_acceptance": "INCOMPLETE",
        "actual_16k_runtime": "NOT_RUN",
        "full_100_item_mixed_failure_exercise": "NOT_RUN",
        "exhaustive_human_locale_review": "NOT_RUN",
        "full_accessibility_review": "NOT_RUN",
        "wider_independent_privacy_color_review": "NOT_RUN",
        "detailed_settings_default_retention": "NOT_RUN",
        "obtainium_install_update": "NOT_RUN",
    }
    build_info = {
        "schema": 1, "status": "SIGNED_AND_VERIFIED_DEVELOPMENT_CANDIDATE",
        "channel": "development-prerelease", "application_id": request["application_id"],
        "version_name": version, "version_code": request["version_code"],
        "source_commit": request["compiled_source"],
        "compiled_source_commit": request["compiled_source"], "compiled_source_tree": request["compiled_tree"],
        "git_tag": request["tag_name"], "proposed_tag_target_commit": control,
        "tag_is_documentation_and_packaging_control_not_compiled_apk_source": True,
        "unsigned_apk_sha256": apk_record["unsigned_sha256"], "signed_apk": apk_record,
        "corresponding_source": {
            "filename": source_record["filename"], "bytes": source_record["bytes"],
            "sha256": source_record["sha256"], "compiled_source_commit": request["compiled_source"],
            "restore_instructions": "Extract source materials, then the nested project/WebP kit; use its README and Git identity restoration helper before rebuilding.",
        },
        "actual_toolchain": unsigned_receipt["toolchain"],
        "verification": {
            "core_tests": 99, "api36_instrumented_tests": 135,
            "independent_unsigned_rebuild": unsigned_receipt["independent_unsigned_rebuild"],
            "independent_signature_inspection": signed_receipt["independent_inspection"],
            "maintainer_report": signed_receipt["maintainer_acceptance"],
            "artifact_specific_assurance": unsigned_receipt["artifact_specific_assurance"],
            "pending_publisher_classifications_retained": True,
            "dependency_authentication": unsigned_receipt["artifact_specific_assurance"],
            "fresh_install": "NOT_RUN_ALPHA2_FRESH_INSTALL",
            "same_certificate_upgrade": signed_receipt["sequential_upgrade_test"],
            "obtainium_install_and_update": "NOT_RUN",
            "remaining_limitations": limitations,
        },
        "coverage_limits": limitations,
        "publication_state": "Not asserted by this immutable build receipt; inspect the actual GitHub release.",
    }
    assets, review = output / "assets", output / "review"
    assets.mkdir(parents=True)
    review.mkdir()
    shutil.copyfile(apk, assets / filename(apk_record["filename"]))
    shutil.copyfile(sources, assets / filename(source_record["filename"]))
    write_json(assets / "BUILD-INFO.json", build_info)
    sums = "".join(digest(p) + "  " + p.name + "\n" for p in sorted(assets.iterdir()))
    (assets / "SHA256SUMS.txt").write_text(sums, encoding="utf-8")
    rows = [{"filename": p.name, "bytes": p.stat().st_size, "sha256": digest(p)}
            for p in sorted(assets.iterdir())]
    require(re.fullmatch(r"FixMyShot-.*-universal\.apk", apk_record["filename"]) is not None,
            "Obtainium APK filter mismatch")
    notes = (ROOT / "docs/releases" / (version + "-notes.md")).read_text()
    notes = notes.replace("{{CONTROL_COMMIT}}", control)
    require("{{" not in notes and "<!-- EDIT" not in notes, "Unresolved notes field")
    (review / "RELEASE-NOTES.md").write_text(notes, encoding="utf-8")
    plan = {
        "schema": 1, "prepared_channel": "development-prerelease", "prerelease": True,
        "make_latest": False, "tag_name": request["tag_name"], "proposed_tag_target_commit": control,
        "compiled_source_commit": request["compiled_source"],
        "compiled_source_tree": request["compiled_tree"], "assets": rows,
        "integration_snapshot": request["integration_snapshot"],
        "proposed_integration_sequence": ["Separately authorize PR1 into release/0.1.0",
            "Prepare/review release/0.1.0 into main; recheck the recorded default-branch snapshot",
            "After authorized integration, publish only the reviewed assets and tag the exact packaging-control commit"],
        "integration_gate": "PR1 targets release/0.1.0, not main. Separate authority must resolve default-branch integration before publication; staging changes neither branch.",
        "tag_source_explanation": "The tag identifies current documentation/packaging control. BUILD-INFO and the nested source kit identify the exact earlier compiled Git source; no rebuild of the tag is claimed.",
        "obtainium_source": "https://github.com/santirodriguez/FixMyShot",
        "obtainium_apk_filter": r"^FixMyShot-.*-universal\.apk$",
        "coverage_limits": limitations, "merge_tag_publication_authorized_by_staging": False,
    }
    write_json(review / "release-plan.json", plan)
    receipt = {
        "schema": 1, "scope": "EXACT_DEVELOPMENT_RELEASE_ASSET_STAGING_ONLY",
        "control_commit": control, "producer_sha256": digest(Path(__file__)),
        "compiled_source_commit": request["compiled_source"], "assets": rows,
        "source_manifest_members_verified": len(verified),
        "original_signed_apk_preserved": True, "original_source_archive_preserved": True,
        "build_signing_tag_merge_release_performed": False,
    }
    write_json(review / "staging-receipt.json", receipt)
    print(json.dumps(receipt, indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("request", "signed_root", "source_root", "output"):
        parser.add_argument(name, type=Path)
    parser.add_argument("--control-commit", required=True)
    args = parser.parse_args()
    stage(json.loads(args.request.read_text()), args.signed_root, args.source_root,
          args.output, args.control_commit)
