#!/usr/bin/env python3
"""Read-only, fail-closed inventory of source-bound Gradle trust candidates.

The generated hashes are NOT proof of publisher authenticity. No lockfile/verification
metadata is accepted or modified by this script.
"""
from __future__ import annotations

import argparse
import hashlib
import io
import json
from pathlib import Path
import re
import tarfile
import xml.etree.ElementTree as ET
from zipfile import ZipFile

GRADLE_JAR_SHA = '81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f'
GRADLE_DIST_SHA = '20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78'
NS = {'g': 'https://schema.gradle.org/dependency-verification'}
CRITICAL = (
    ('com.android.tools.build', 'gradle', '8.13.2', '.jar', 'GOOGLE_MAVEN'),
    ('org.jetbrains.kotlin', 'kotlin-gradle-plugin', '2.2.21', '.jar', 'MAVEN_CENTRAL'),
    ('androidx.heifwriter', 'heifwriter', '1.2.0-rc01', '.aar', 'GOOGLE_MAVEN'),
    ('androidx.activity', 'activity-compose', '1.11.0', '.aar', 'GOOGLE_MAVEN'),
    ('androidx.compose', 'compose-bom', '2025.12.00', '.pom', 'GOOGLE_MAVEN'),
    ('junit', 'junit', '4.13.2', '.jar', 'MAVEN_CENTRAL'),
)
LOCK_PATHS = ('app/gradle.lockfile', 'core/gradle.lockfile', 'codec-native/gradle.lockfile')
HASH_RE = re.compile(r'[0-9a-f]{64}\Z')
GIT_SHA_RE = re.compile(r'[0-9a-f]{40}\Z')
MAX_SOURCE_BYTES = 64 * 1024 * 1024
MAX_SOURCE_FILE_BYTES = 16 * 1024 * 1024
MAX_ARCHIVE_ENTRIES = 4096


def inspect_source_tar(payload: bytes) -> dict:
    """Reconstruct tracked Git files/modes without extracting or executing archive content."""
    if len(payload) > MAX_SOURCE_BYTES:
        raise ValueError('Oversized source archive')
    root = {}
    seen = set()
    hashes = {}
    with tarfile.open(fileobj=io.BytesIO(payload), mode='r:') as source:
        for member in source:
            name = member.name.removeprefix('./')
            if member.isdir():
                name = name.removesuffix('/')
            parts = name.split('/')
            if (not name or name.startswith('/') or len(parts) > 64 or
                    len(name.encode('utf-8')) > 4096 or any(p in ('', '.', '..') for p in parts)):
                raise ValueError('Invalid source path')
            if name in seen or len(seen) >= MAX_ARCHIVE_ENTRIES:
                raise ValueError('Repeated or excessive source entries')
            seen.add(name)
            if member.isdir():
                continue
            if not member.isfile() or not 0 <= member.size <= MAX_SOURCE_FILE_BYTES:
                raise ValueError('Unsupported or oversized source member')
            stream = source.extractfile(member)
            if stream is None:
                raise ValueError('Unreadable source member')
            with stream:
                data = stream.read(member.size + 1)
            if len(data) != member.size:
                raise ValueError('Incomplete source member')
            node = root
            for part in parts[:-1]:
                node = node.setdefault(part, {})
                if not isinstance(node, dict):
                    raise ValueError('Source file/directory conflict')
            if parts[-1] in node:
                raise ValueError('Source file/directory conflict')
            mode = b'100755' if member.mode & 0o111 else b'100644'
            blob = hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).digest()
            node[parts[-1]] = (mode, blob)
            hashes[name] = digest(data)
    required = {'settings.gradle.kts', 'gradle/libs.versions.toml', 'scripts/bootstrap-gradle.sh'}
    if not required.issubset(hashes):
        raise ValueError('Source manifest incomplete')

    def git_tree(node: dict) -> bytes:
        content = bytearray()
        ordered = sorted(node.items(), key=lambda pair:
                         (pair[0] + ('/' if isinstance(pair[1], dict) else '')).encode('utf-8'))
        for name, value in ordered:
            mode, sha = (b'40000', git_tree(value)) if isinstance(value, dict) else value
            content.extend(mode + b' ' + name.encode('utf-8') + b'\0' + sha)
        return hashlib.sha1(b'tree ' + str(len(content)).encode() + b'\0' + content).digest()

    return {'tree_sha1': git_tree(root).hex(), 'file_count': len(hashes), 'file_sha256': hashes}


