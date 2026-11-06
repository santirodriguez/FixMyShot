#!/usr/bin/env python3
"""Offline, read-only cross-receipt preflight for the exact FixMyShot icon1 1.0.0.

This cannot authorize source distribution, change the active release request,
or replace the separate stable release staging/approval validation.
"""
import argparse
import copy
import hashlib
import json
from pathlib import Path

SOURCE = '7649f0e002aca9c91cdf1294ab3033a9572029a9'
TREE = 'a222fb9964e2f328c59acf352e23e67e45650744'
SIGNED = '92efebea8ebfce6a88865a8b4842f61e4362d8951fb7949ea626c0a0245ea428'
UNSIGNED = 'bc1cc4f0ca0be8e1469081b323da760d576d87b608bc5d5a59b6d162993776e2'
SOURCES = '07ec6845cb4e9dda7731a854f8130cf87b9803e920c8f02f376ab55ea879cd3a'
HISTORICAL = '0a1b88ee6ebbbfaaa4752540b28f45e11dbe50efa30e229bea5fec766851fcfd'
CERT = '33e626f0980ffd263e24e688a372ff840fd877f400f2a9ea5db5759655bbf445'

FILES = {
    'draft': 'docs/releases/1.0.0-7649f0e-release-inputs-draft.json',
    'signed': 'docs/releases/1.0.0-7649f0e-signed-build-info.json',
    'risk': 'docs/releases/1.0.0-7649f0e-assurance.json',
    'unsigned': 'docs/releases/1.0.0-7649f0e-unsigned-build-info.json',
    'review': 'docs/releases/1.0.0-7649f0e-static-source-review.json',
    'active': '.ci/development-release-request.json',
    'approval': 'docs/releases/1.0.0-7649f0e-final-release-approval.json',
}


def require(condition, explanation):
    if not condition:
        raise ValueError(explanation)


