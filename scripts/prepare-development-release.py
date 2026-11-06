#!/usr/bin/env python3
"""Stage only an explicitly accepted, source-bound release kit; never publish."""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import shutil
import runpy
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


def candidate_receipts(request):
    """Resolve current candidate receipts, never historical same-version defaults."""
    names = request.get("evidence_receipts")
    require(type(names) is dict and set(names) == {"signed", "unsigned", "issuer_risk", "source_review"},
            "Missing exact candidate receipt mapping")
    receipts = {}
    for kind, value in names.items():
        name = filename(value)
        require(name.startswith(request["version_name"] + "-") and name.endswith(".json"),
                "Unsafe or stale candidate receipt path")
        path = ROOT / "docs/releases" / name
        require(path.is_file() and not path.is_symlink(), "Missing candidate receipt: " + kind)
        receipts[kind] = json.loads(path.read_text(encoding="utf-8"))

    signed, unsigned = receipts["signed"], receipts["unsigned"]
    risk, review = receipts["issuer_risk"], receipts["source_review"]
    source, tree = request["compiled_source"], request["compiled_tree"]
    apk, sources = request["signed_apk"], request["source_materials"]
    require(signed.get("status") == "SIGNED_AND_INDEPENDENTLY_INSPECTED__NOT_FINAL_RELEASE_ACCEPTANCE"
            and unsigned.get("status") == "UNSIGNED_VERIFIED__NOT_FINAL_RELEASE_ACCEPTANCE"
            and risk.get("status") == "ARTIFACT_SPECIFIC_RESIDUAL_ISSUER_RISK_ACCEPTED",
            "Candidate evidence is incomplete or wrong type")
    for record in (signed, unsigned):
        require(record["source_commit"] == source and record["source_tree"] == tree,
                "Candidate source/tree differs")
        require((record["version_name"], record["version_code"]) ==
                (request["version_name"], request["version_code"]),
                "Candidate version differs")
        require(record["application_id"] == request["application_id"],
                "Candidate application ID differs")
    require(risk["compiled_source"] == source and risk["compiled_tree"] == tree
            and risk["application_id"] == request["application_id"]
            and (risk["version_name"], risk["version_code"]) ==
            (request["version_name"], request["version_code"]),
            "Assurance source/version differs")
    require(risk["production_signing_authorized"] is True
            and risk["production_signing_executed"] is True
            and risk["merge_release_or_deployment_authorized"] is False,
            "Risk receipt signing or publication authority differs")
    classifications = risk["issuer_classifications"]
    require(classifications["reviewed"] + classifications["pending"] == classifications["frozen_files"]
            and risk["classifications_changed"] is False,
            "Issuer provenance classifications differ")
    require(signed["signed_apk"] == {"filename": apk["filename"],
            "bytes": apk["bytes"], "sha256": apk["sha256"]},
            "Exact signed APK receipt differs")
    require(signed["unsigned_apk"]["sha256"] == apk["unsigned_sha256"]
            and unsigned["unsigned_apk"]["sha256"] == apk["unsigned_sha256"]
            and signed["unsigned_apk"]["bytes"] == unsigned["unsigned_apk"]["bytes"]
            and risk["unsigned_apk"] == {"bytes": signed["unsigned_apk"]["bytes"],
            "sha256": apk["unsigned_sha256"]},
            "Unsigned APK binding differs")
    require(signed["signing"]["certificate_sha256"] == apk["certificate_sha256"]
            and signed["signing"]["run_id"] == request["signing"]["run_id"]
            and signed["signing"]["conclusion"] == "success"
            and signed["signing"]["original_actions_artifact_id"] ==
            request["signing"]["artifact_id"],
            "Signature or original signed artifact differs")
    # Both archived assurance schemas bind the same executable and certificate.
    # Check every present field instead of inventing legacy names in icon1.
    for field, expected in (("signed_apk_sha256", apk["sha256"]),
                            ("completed_signed_apk_sha256", apk["sha256"]),
                            ("certificate_sha256", apk["certificate_sha256"]),
                            ("completed_certificate_sha256", apk["certificate_sha256"])):
        if field in risk:
            require(risk[field] == expected, "Original assurance signing proof differs: " + field)
    require(("signed_apk_sha256" in risk or "completed_signed_apk_sha256" in risk)
            and ("certificate_sha256" in risk or "completed_certificate_sha256" in risk),
            "Original assurance signing identity missing")
    inspection = signed["independent_inspection"]
    for key in ("signature_v2_verified_against_embedded_certificate",
                "signature_v3_verified_against_embedded_certificate",
                "signature_v2_content_digest_recomputed",
                "signature_v3_content_digest_recomputed",
                "original_unsigned_apk_sha256_rechecked",
                "signed_apk_sha256_rechecked"):
        require(inspection.get(key) is True, "Signed APK verification incomplete: " + key)
    require(unsigned["baseline"]["workflow_run"] == risk["baseline"]["workflow_run"]
            and unsigned["independent_unsigned_rebuild"]["byte_identical"] is True
            and unsigned["independent_unsigned_rebuild"]["workflow_run"] == risk["independent_build_run"]
            and risk["identical_unsigned_bytes"] is True,
            "Unsigned independent reproduction not proven")
    # Old receipts separately bind the baseline and the rebuild; the immutable
    # icon1 receipt binds the actual independently rebuilt signed input.
    signed_input = signed["unsigned_apk"]
    if "baseline_run_id" in signed_input or "independent_rebuild_run_id" in signed_input:
        require(signed_input.get("baseline_run_id") == unsigned["baseline"]["workflow_run"]
                and signed_input.get("independent_rebuild_run_id") ==
                unsigned["independent_unsigned_rebuild"]["workflow_run"],
                "Historical signed-input build provenance differs")
    else:
        require(signed_input.get("producer_run_id") == risk["independent_build_run"]
                and signed_input.get("original_artifact_id") == risk["independent_build_artifact_id"],
                "Exact icon1 signed-input independent rebuild differs")
    materials = unsigned["corresponding_source_materials"]
    require(materials["workflow_run"] == request["sources"]["run_id"]
            and materials["source_materials_sha256"] == sources["sha256"]
            and materials["source_materials_archive_bytes"] == sources["bytes"]
            and materials["source_kit_sha256"] == sources["project_kit_sha256"]
            and risk["source_materials_sha256"] == sources["sha256"],
            "Source archive provenance differs")
    candidate = review["candidate"]
    require(candidate["compiled_source"] == source and candidate["compiled_tree"] == tree
            and candidate["signed_apk_sha256"] == apk["sha256"]
            and candidate["unsigned_apk_sha256"] == apk["unsigned_sha256"]
            and candidate["certificate_sha256"] == apk["certificate_sha256"]
            and review["original_artifacts"]["source_materials"]["source_tar_sha256"] ==
            sources["sha256"], "Source and APK review differs")
    # Validate both exact icon1 states. The original signer/risk/source receipts
    # remain immutable historical evidence; only a distinct later release
    # approval can authorize READY without erasing their original NOT_APPROVED.
    if source == "7649f0e002aca9c91cdf1294ab3033a9572029a9":
        draft_path = ROOT / "docs/releases/1.0.0-7649f0e-release-inputs-draft.json"
        require(draft_path.is_file() and not draft_path.is_symlink(),
                "Missing exact icon1 release inputs draft")
        draft = json.loads(draft_path.read_text(encoding="utf-8"))
        validator = runpy.run_path(str(ROOT / "scripts/check-icon1-release-receipts.py"))
        decision_path = ROOT / validator["FILES"]["approval"]
        require(decision_path.is_file() and not decision_path.is_symlink(),
                "Missing separately authorized icon1 release decision")
        decision = json.loads(decision_path.read_text(encoding="utf-8"))
        verdict = validator["validate"](dict(draft=draft, signed=signed, unsigned=unsigned,
                                             risk=risk, review=review, active=request,
                                             approval=decision))
        if request.get("state") == "READY":
            require(verdict == "PASS_EXACT_ICON1_READY_BOUNDED_APPROVAL__PUBLISHING_NOT_YET_DONE",
                    "Ready release has no separately reviewed source and functional acceptance")
    return receipts


