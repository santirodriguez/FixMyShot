#!/usr/bin/env python3
"""Offline, read-only cross-receipt preflight for the exact FixMyShot icon1 1.0.0.

This cannot authorize source distribution, change the active release request,
or replace the separate stable release staging/approval validation.
"""
import argparse
import copy
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
    require(active['state'] == 'PREPARED' and
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
            active['stable_acceptance_reviewed'] is False and
            active['source_distribution_reviewed'] is False and
            active['branding_revision_pending'] is False,
            'PREPARED icon1 request differs or was prematurely approved')
    return 'PASS_EXACT_ICON1_OFFLINE_PROVENANCE__DISTRIBUTION_GATES_OPEN'


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
                   'branding_revision_pending': False},
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
    return len(mutations)


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
    print('Scope: offline receipts only; signed 16 KiB physical, legal approval and release remain OPEN.')


if __name__ == '__main__':
    main()
