#!/usr/bin/env python3
"""Stage a deterministic exact-project/full-WebP source kit, not release approval.

Reads existing local public Git objects only. No fetch, build, signing or upload.
"""
import argparse
import gzip
import hashlib
import importlib.util
import io
import json
from pathlib import Path, PurePosixPath
import subprocess
import sys
import tarfile
import zlib

spec=importlib.util.spec_from_file_location('snapshot',Path(__file__).with_name('restore-source-snapshot.py'))
snapshot=importlib.util.module_from_spec(spec)
spec.loader.exec_module(snapshot)
WEBP_COMMIT='4fa21912338357f89e4fd51cf2368325b59e9bd9'
WEBP_TREE='3a7830b9713d9187cf1211862d4571ff58b0d507'
MAX_SOURCE_BYTES=256*1024*1024


def git(repo,*args):
    return subprocess.check_output(['git','-C',str(repo),*args])


def sources(repo,commit,expected_tree):
    for value in (commit,expected_tree):
        if len(value)!=40 or any(x not in '0123456789abcdef' for x in value):
            raise ValueError('Sources need explicit immutable commit/tree pins')
    raw=git(repo,'cat-file','commit',commit)
    if snapshot.digest(raw,'commit')!=commit or not raw.startswith(('tree '+expected_tree+'\n').encode()):
        raise ValueError('Source commit/tree mismatch')
    entries={}
    total=0
    for line in git(repo,'ls-tree','-r','-z',commit).split(b'\0'):
        if not line:continue
        header,name=line.split(b'\t',1)
        mode,kind,oid=header.decode().split()
        name=name.decode('utf-8')
        path=PurePosixPath(name)
        if (kind!='blob' or mode not in ('100644','100755') or path.is_absolute() or
                '..' in path.parts or '.git' in path.parts or name in entries):
            raise ValueError('Unsafe or unsupported source tree entry')
        data=git(repo,'cat-file','blob',oid)
        total+=len(data)
        if total>MAX_SOURCE_BYTES or snapshot.digest(data)!=oid:
            raise ValueError('Source bounds or Git blob identity conflict')
        entries[name]={'data':data,'mode':mode,'blob_sha1':oid,'bytes':len(data),
                       'sha256':hashlib.sha256(data).hexdigest()}
    return raw,entries


def encode(value):
    return (json.dumps(value,indent=2)+'\n').encode()