def validate(r):
    draft, signed, risk = r['draft'], r['signed'], r['risk']
    unsigned, review, active = r['unsigned'], r['review'], r['active']
    identity = ('org.cajapersonal.fixmyshot', '1.0.0', 1)
    for label, record in (('signed', signed), ('unsigned', unsigned)):
        require((record['application_id'], record['version_name'], record['version_code']) == identity,
                label + ': wrong package/version')
        require((record['source_commit'], record['source_tree']) == (SOURCE, TREE),
                label + ': wrong source/tree')
    require((risk['compiled_source'], risk['compiled_tree']) == (SOURCE, TREE), 'wrong assurance source')
    require(risk['status'] == 'ARTIFACT_SPECIFIC_RESIDUAL_ISSUER_RISK_ACCEPTED', 'wrong risk class')
    require(unsigned['status'] == 'UNSIGNED_VERIFIED__NOT_FINAL_RELEASE_ACCEPTANCE', 'unsupported unsigned status')
    require(signed['signed_apk']['sha256'] == SIGNED and signed['signed_apk']['bytes'] == 12675989,
            'wrong signed APK')
    require(signed['unsigned_apk']['sha256'] == UNSIGNED and signed['unsigned_apk']['bytes'] == 12629654,
            'signed input unsigned identity differs')
    require(unsigned['unsigned_apk']['sha256'] == UNSIGNED and unsigned['unsigned_apk']['bytes'] == 12629654,
            'independently rebuilt unsigned identity differs')
    require(signed['signing']['certificate_sha256'] == CERT and
            risk['completed_signed_apk_sha256'] == SIGNED, 'signing identity differs')
    require(signed['signing']['run_id'] == 42226227569 and
            signed['signing']['original_actions_artifact_id'] == 13504789246,
            'signed artifact provenance differs')
    base = unsigned['baseline']
    require(base['workflow_run'] == risk['baseline']['workflow_run'] == 42132693754,
            'baseline run differs')
    require(base['original_run_conclusion'] == risk['baseline']['original_run_conclusion'] == 'failure',
            'historical PR-merge failure was falsified')
    require(base['pr_merge_comparison'] == 'FAIL_NON_EQUIVALENT_EMBEDDED_REVISION',
            'missing historical comparison distinction')
    require(unsigned['source_input_discrepancy']['historical_expected_unsigned_sha256'] == HISTORICAL and
            unsigned['source_input_discrepancy']['actual_signed_input_unsigned_sha256'] == UNSIGNED,
            'source request and actual signed input were conflated')
    rebuild = unsigned['independent_unsigned_rebuild']
    require(rebuild['workflow_run'] == risk['independent_build_run'] == 42134905825 and
            rebuild['artifact_id'] == risk['independent_build_artifact_id'] == 13465741212 and
            rebuild['byte_identical'] is True and risk['identical_unsigned_bytes'] is True,
            'exact clean-source independent rebuild not proven')
    sources = unsigned['corresponding_source_materials']
    require(sources['workflow_run'] == risk['source_materials_run'] == 42132693740 and
            sources['artifact_id'] == risk['source_materials_artifact_id'] == 13463158916 and
            sources['source_materials_sha256'] == risk['source_materials_sha256'] == SOURCES and
            sources['source_materials_archive_bytes'] == 39548907,
            'wrong original source material identity')
    cand = review['candidate']
    require((cand['compiled_source'], cand['compiled_tree']) == (SOURCE, TREE) and
            (cand['unsigned_apk_sha256'], cand['signed_apk_sha256'], cand['certificate_sha256']) ==
            (UNSIGNED, SIGNED, CERT), 'source review candidate identity differs')
    require(review['original_artifacts']['source_materials']['source_tar_sha256'] == SOURCES,
            'source review source tar differs')
    require(review['distribution_disposition']['complete_corresponding_source_and_license_legal_review'] == 'OPEN',
            'legal approval cannot be inferred from technical receipts')
    require(risk['full_source_distribution_approved'] is False and
            risk['publication_authorized'] is False, 'assurance must remain non-publishing')
    require(draft['status'] == 'PREPARED_INPUTS_ONLY_NOT_WORKFLOW_REQUEST' and
            draft['compiled_candidate']['compiled_source'] == SOURCE and
            draft['signed_apk']['sha256'] == SIGNED and
            draft['source_materials']['sha256'] == SOURCES, 'draft input differs')
    require(active['state'] in ('PREPARED', 'READY') and
            active['compiled_source'] == SOURCE and active['compiled_tree'] == TREE and
            active['signed_apk']['sha256'] == SIGNED and
            active['signed_apk']['unsigned_sha256'] == UNSIGNED and
            active['signed_apk']['certificate_sha256'] == CERT and
            active['signing']['run_id'] == 42226227569 and
            active['signing']['artifact_id'] == 13504789246 and
            active['sources']['run_id'] == 42132693740 and
            active['sources']['artifact_id'] == 13463158916 and
            active['source_materials']['sha256'] == SOURCES and
            active['evidence_receipts'] == {
                'signed': '1.0.0-7649f0e-signed-build-info.json',
                'unsigned': '1.0.0-7649f0e-unsigned-build-info.json',
                'issuer_risk': '1.0.0-7649f0e-assurance.json',
                'source_review': '1.0.0-7649f0e-static-source-review.json'} and
            active['branding_revision_pending'] is False,
            'Exact icon1 request differs from frozen original executable and source')
    if active['state'] == 'PREPARED':
        require(active['stable_acceptance_reviewed'] is False and
                active['source_distribution_reviewed'] is False and
                active['coverage_limits']['functional_acceptance'] == 'INCOMPLETE',
                'PREPARED state improperly approved')
        return 'PASS_EXACT_ICON1_OFFLINE_PROVENANCE__DISTRIBUTION_GATES_OPEN'
    # Original archive-era reviews remain immutable. A distinct, later
    # release-tagged decision may approve only this bounded first public scope.
    decision = r['approval']
    candidate = decision['candidate']
    distribution = decision['source_distribution']
    runtime = decision['signed_runtime']
    functional = decision['functional_acceptance']
    constraints = decision['release_constraints']
    require(decision['status'] == 'CONDITIONAL_FIRST_PUBLIC_GITHUB_RELEASE_APPROVAL' and
            decision['authorization'] == 'MAINTAINER_GO_TO_CONDITIONAL_FULL_FIRST_PUBLIC_RELEASE_2026-11-06',
            'Missing separate maintainer release approval')
    require((candidate['source_commit'], candidate['source_tree'],
             candidate['signed_apk_sha256'], candidate['unsigned_apk_sha256'],
             candidate['source_tar_sha256'], candidate['certificate_sha256']) ==
            (SOURCE, TREE, SIGNED, UNSIGNED, SOURCES, CERT),
            'Release approval candidate not bound to immutable originals')
    require(distribution['review'] == 'BOUNDED_RELEASE_DISTRIBUTION_REVIEW_APPROVED' and
            distribution['original_binary_archives_inventory'] == 95 and
            distribution['original_embedded_license_members'] == 57 and
            distribution['original_embedded_NOTICE_members'] == 0 and
            distribution['original_apk_legal_assets_verified'] == 8 and
            distribution['mandatory_missing_legal_material_discovered'] is False and
            distribution['halt_on_new_mandatory_notice_or_source_gap'] is True and
            distribution['source_offer_requirement'] == 'PUBLIC_APK_AND_IDENTICAL_SOURCE_TAR_ON_SAME_RELEASE',
            'Source distribution review insufficient or source offer waived')
    review_path = distribution['supplemental_review_path']
    require(review_path == 'docs/releases/1.0.0-7649f0e-final-source-license-disposition.md',
            'Source review document locator differs')
    document = (Path(__file__).resolve().parents[1] / review_path).read_bytes()
    require(hashlib.sha1(b'blob ' + str(len(document)).encode() + b'\0' + document).hexdigest() ==
            distribution['supplemental_review_git_blob_sha'] ==
            '47b8ad6f121e3ce0a67702944137cc9a4ec725c9',
            'Actual reviewed source disposition document has changed')
    require(runtime['status'] ==
            'PASS_SIGNED_APK_INSTALL_LAUNCH_RELAUNCH_EMULATED_16K__NATIVE_EXPORT_NOT_EXERCISED' and
            runtime['run_id'] == 42292629807 and
            runtime['evidence_artifact_id'] == 13540536980 and
            runtime['runtime_page_size_bytes'] == 16384 and
            runtime['physical_device_16k'] == 'NOT_RUN' and
            runtime['signed_native_end_to_end_export'] == 'NOT_RUN',
            'Signed-device proof is overstated')
    require(functional['scope'] == 'ACCEPTED_BOUNDED_FIRST_PUBLIC_GITHUB_1_0_0' and
            functional['full_100_mixed_batch_stress'] == 'NOT_RUN' and
            functional['independent_signed_output_metadata_color'] == 'NOT_RUN' and
            functional['no_claim_of_exhaustive_manual_pass'] is True and
            constraints['requires_successful_exact_4_asset_stage_ci'] is True and
            constraints['requires_approved_final_current_PR_merge_ref_CI'] is True and
            constraints['requires_published_and_downloadable_apk_and_corresponding_source_with_matching_sha'] is True,
            'Manual gaps or public source/CI conditions silently waived')
    limits = active['coverage_limits']
    require(active['stable_acceptance_reviewed'] is True and
            active['source_distribution_reviewed'] is True and
            limits['functional_acceptance'] == 'ACCEPTED' and
            limits['actual_16k_runtime'] == 'PASS_SIGNED_EMULATOR_16K_LAUNCH_ONLY' and
            limits['full_100_item_mixed_failure_exercise'] == 'NOT_RUN' and
            limits['wider_independent_privacy_color_review'] == 'NOT_RUN' and
            limits['signed_physical_install'] == 'NOT_RUN' and
            limits['obtainium_install_update'] == 'NOT_RUN' and
            limits['complete_corresponding_source_license_distribution'] ==
            'REVIEW_APPROVED_WITH_CONDITIONS__PUBLIC_SOURCE_ACCESS_REQUIRED',
            'READY coverage limits hide untested behavior or source obligations')
    return 'PASS_EXACT_ICON1_READY_BOUNDED_APPROVAL__PUBLISHING_NOT_YET_DONE'


