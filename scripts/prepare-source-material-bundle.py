#!/usr/bin/env python3
"""Assemble source-bound runtime materials without signing or distribution approval."""
import argparse
import gzip
import hashlib
import io
import json
import re
from pathlib import Path, PurePosixPath
import sys
import tarfile
import zipfile
import zlib


CAPTURE_SHA = 'f88e271b106e6f92484d7be1e5ac605ea66d6ff1e70bf0882e2501101fb5da31'
PLAN_SHA = '3a0e1aae590a2296f8315d097b083758663c5f8fbb22e5f50c39bf60047fb08e'
PROTO_SHA = '2460a92938259e4441826e57638193d0bde9cb5cdd0dafd5367aacb833f9a971'


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def pinned(path, expected):
    if path.is_symlink() or not path.is_file() or path.stat().st_size > 64*1024*1024:
        raise ValueError('Invalid or oversized material input')
    raw = path.read_bytes()
    if sha(raw) != expected:
        raise ValueError('Source material input changed: '+str(path))
    return raw


def kit_binding(kit, receipt_path, source_commit, source_tree):
    """Verify source archive against an exact current-source producer receipt."""
    for name, value in (("source_commit", source_commit), ("source_tree", source_tree)):
        if not isinstance(value, str) or re.fullmatch(r"[0-9a-f]{40}", value) is None:
            raise ValueError("Invalid " + name)
    if receipt_path.is_symlink() or not receipt_path.is_file() or receipt_path.stat().st_size > 16384:
        raise ValueError("Missing or oversized source-kit receipt")
    def unique_fields(pairs):
        result = {}
        for name, item in pairs:
            if name in result:
                raise ValueError("Repeated source-kit receipt field")
            result[name] = item
        return result
    original = receipt_path.read_bytes()
    receipt = json.loads(original, object_pairs_hook=unique_fields)
    if not isinstance(receipt, dict) or type(receipt.get("schema")) is not int or receipt["schema"] != 1:
        raise ValueError("Unknown source-kit receipt schema")
    if (receipt.get("scope") != "EXACT_PROJECT_AND_FULL_WEBP_SOURCE_KIT_NOT_DISTRIBUTION_CLEARANCE"
            or receipt.get("project_commit") != source_commit or receipt.get("project_tree") != source_tree):
        raise ValueError("Source-kit receipt does not belong to the compiled candidate")
    digest, size = receipt.get("archive_sha256"), receipt.get("archive_bytes")
    if (not isinstance(digest, str) or re.fullmatch(r"[0-9a-f]{64}", digest) is None
            or type(size) is not int or not 0 < size <= 64 * 1024 * 1024):
        raise ValueError("Invalid source-kit archive pin")
    raw = pinned(kit, digest)
    if len(raw) != size:
        raise ValueError("Source-kit archive size differs")
    return raw, original, digest


