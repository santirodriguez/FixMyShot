#!/usr/bin/env python3
"""Reuse exact historical proofs in a different pinned inventory; never activate trust."""
import argparse
import copy
import importlib.util
import json
from pathlib import Path
import re

spec = importlib.util.spec_from_file_location('batch', Path(__file__).with_name('review-google-batch.py'))
batch = importlib.util.module_from_spec(spec)
spec.loader.exec_module(batch)


def rebase(old_metadata, prior_bytes, metadata, *, prior_pin, metadata_pin, source, prior_source):
    for value in (source, prior_source):
        if not isinstance(value, str) or re.fullmatch('[0-9a-f]{40}', value) is None:
            raise ValueError('Missing inspected source identity')
    if batch.digest(metadata) != metadata_pin:
        raise ValueError('New inventory differs from its inspected pin')
    prior, _, _ = batch.checked_receipt(prior_bytes, old_metadata, prior_pin)
    rows = {batch.planner.identity(x): x for x in batch.core.captured_rows(metadata)}
    reused = []
    for old in prior['reviewed_artifacts']:
        row = rows.get(batch.planner.identity(old))
        if row is None:
            continue
        if row['sha256'] != old['sha256']:
            raise ValueError('Previously reviewed exact identity has conflicting new bytes')
        reused.append(copy.deepcopy(old))
    return {
        'schema': 1, 'status': 'PARTIAL_PUBLISHER_REVIEW__NOT_ACTIVE',
        'baseline_source_commit': source, 'baseline_metadata_sha256': metadata_pin,
        'captured_artifact_count': len(rows), 'reviewed_artifact_count': len(reused),
        'remaining_unreviewed_artifact_count': len(rows)-len(reused),
        'complete_for_baseline': len(rows) == len(reused),
        'active_gradle_policy_changed': False, 'current_vulnerability_review': 'OPEN',
        'reviewed_artifacts': reused,
        'inherited_review': {
            'repository': 'santirodriguez/FixMyShot', 'commit': prior_source,
            'path': 'gradle/dependency-provenance-review.json', 'sha256': prior_pin,
            'baseline_metadata_sha256': batch.digest(old_metadata),
            'prior_reviewed_count': prior['reviewed_artifact_count'],
            'reused_exact_files': len(reused),
            'scope': 'IDENTITY_AND_SHA256_EQUALITY_ONLY; original proof records unchanged; no new revocation or security verdict',
        },
    }


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    for arg in ('old_metadata', 'prior_review', 'metadata', 'output'):
        parser.add_argument(arg, type=Path)
    for arg in ('prior', 'metadata', 'source', 'prior-source'):
        parser.add_argument('--expected-'+arg, required=True)
    args = parser.parse_args()
    result = rebase(batch.core.bounded_bytes(args.old_metadata, 3_000_000),
                    batch.core.bounded_bytes(args.prior_review, 2_000_000),
                    batch.core.bounded_bytes(args.metadata, 3_000_000),
                    prior_pin=args.expected_prior, metadata_pin=args.expected_metadata,
                    source=args.expected_source, prior_source=args.expected_prior_source)
    args.output.mkdir(parents=True, exist_ok=False)
    (args.output/'dependency-provenance-review.json').write_text(json.dumps(result, indent=2)+'\n')
    (args.output/'reviewed-verification-candidate.xml').write_bytes(batch.core.candidate_xml(result['reviewed_artifacts']))
    print(json.dumps({k: result[k] for k in ('reviewed_artifact_count', 'remaining_unreviewed_artifact_count', 'active_gradle_policy_changed')}))