def package(repo,source_commit,source_tree,webp_repo):
    entries={}
    repositories={}
    for label,directory,upstream,commit,tree in (
        ('project','project',repo,source_commit,source_tree),
        ('webp','upstream/libwebp',webp_repo,WEBP_COMMIT,WEBP_TREE)):
        raw,files=sources(upstream,commit,tree)
        if label=='webp' and (len(files)!=343 or sum(x['bytes'] for x in files.values())!=8073794):
            raise ValueError('Full WebP source differs from the reviewed native manifest')
        repositories[label]={'directory':directory,'commit':commit,'tree':tree,'files':[]}
        for name,item in sorted(files.items()):
            entries[directory+'/'+name]=(item['data'],int(item['mode'],8)&0o777)
            repositories[label]['files'].append({'path':name,**{k:v for k,v in item.items() if k not in ('data',)}})
        entries['source-control/'+label+'.commit']=(raw,0o644)
    manifest={'schema':1,'scope':'EXACT_PROJECT_AND_FULL_WEBP_SOURCE_KIT_NOT_DISTRIBUTION_CLEARANCE',
              'repositories':repositories,'included':{'project_build_scripts_wrapper_and_locks':True,
              'full_pinned_webp_source_and_notices':True,'ndk_cpufeatures_source_and_notices':'IN_PROJECT_EVIDENCE',
              'androidx_native_source_excerpts':'IN_PROJECT_EVIDENCE'},
              'remaining_distribution_review':['Complete corresponding-source obligations and other runtime-library source material',
              'Publisher authenticity and owning-tool security','Physical acceptance and production signing'],
              'all_build_dependencies_included':False,'complete_offline_build_claim':False,
              'release_publication_authorized':False}
    entries['PINNED-SOURCES.json']=(encode(manifest),0o644)
    entries['restore-source-snapshot.py']=(Path(snapshot.__file__).read_bytes(),0o755)
    entries['prepare-source-kit.py']=(Path(__file__).read_bytes(),0o755)
    entries['README-SOURCES.md']=('''# FixMyShot source kit

This kit stages the exact accepted project source and the complete pinned WebP
source, including original notices. PINNED-SOURCES.json identifies every byte,
mode, Git blob, tree and original public commit. It is not a complete dependency
mirror or a verdict that all corresponding-source obligations are satisfied.
AndroidX native excerpts and NDK cpufeatures sources/notices are in project/docs/
feasibility/evidence/native-source-review. Full other-library source/distribution
review remains required. No APK, production signing key, cache or publication is
created by the preparation tool; tracked public review evidence remains included.

Extract into a NEW directory, preserving executable modes. Run the supplied
restore-source-snapshot.py with Python3/Git to verify every source file before
restoring shallow Git identities. It refuses extra/missing/changed files and an
existing .git. The original project commit is needed because AGP embeds it in
the APK. This creates only local .git data in these two extracted directories;
it does not contact a server or install software. Then follow project/CONTRIBUTING.md
with the source-pinned SDK/JDK/Gradle/NDK/CMake. Dependencies still require their
reviewed repositories or existing verified caches. Do not claim an offline build.

The full local WebP tree can be supplied through CMake's documented
FETCHCONTENT_SOURCE_DIR_WEBP override in a deliberately configured developer
build. The accepted project CMake configuration still pins the official Git
commit and fetches it normally. This source kit changes no compiled input or
accepted build procedure and does not prove a build on every host.

The exact preparation tool is included beside the restoration helper. After
restoration, it can regenerate this archive from project and upstream/libwebp
without the original producer workspace or a network fetch.

Archive metadata is normalized (uid/gid/mtime zero, fixed modes, sorted paths)
and gzip has no filename/current timestamp. Recreating the kit from the same
explicit Git objects, preparation-tool and Python/zlib versions must produce
identical bytes. Check the recorded runtime when comparing independently.
'''.encode(),0o644)
    entries['MANIFEST.sha256']=(''.join(hashlib.sha256(data).hexdigest()+'  '+name+'\n'
                                      for name,(data,mode) in sorted(entries.items())).encode(),0o644)
    output=io.BytesIO()
    with gzip.GzipFile(fileobj=output,mode='wb',filename='',mtime=0,compresslevel=9) as compressed:
        with tarfile.open(fileobj=compressed,mode='w',format=tarfile.USTAR_FORMAT) as archive:
            for name,(data,mode) in sorted(entries.items()):
                info=tarfile.TarInfo(name)
                info.size=len(data)
                info.mode=mode
                archive.addfile(info,io.BytesIO(data))
    data=output.getvalue()
    return data,{'schema':1,'scope':manifest['scope'],'project_commit':source_commit,'project_tree':source_tree,
                 'project_files':len(repositories['project']['files']),'webp_commit':WEBP_COMMIT,
                 'webp_tree':WEBP_TREE,'webp_files':343,'webp_source_bytes':8073794,
                 'archive_members':len(entries),'archive_bytes':len(data),'archive_sha256':hashlib.sha256(data).hexdigest(),
                 'producer_sha256':hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                 'restore_helper_sha256':hashlib.sha256(Path(snapshot.__file__).read_bytes()).hexdigest(),
                 'python_version':'.'.join(map(str,sys.version_info[:3])),
                 'zlib_runtime_version':zlib.ZLIB_RUNTIME_VERSION,
                 'complete_source_distribution_approval':False,'signing_or_publication':False}


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('repository',type=Path)
    parser.add_argument('webp_repository',type=Path)
    parser.add_argument('output',type=Path)
    parser.add_argument('--source',required=True)
    parser.add_argument('--tree',required=True)
    parser.add_argument('--receipt',required=True,type=Path)
    args=parser.parse_args()
    if args.output.exists() or args.receipt.exists():
        raise ValueError('Output and receipt must both be fresh paths')
    data,report=package(args.repository,args.source,args.tree,args.webp_repository)
    with args.output.open('xb') as stream:stream.write(data)
    with args.receipt.open('x') as stream:json.dump(report,stream,indent=2);stream.write('\n')
    print(json.dumps(report))