def package(kit, source_root, binaries, evidence, supplemental, protobuf,
            source_kit_receipt, source_commit, source_tree):
    entries, owners = {}, []
    source_kit_bytes, kit_receipt_bytes, kit_sha = kit_binding(
        kit, source_kit_receipt, source_commit, source_tree)

    def add(name, raw):
        p = PurePosixPath(name)
        if name in entries or p.is_absolute() or '..' in p.parts or '\\' in name or '\n' in name:
            raise ValueError('Unsafe or repeated source material path')
        entries[name] = raw

    add('project-and-webp-source.tar.gz', source_kit_bytes)
    add('records/compiled-project-source-kit.json', kit_receipt_bytes)
    capture_raw = pinned(evidence/'capture.json', CAPTURE_SHA)
    plan_raw = pinned(supplemental/'binary-plan.json', PLAN_SHA)
    capture, plan = json.loads(capture_raw), json.loads(plan_raw)
    add('records/runtime-source-capture.json', capture_raw)
    add('records/frozen-binary-plan.json', plan_raw)
    for row in capture['components']:
        if 'archive_sha256' in row:
            path = PurePosixPath(row['relative_archive'])
            if path.is_absolute() or '..' in path.parts:
                raise ValueError('Unsafe companion input path')
            add('runtime-sources/'+str(path), pinned(source_root/str(path), row['archive_sha256']))
    add('runtime-sources/protobuf-javalite-4.28.2-sources.jar', pinned(protobuf, PROTO_SHA))
    # Original AAR resource/build inputs only. No APK, .class, .so or cache files.
    for i, row in enumerate(plan):
        raw = pinned(binaries/row['sha256'], row['sha256'])
        if len(raw) != row['bytes']:
            raise ValueError('Frozen binary length differs')
        with zipfile.ZipFile(io.BytesIO(raw)) as archive:
            names = archive.namelist()
            if len(names) != len(set(names)):
                raise ValueError('Repeated AAR input path')
            included = []
            for item in archive.infolist():
                name = item.filename
                if not item.is_dir() and (name.startswith(('res/', 'assets/')) or name in ('AndroidManifest.xml', 'R.txt', 'public.txt', 'proguard.txt')):
                    if item.file_size > 16*1024*1024 or (item.external_attr >> 16) & 0o170000 == 0o120000:
                        raise ValueError('Oversized or linked AAR material')
                    output = f'aar-inputs/{i:03d}/'+name
                    add(output, archive.read(item))
                    included.append(output)
            owners.append({'coordinate': row['coordinate'], 'frozen_archive_sha256': row['sha256'], 'material_paths': included})
    for prefix, root in (('records/source-stage', evidence), ('records/closure', supplemental)):
        for path in sorted(root.rglob('*')):
            if path.is_symlink():
                raise ValueError('Linked evidence path')
            if path.is_file():
                if path.stat().st_size > 16*1024*1024:
                    raise ValueError('Oversized evidence file')
                add(prefix+'/'+path.relative_to(root).as_posix(), path.read_bytes())
    add('prepare-source-material-bundle.py', Path(__file__).read_bytes())
    record = {'schema': 1, 'scope': 'BOUND_CANDIDATE_SOURCE_MATERIALS_NOT_COMPLETE_DISTRIBUTION_APPROVAL',
              'compiled_source': source_commit, 'compiled_tree': source_tree,
              'project_webp_kit_sha256': kit_sha, 'source_kit_receipt_sha256': sha(kit_receipt_bytes),
              'source_capture_sha256': CAPTURE_SHA,
              'upstream_protobuf_sha256': PROTO_SHA, 'aar_material_owners': owners,
              'publisher_authentication_approved': False, 'binary_source_equivalence_approved': False,
              'complete_offline_build': False, 'final_corresponding_source_approved': False,
              'signing_or_publication': False}
    add('SOURCE-MATERIALS.json', (json.dumps(record, indent=2)+'\n').encode())
    add('README-SOURCES.md', b'''# FixMyShot candidate source materials

SOURCE-MATERIALS.json identifies the compiled source commit/tree and current
project/WebP kit digest. The nested archive retains that exact project
and full pinned WebP source plus its original Git restoration helpers. Extract
it separately and follow its README. runtime-sources/ contains original source
companions and mapped upstream Protobuf. aar-inputs/ preserves published AAR
resources, manifests and rules, with exact original archive owners recorded in
SOURCE-MATERIALS.json. These are published build inputs, not proof every asset
is its original preferred editing form. records/ retains prior reviewed runtime source-capture inputs, original notices,
supplemental JNI/FlatBuffers/AIDL sources and precise review limits. An unchanged
dependency-source capture is not new issuer authentication or proof of final
corresponding-source acceptance.

After extraction run sha256sum -c MANIFEST.sha256 from this directory. This
verifies bytes, not publisher identity or source/binary equivalence. Read the
closure and source-stage records before reuse. This is not a complete offline
toolchain, final corresponding-source approval, an APK or a release. Existing
publisher, source/distribution, physical and production-signing gates remain.
No download, build, signing, upload or operator software installation occurs.
''')
    add('MANIFEST.sha256', ''.join(sha(raw)+'  '+name+'\n' for name, raw in sorted(entries.items())).encode())
    if sum(map(len, entries.values())) > 128*1024*1024:
        raise ValueError('Aggregate source materials limit')
    stream = io.BytesIO()
    with gzip.GzipFile(fileobj=stream, mode='wb', filename='', mtime=0, compresslevel=9) as compressed:
        with tarfile.open(fileobj=compressed, mode='w', format=tarfile.PAX_FORMAT) as archive:
            for name, raw in sorted(entries.items()):
                info = tarfile.TarInfo(name)
                info.size, info.mode = len(raw), 0o644
                archive.addfile(info, io.BytesIO(raw))
    raw = stream.getvalue()
    return raw, {'schema': 1, 'scope': record['scope'], 'archive_sha256': sha(raw), 'archive_bytes': len(raw),
                 'members': len(entries), 'producer_sha256': sha(Path(__file__).read_bytes()),
                 'python': '.'.join(map(str, sys.version_info[:3])), 'zlib': zlib.ZLIB_RUNTIME_VERSION,
                 'final_distribution_source_approved': False}


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    for name in ('kit', 'source_root', 'binaries', 'evidence', 'supplemental', 'protobuf', 'output', 'receipt'):
        p.add_argument(name, type=Path)
    p.add_argument('--source-kit-receipt', required=True, type=Path)
    p.add_argument('--source-commit', required=True)
    p.add_argument('--source-tree', required=True)
    a = p.parse_args()
    if a.output.exists() or a.receipt.exists():
        raise ValueError('Outputs must be fresh')
    raw, receipt = package(a.kit, a.source_root, a.binaries, a.evidence, a.supplemental,
                           a.protobuf, a.source_kit_receipt, a.source_commit, a.source_tree)
    with a.output.open('xb') as f:
        f.write(raw)
    with a.receipt.open('x') as f:
        json.dump(receipt, f, indent=2)
        f.write('\n')
    print(json.dumps(receipt))
