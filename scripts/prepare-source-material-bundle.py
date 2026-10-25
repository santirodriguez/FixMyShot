#!/usr/bin/env python3
"""Assemble the accepted e516 source materials, offline; no release approval."""
import argparse
import gzip
import hashlib
import io
import json
from pathlib import Path, PurePosixPath
import sys
import tarfile
import zipfile
import zlib

KIT_SHA = '0e2725b62e2a0b19d55545d85b384e2ec6d4cda829e4e4a95e2e800601ce2de5'
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


def package(kit, source_root, binaries, evidence, supplemental, protobuf):
    entries, owners = {}, []

    def add(name, raw):
        p = PurePosixPath(name)
        if name in entries or p.is_absolute() or '..' in p.parts or '\\' in name or '\n' in name:
            raise ValueError('Unsafe or repeated source material path')
        entries[name] = raw

    add('project-and-webp-source.tar.gz', pinned(kit, KIT_SHA))
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
    record = {'schema': 1, 'scope': 'ACCEPTED_E516_SOURCE_MATERIALS_NOT_COMPLETE_DISTRIBUTION_APPROVAL',
              'compiled_source': 'e516aa89efae94fe2376fd9f62e143218b49ccac',
              'project_webp_kit_sha256': KIT_SHA, 'source_capture_sha256': CAPTURE_SHA,
              'upstream_protobuf_sha256': PROTO_SHA, 'aar_material_owners': owners,
              'publisher_authentication_approved': False, 'binary_source_equivalence_approved': False,
              'complete_offline_build': False, 'final_corresponding_source_approved': False,
              'signing_or_publication': False}
    add('SOURCE-MATERIALS.json', (json.dumps(record, indent=2)+'\n').encode())
    add('README-SOURCES.md', b'''# FixMyShot e516 source materials

The nested project-and-webp-source.tar.gz retains the exact accepted project
and full pinned WebP source plus its original Git restoration helpers. Extract
it separately and follow its README. runtime-sources/ contains original source
companions and mapped upstream Protobuf. aar-inputs/ preserves published AAR
resources, manifests and rules, with exact original archive owners recorded in
SOURCE-MATERIALS.json. These are published build inputs, not proof every asset
is its original preferred editing form. records/ retains original notices,
supplemental JNI/FlatBuffers/AIDL sources and precise review limits.

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
    a = p.parse_args()
    if a.output.exists() or a.receipt.exists():
        raise ValueError('Outputs must be fresh')
    raw, receipt = package(a.kit, a.source_root, a.binaries, a.evidence, a.supplemental, a.protobuf)
    with a.output.open('xb') as f:
        f.write(raw)
    with a.receipt.open('x') as f:
        json.dump(receipt, f, indent=2)
        f.write('\n')
    print(json.dumps(receipt))
