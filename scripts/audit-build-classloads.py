#!/usr/bin/env python3
"""Summarize bounded JVM class-load traces; observation is not exploitability clearance."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
from urllib.parse import unquote,urlparse

FAMILIES={'netty':('io.netty.','io.grpc.netty.shaded.io.netty.'),'commons_lang3':'org.apache.commons.lang3.',
          'apache_httpclient':'org.apache.http.','jose4j':'org.jose4j.',
          'bouncycastle':'org.bouncycastle.','jdom2':'org.jdom2.'}
ROLES={'sdkmanager':'com.android.sdklib.tool.sdkmanager.',
       'gradle':'org.gradle.launcher.daemon.', 'android_plugin':'com.android.build.gradle.',
       'lint':'com.android.tools.lint.', 'tests':'org.junit.'}
LINE=re.compile(r'^\[([0-9.]+)s\]\[info\s*\]\[class,load\s*\]\s+(\S+) source: (.+)$')
METADATA=re.compile(r'^\[[0-9.]+s\]\[info\s*\]\[class,load\s*\]\s+(?:opened|path): .+$')
MAX_FILE=33*1024*1024
MAX_TOTAL=256*1024*1024


def audit(directory,source_commit,control_commit,source_roots,*,preflight=False):
    if any(not re.fullmatch('[0-9a-f]{40}',x) for x in (source_commit,control_commit)):
        raise ValueError('Explicit source/control pins required')
    paths=sorted(directory.iterdir())
    if (not paths or len(paths)>64 or any(p.is_symlink() or not p.is_file() or
            re.fullmatch(r'classes-[0-9]+\.log',p.name) is None for p in paths)):
        raise ValueError('Missing, rotated or unexpected trace files')
    if sum(p.stat().st_size for p in paths)>MAX_TOTAL:
        raise ValueError('Trace collection exceeds its bound')
    roles={key:[] for key in ROLES}
    families={key:{} for key in FAMILIES}
    processes=[]
    for path in paths:
        if path.stat().st_size>MAX_FILE:
            raise ValueError('Oversized JVM trace')
        raw=path.read_bytes()
        if not raw.endswith(b'\n'):
            raise ValueError('Empty or partial JVM trace')
        events=0
        metadata_events=0
        java_object=False
        for line in raw.decode('utf-8').splitlines():
            match=LINE.fullmatch(line)
            if not match:
                # HotSpot classLoader.cpp emits these same-tag classpath records.
                if METADATA.fullmatch(line):
                    metadata_events+=1
                    continue
                raise ValueError('Unrecognized JVM record in '+path.name+': '+line[:400])
            name,origin=match[2],match[3]
            java_object|=name=='java.lang.Object'
            events+=1
            for role,prefix in ROLES.items():
                if name.startswith(prefix) and path.name not in roles[role]:roles[role].append(path.name)
            for family,prefix in FAMILIES.items():
                if name.startswith(prefix):
                    families[family].setdefault((name,origin),set()).add(path.name)
        if not java_object or not events:
            raise ValueError('Missing JVM startup class coverage')
        processes.append({'file':path.name,'bytes':len(raw),'sha256':hashlib.sha256(raw).hexdigest(),
                          'class_load_events':events,'classpath_metadata_events':metadata_events,
                          'java_lang_object_observed':True})
    if not preflight and any(not value for value in roles.values()):
        raise ValueError('Missing required SDK/Gradle/AGP/lint/test coverage')
    origins={}
    for entries in families.values():
        for name,origin in entries:
            if origin in origins:continue
            item={'origin':origin,'local_jar_sha256':None,'version_or_frozen_identity_automatically_inferred':False}
            parsed=urlparse(origin)
            if parsed.scheme=='file' and parsed.netloc in ('','localhost'):
                path=Path(unquote(parsed.path)).resolve()
                if (path.suffix=='.jar' and any(path.is_relative_to(root.resolve()) for root in source_roots)
                        and path.is_file() and path.stat().st_size<=128*1024*1024):
                    digest=hashlib.sha256()
                    with path.open('rb') as stream:
                        for chunk in iter(lambda:stream.read(65536),b''):digest.update(chunk)
                    item.update(local_jar_sha256=digest.hexdigest(),local_jar_bytes=path.stat().st_size)
            origins[origin]=item
    return {'schema':1,'scope':'JVM_STARTUP_TRACE_FORMAT_ONLY_NOT_BUILD_REVIEW' if preflight else
            'OBSERVED_JVM_CLASS_LOADING_FOR_EXPLICIT_HOSTED_BUILD_TASK_SET',
            'source_commit':source_commit,'control_commit':control_commit,
            'trace_files':processes,'coverage_markers':roles,
            'family_prefixes':FAMILIES,
            'required_build_coverage_verified':not preflight,
            'families':{family:{'distinct_class_origin_pairs':len(items),
                'classes':[{'class':name,'origin':origin,'processes':sorted(pids)}
                           for (name,origin),pids in sorted(items.items())]}
                        for family,items in families.items()},
            'origins':list(origins.values()),'rotated_or_partial_traces_accepted':False,
            'all_possible_processes_or_methods_proved_covered':False,
            'absence_means':'NOT_OBSERVED_IN_CAPTURED_LOGGED_JVMS_ONLY',
            'class_presence_means_method_execution':False,'security_gate_automatically_closed':False,
            'reviewer_sha256':hashlib.sha256(Path(__file__).read_bytes()).hexdigest()}


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('traces',type=Path)
    parser.add_argument('output',type=Path)
    parser.add_argument('--source',required=True)
    parser.add_argument('--control',required=True)
    parser.add_argument('--source-root',action='append',required=True,type=Path)
    parser.add_argument('--preflight',action='store_true',help='Validate startup format only; no build-coverage verdict')
    args=parser.parse_args()
    if args.output.exists():raise ValueError('Output directory must be fresh')
    args.output.mkdir(parents=True)
    paths=sorted(args.traces.iterdir())
    if len(paths)>64 or sum(p.stat().st_size for p in paths)>MAX_TOTAL:
        raise ValueError('Trace snapshot exceeds its bounds')
    for path in paths:
        if path.is_symlink() or not path.is_file() or not re.fullmatch(r'classes-[0-9]+\.log(?:\.[0-9]+)?',path.name) or path.stat().st_size>MAX_FILE:
            raise ValueError('Unsafe trace snapshot member')
        (args.output/path.name).write_bytes(path.read_bytes())
    # Freeze first, so a parser/coverage refusal still preserves actual raw input.
    try:
        result=audit(args.output,args.source,args.control,args.source_root,preflight=args.preflight)
    except ValueError as error:
        (args.output/'failure.json').write_text(json.dumps({'scope':'REFUSED_CAPTURE','reason':str(error)})+'\n')
        raise
    (args.output/'report.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps({'processes':len(result['trace_files']),
                      'families':{k:v['distinct_class_origin_pairs'] for k,v in result['families'].items()},
                      'security_gate_automatically_closed':False}))