def fixture():
    """Small, deliberately synthetic positive model for mutation self-tests."""
    return {
        'draft': {'status': 'PREPARED_INPUTS_ONLY_NOT_WORKFLOW_REQUEST',
                  'compiled_candidate': {'compiled_source': SOURCE},
                  'signed_apk': {'sha256': SIGNED}, 'source_materials': {'sha256': SOURCES}},
        'signed': {'application_id': 'org.cajapersonal.fixmyshot', 'version_name': '1.0.0',
                   'version_code': 1, 'source_commit': SOURCE, 'source_tree': TREE,
                   'signed_apk': {'sha256': SIGNED, 'bytes': 12675989},
                   'unsigned_apk': {'sha256': UNSIGNED, 'bytes': 12629654},
                   'signing': {'certificate_sha256': CERT, 'run_id': 42226227569,
                               'original_actions_artifact_id': 13504789246}},
        'risk': {'compiled_source': SOURCE, 'compiled_tree': TREE,
                 'status': 'ARTIFACT_SPECIFIC_RESIDUAL_ISSUER_RISK_ACCEPTED',
                 'completed_signed_apk_sha256': SIGNED, 'baseline': {'workflow_run': 42132693754,
                 'original_run_conclusion': 'failure'}, 'independent_build_run': 42134905825,
                 'independent_build_artifact_id': 13465741212, 'identical_unsigned_bytes': True,
                 'source_materials_run': 42132693740, 'source_materials_artifact_id': 13463158916,
                 'source_materials_sha256': SOURCES, 'full_source_distribution_approved': False,
                 'publication_authorized': False},
        'unsigned': {'application_id': 'org.cajapersonal.fixmyshot', 'version_name': '1.0.0',
                     'version_code': 1, 'source_commit': SOURCE, 'source_tree': TREE,
                     'status': 'UNSIGNED_VERIFIED__NOT_FINAL_RELEASE_ACCEPTANCE',
                     'unsigned_apk': {'sha256': UNSIGNED, 'bytes': 12629654},
                     'baseline': {'workflow_run': 42132693754, 'original_run_conclusion': 'failure',
                                  'pr_merge_comparison': 'FAIL_NON_EQUIVALENT_EMBEDDED_REVISION'},
                     'source_input_discrepancy': {'historical_expected_unsigned_sha256': HISTORICAL,
                                                  'actual_signed_input_unsigned_sha256': UNSIGNED},
                     'independent_unsigned_rebuild': {'workflow_run': 42134905825,
                                                     'artifact_id': 13465741212, 'byte_identical': True},
                     'corresponding_source_materials': {'workflow_run': 42132693740,
                                                         'artifact_id': 13463158916,
                                                         'source_materials_sha256': SOURCES,
                                                         'source_materials_archive_bytes': 39548907}},
        'review': {'candidate': {'compiled_source': SOURCE, 'compiled_tree': TREE,
                                 'unsigned_apk_sha256': UNSIGNED,
                                 'signed_apk_sha256': SIGNED, 'certificate_sha256': CERT},
                   'original_artifacts': {'source_materials': {'source_tar_sha256': SOURCES}},
                   'distribution_disposition': {'complete_corresponding_source_and_license_legal_review': 'OPEN'}},
        'active': {'state': 'PREPARED', 'compiled_source': SOURCE, 'compiled_tree': TREE,
                   'signed_apk': {'sha256': SIGNED, 'unsigned_sha256': UNSIGNED,
                                  'certificate_sha256': CERT},
                   'signing': {'run_id': 42226227569, 'artifact_id': 13504789246},
                   'sources': {'run_id': 42132693740, 'artifact_id': 13463158916},
                   'source_materials': {'sha256': SOURCES},
                   'evidence_receipts': {
                       'signed': '1.0.0-7649f0e-signed-build-info.json',
                       'unsigned': '1.0.0-7649f0e-unsigned-build-info.json',
                       'issuer_risk': '1.0.0-7649f0e-assurance.json',
                       'source_review': '1.0.0-7649f0e-static-source-review.json'},
                   'stable_acceptance_reviewed': False, 'source_distribution_reviewed': False,
                   'branding_revision_pending': False,
                   'coverage_limits': {'functional_acceptance': 'INCOMPLETE'}},
    }