def stage(request, signed_root, source_root, output, control):
    require(not output.exists(), "Output directory must be fresh")
    require(request.get("state") == "READY", "Release inputs are not ready")
    require(request.get("branding_revision_pending") is not True,
            "Branding changed after signature: rebuild and re-sign exact APK before release staging")
    policy = runpy.run_path(str(ROOT / "scripts/publish-development-release.py"))["release_policy"](request)
    require(request["schema"] == 1 and request["application_id"] == "org.cajapersonal.fixmyshot",
            "Unexpected request identity")
    require(re.fullmatch(r"[0-9a-f]{40}", control or "") is not None, "Invalid control commit")
    require(re.fullmatch(r"[0-9a-f]{40}", request["compiled_source"]) is not None,
            "Invalid compiled source")
    require(re.fullmatch(r"[0-9a-f]{40}", request["compiled_tree"]) is not None,
            "Invalid compiled tree")
    version = request["version_name"]
    require(re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+(?:-[A-Za-z0-9.-]+)?", version or "") is not None,
            "Release version required")
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
    receipts = candidate_receipts(request)
    signed_receipt = receipts["signed"]
    unsigned_receipt = receipts["unsigned"]
    risk = receipts["issuer_risk"]
    source_review = receipts["source_review"]
    limitations = {
        "functional_acceptance": "INCOMPLETE",
        "actual_16k_runtime": "NOT_RUN",
        "full_100_item_mixed_failure_exercise": "NOT_RUN",
        "exhaustive_human_locale_review": "NOT_RUN",
        "full_accessibility_review": "NOT_RUN",
        "wider_independent_privacy_color_review": "NOT_RUN",
        "detailed_settings_default_retention": "NOT_RUN",
        "obtainium_install_update": "NOT_RUN",
    }
    if not policy["prerelease"]:
        require(request.get("stable_acceptance_reviewed") is True,
                "Stable acceptance has not been reviewed")
        limitations = request["coverage_limits"]
        require(limitations.get("functional_acceptance") == "ACCEPTED", "Stable acceptance incomplete")
        require(request.get("source_distribution_reviewed") is True,
                "Corresponding-source/license distribution is not approved")
        if request["compiled_source"] == "7649f0e002aca9c91cdf1294ab3033a9572029a9":
            # Same frozen original historical reviews remain NOT_APPROVED as of
            # their respective capture dates. The separate later approval is
            # required and verified by candidate_receipts() for this READY state.
            require(risk["full_source_distribution_approved"] is False
                    and source_review["distribution_disposition"]
                    ["complete_corresponding_source_and_license_legal_review"] == "OPEN",
                    "Original source-risk evidence was retroactively rewritten")
        else:
            require(risk["full_source_distribution_approved"] is True
                    and source_review["distribution_disposition"]
                    ["complete_corresponding_source_and_license_legal_review"] == "APPROVED",
                    "Corresponding-source/license distribution is not approved")
    build_info = {
        "schema": 1, "status": "SIGNED_AND_VERIFIED_DEVELOPMENT_CANDIDATE" if policy["prerelease"] else "SIGNED_AND_VERIFIED_STABLE_CANDIDATE",
        "channel": "development-prerelease" if policy["prerelease"] else "stable", "application_id": request["application_id"],
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
            "maintainer_report": signed_receipt.get("maintainer_acceptance", "NOT_RUN"),
            "artifact_specific_assurance": unsigned_receipt["artifact_specific_assurance"],
            "pending_publisher_classifications_retained": True,
            "dependency_authentication": unsigned_receipt["artifact_specific_assurance"],
            "fresh_install": signed_receipt.get("physical_signed_fresh_install", "NOT_RUN"),
            "same_certificate_upgrade": signed_receipt.get("sequential_public_version_update", "NOT_RUN"),
            "obtainium_install_and_update": "NOT_RUN",
            "signed_16k_emulator_launch": (
                "PASS_ORIGINAL_SIGNED_APK_API35_16384B_LAUNCH_RELAUNCH__NATIVE_EXPORT_NOT_TESTED"
                if request["compiled_source"] == "7649f0e002aca9c91cdf1294ab3033a9572029a9"
                else "NOT_RUN"),
            "first_public_source_distribution_review": (
                "CONDITIONAL_SEPARATE_2026_11_06_REVIEW__SAME_RELEASE_SOURCE_REQUIRED"
                if request["compiled_source"] == "7649f0e002aca9c91cdf1294ab3033a9572029a9"
                else "NOT_RUN"),
            "remaining_limitations": limitations,
        },
        "coverage_limits": limitations,
        "source_distribution_reviewed": request.get("source_distribution_reviewed") is True,
        "evidence_receipts": request["evidence_receipts"],
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
        "schema": 1, "prepared_channel": "development-prerelease" if policy["prerelease"] else "stable", "prerelease": policy["prerelease"],
        "make_latest": policy["make_latest"], "tag_name": request["tag_name"], "proposed_tag_target_commit": control,
        "compiled_source_commit": request["compiled_source"],
        "compiled_source_tree": request["compiled_tree"], "assets": rows,
        "integration_snapshot": request["integration_snapshot"],
        "proposed_integration_sequence": ["Review the current candidate PR and this exact prepared kit",
            "Separately authorize the candidate branch into main",
            "Separately authorize the manual publishing workflow with this successful staging run ID"],
        "integration_gate": "The exact packaging-control commit must be contained in main before publication. Staging does not merge, tag or publish.",
        "tag_source_explanation": "The tag identifies current documentation/packaging control. BUILD-INFO and the nested source kit identify the exact earlier compiled Git source; no rebuild of the tag is claimed.",
        "obtainium_source": "https://github.com/santirodriguez/FixMyShot",
        "obtainium_apk_filter": r"^FixMyShot-.*-universal\.apk$",
        "coverage_limits": limitations,
        "source_distribution_reviewed": request.get("source_distribution_reviewed") is True,
        "merge_tag_publication_authorized_by_staging": False,
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