def digest(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest()


def audit_metadata(xml: bytes, critical=CRITICAL) -> dict:
    if len(xml) > 3_000_000:
        raise ValueError('Oversized candidate verification metadata')
    root = ET.fromstring(xml)
    config = root.find('g:configuration', NS)
    if config is None:
        raise ValueError('Missing Gradle verification configuration')
    components = root.findall('./g:components/g:component', NS)
    if not components:
        raise ValueError('No captured Gradle components')
    seen = set()
    all_artifacts = []
    for comp in components:
        identity = tuple(comp.get(k) for k in ('group', 'name', 'version'))
        if not all(identity) or identity in seen:
            raise ValueError('Duplicate or malformed Gradle component')
        seen.add(identity)
        for artifact in comp.findall('g:artifact', NS):
            name = artifact.get('name', '')
            checksums = artifact.findall('g:sha256', NS)
            signatures = artifact.findall('g:pgp', NS)
            if not name or len(checksums) != 1 or not HASH_RE.fullmatch(checksums[0].get('value', '')):
                raise ValueError('Ambiguous or non-SHA256 dependency artifact')
            key = (*identity, name)
            if any(a['key'] == key for a in all_artifacts):
                raise ValueError('Duplicate artifact')
            all_artifacts.append({'key': key, 'sha256': checksums[0].get('value'),
                                  'origin': checksums[0].get('origin', ''), 'pgp': len(signatures)})
    checks = []
    for group, name, version, ext, publisher in critical:
        eligible = [a for a in all_artifacts if a['key'][:3] == (group, name, version)
                    and a['key'][3].endswith(ext)]
        if not eligible:
            raise ValueError(f'Missing critical dependency in generated metadata: {group}:{name}:{version}')
        for item in eligible:
            checks.append({'coordinate': f'{group}:{name}:{version}', 'file': item['key'][3],
                           'observed_sha256': item['sha256'], 'expected_publisher': publisher,
                           'publisher_sha_independently_verified': False,
                           'origin_claim': item['origin'], 'pgp_signatures_in_candidate': item['pgp']})
    return {'component_count': len(components), 'artifact_count': len(all_artifacts),
            'pgp_signature_count': sum(a['pgp'] for a in all_artifacts),
            'generated_origin_count': sum(a['origin'] == 'Generated by Gradle' for a in all_artifacts),
            'verify_metadata': config.findtext('g:verify-metadata', default='', namespaces=NS),
            'verify_signatures': config.findtext('g:verify-signatures', default='', namespaces=NS),
            'critical_candidates': checks}


def audit_archive(path: Path, *, expected_source: str | None = None,
                  expected_tree: str | None = None) -> dict:
    if (expected_source is None) != (expected_tree is None):
        raise ValueError('Expected source commit and tree must be supplied together')
    if expected_source is not None and (not GIT_SHA_RE.fullmatch(expected_source) or
                                        not GIT_SHA_RE.fullmatch(expected_tree)):
        raise ValueError('Invalid expected Git identity')
    with ZipFile(path) as archive:
        entries = archive.infolist()
        names = {entry.filename for entry in entries}
        if len(entries) != len(names) or len(entries) > MAX_ARCHIVE_ENTRIES:
            raise ValueError('Repeated or excessive ZIP entries')
        if sum(entry.file_size for entry in entries) > 512 * 1024 * 1024 or any(
                entry.file_size > 128 * 1024 * 1024 or
                (entry.filename == 'evidence/source.tar' and entry.file_size > MAX_SOURCE_BYTES)
                for entry in entries):
            raise ValueError('Oversized candidate ZIP entries')
        if archive.testzip() is not None:
            raise ValueError('ZIP CRC failed')
        required = {'gradle/verification-metadata.xml', 'gradle/wrapper/gradle-wrapper.jar',
                    'gradle/wrapper/gradle-wrapper.properties', 'evidence/source-sha.txt',
                    'evidence/source.tar', *LOCK_PATHS}
        if not required.issubset(names):
            raise ValueError(f'Missing source-bound candidate entries: {sorted(required - names)}')
        files = {n: archive.read(n) for n in required}
    source_sha = files['evidence/source-sha.txt'].decode('ascii').strip()
    if not GIT_SHA_RE.fullmatch(source_sha):
        raise ValueError('Invalid build source SHA')
    properties = files['gradle/wrapper/gradle-wrapper.properties'].decode()
    # Retain the immutable historical8.13 profile while recognizing the reviewed migration.
    if 'gradle-9.4.1-bin.zip' in properties:
        distribution_sha = '2ab2958f2a1e51120c326cad6f385153bb11ee93b3c216c5fccebfdfbb7ec6cb'
        wrapper_sha = '55243ef57851f12b070ad14f7f5bb8302daceeebc5bce5ece5fa6edb23e1145c'
        critical = (('com.android.tools.build', 'gradle', '9.2.1', '.jar', 'GOOGLE_MAVEN'),
                    ('org.jetbrains.kotlin', 'kotlin-gradle-plugin', '2.4.21', '.jar', 'MAVEN_CENTRAL'), *CRITICAL[2:])
    elif 'gradle-8.13-bin.zip' in properties:
        distribution_sha, wrapper_sha, critical = GRADLE_DIST_SHA, GRADLE_JAR_SHA, CRITICAL
    else:
        raise ValueError('Unrecognized Gradle distribution version')
    if f'distributionSha256Sum={distribution_sha}' not in properties:
        raise ValueError('Unrecognized Gradle distribution pin')
    if digest(files['gradle/wrapper/gradle-wrapper.jar']) != wrapper_sha:
        raise ValueError('Wrong official Gradle wrapper JAR')
    source_info = inspect_source_tar(files['evidence/source.tar'])
    identity_verified = expected_source is not None
    if identity_verified and (source_sha != expected_source or source_info['tree_sha1'] != expected_tree):
        raise ValueError('Archived source does not match the expected commit/tree')
    # Independently archived build inputs must be the same bytes as the tracked source.
    untracked_inputs = []
    for name in (*LOCK_PATHS, 'gradle/wrapper/gradle-wrapper.jar',
                 'gradle/wrapper/gradle-wrapper.properties'):
        tracked = source_info['file_sha256'].get(name)
        if tracked is None:
            untracked_inputs.append(name)
        elif tracked != digest(files[name]):
            raise ValueError(f'Build input differs from archived source: {name}')
    if identity_verified and untracked_inputs:
        raise ValueError('Build inputs are not tracked in the pinned source')
    locks = {}
    for key in LOCK_PATHS:
        rows = [line for line in files[key].decode().splitlines()
                if line and not line.startswith('#') and not line.startswith('empty=')]
        if not rows or len(rows) != len(set(rows)):
            raise ValueError(f'Malformed or duplicated lockfile rows in {key}')
        locks[key] = {'entry_count': len(rows), 'sha256': digest(files[key])}
    return {
        'source_commit': source_sha,
        'source_tree_sha1': source_info['tree_sha1'],
        'source_file_count': source_info['file_count'],
        'source_identity_verified': identity_verified,
        'source_build_inputs_verified': not untracked_inputs,
        'source_untracked_build_inputs': untracked_inputs,
        'candidate_archive_sha256': digest(path.read_bytes()),
        'gradle_distribution_sha256': distribution_sha,
        'official_gradle_wrapper_jar_verified': True,
        'observed_candidate_sha256': digest(files['gradle/verification-metadata.xml']),
        'generated_metadata': audit_metadata(files['gradle/verification-metadata.xml'], critical),
        'lockfiles': locks,
        'trust_status': 'GENERATED_ONLY__NOT_APPROVED_FOR_STRICT_FREEZE',
        'next_requirement': 'Verify publisher identity and independent artifact hashes/signatures for critical GAVs, '
                            'then review the remaining initial trust list; do not auto-approve generated hashes.',
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('archive', type=Path)
    parser.add_argument('--baseline', type=Path)
    parser.add_argument('--expected-source', help='Commit SHA inspected from the trusted repository')
    parser.add_argument('--expected-tree', help='Tree SHA independently obtained for that commit')
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    current = audit_archive(args.archive, expected_source=args.expected_source,
                            expected_tree=args.expected_tree)
    if args.baseline:
        before = audit_archive(args.baseline)
        current['baseline_comparison'] = {
            'source_commit': before['source_commit'],
            'source_tree_sha1': before['source_tree_sha1'],
            'source_identity_verified': before['source_identity_verified'],
            'lockfile_sha_match': {name: current['lockfiles'][name]['sha256'] == before['lockfiles'][name]['sha256']
                                   for name in LOCK_PATHS},
            'metadata_sha_match': current['observed_candidate_sha256'] == before['observed_candidate_sha256'],
            'interpretation': 'Matching generated bytes are reproducible observations, not independent authentication.',
        }
    result = json.dumps(current, indent=2) + '\n'
    if args.output:
        args.output.write_text(result)
    print(result)


if __name__ == '__main__':
    main()