def self_test():
    original = fixture()
    require(validate(original).startswith('PASS_'), 'positive fixture rejected')
    mutations = [
        ('unsigned digest', lambda r: r['unsigned']['unsigned_apk'].update(sha256='0' * 64)),
        ('baseline falsification', lambda r: r['unsigned']['baseline'].update(original_run_conclusion='success')),
        ('source request conflation', lambda r: r['unsigned']['source_input_discrepancy'].update(
            actual_signed_input_unsigned_sha256=HISTORICAL)),
        ('source reviewer forgery', lambda r: r['review']['distribution_disposition'].update(
            complete_corresponding_source_and_license_legal_review='APPROVED')),
        ('unauthorized source approval', lambda r: r['risk'].update(full_source_distribution_approved=True)),
        ('staging armed', lambda r: r['active'].update(state='READY')),
        ('wrong active signed APK', lambda r: r['active']['signed_apk'].update(sha256='f' * 64)),
        ('stale receipt binding', lambda r: r['active']['evidence_receipts'].update(
            unsigned='1.0.0-3437722-unsigned-build-info.json')),
    ]
    for label, change in mutations:
        altered = copy.deepcopy(original)
        change(altered)
        try:
            validate(altered)
        except ValueError:
            continue
        raise AssertionError('Negative mutation unexpectedly accepted: ' + label)

    # The release-ready path requires the separate later decision and exact
    # source documentation; a bare flip of PREPARED may never suffice.
    ready = copy.deepcopy(original)
    ready['approval'] = json.loads((Path(__file__).resolve().parents[1] /
                                    FILES['approval']).read_text())
    ready['active'].update(state='READY', stable_acceptance_reviewed=True,
                           source_distribution_reviewed=True)
    ready['active']['coverage_limits'] = {
        'functional_acceptance': 'ACCEPTED',
        'actual_16k_runtime': 'PASS_SIGNED_EMULATOR_16K_LAUNCH_ONLY',
        'full_100_item_mixed_failure_exercise': 'NOT_RUN',
        'wider_independent_privacy_color_review': 'NOT_RUN',
        'signed_physical_install': 'NOT_RUN',
        'obtainium_install_update': 'NOT_RUN',
        'complete_corresponding_source_license_distribution':
            'REVIEW_APPROVED_WITH_CONDITIONS__PUBLIC_SOURCE_ACCESS_REQUIRED'}
    require(validate(ready).startswith('PASS_EXACT_ICON1_READY_'), 'READY fixture rejected')
    ready_mutations = [
        ('READY with missing source review', lambda r: r.pop('approval')),
        ('READY with falsified signed test', lambda r: r['approval']['signed_runtime'].update(
            runtime_page_size_bytes=4096)),
        ('READY masking 100-item stress', lambda r: r['active']['coverage_limits'].update(
            full_100_item_mixed_failure_exercise='PASS')),
        ('READY changed source offer', lambda r: r['approval']['source_distribution'].update(
            source_offer_requirement='EXPIRES_AFTER_7_DAYS')),
    ]
    for label, change in ready_mutations:
        changed = copy.deepcopy(ready)
        change(changed)
        try:
            validate(changed)
        except (ValueError, KeyError):
            continue
        raise AssertionError('Negative READY mutation accepted: ' + label)
    return len(mutations) + len(ready_mutations)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--self-test', action='store_true')
    args = parser.parse_args()
    if args.self_test:
        print('PASS: positive fixture and %d fail-closed mutations' % self_test())
        return
    root = args.root.resolve()
    records = {key: json.loads((root / path).read_text(encoding='utf-8'))
               for key, path in FILES.items()}
    print(validate(records))
    print('Scope: original signed source + bounded later approval only; physical review is NOT_RUN; public source offer must be checked after publication.')


if __name__ == '__main__':
    main()
