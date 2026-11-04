#!/usr/bin/env python3
"""Replay exact Google source-copy POM evidence; never approve unavailable or different bytes."""
import argparse
import importlib.util
import json
from pathlib import Path

spec=importlib.util.spec_from_file_location('source',Path(__file__).with_name('review-google-source-copy.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)


def review(metadata,prior_bytes,report_bytes,evidence,prior_pin,report_pin):
    prior,rows,known=m.batch.checked_receipt(prior_bytes,metadata,prior_pin)
    if m.batch.digest(report_bytes)!=report_pin:raise ValueError('Source capture report changed')
    report=json.loads(report_bytes)
    attempted={tuple(x['identity']) for entry in prior.get('publisher_source_copy_batches',[]) if entry.get('commit')==m.COMMIT for x in entry.get('outcomes',[])}
    expected=[identity for identity,row in sorted(rows.items()) if identity not in known and identity not in attempted and
              row['group'].startswith(('androidx.','com.android.')) and row['file']==row['name']+'-'+row['version']+'.pom']
    if (report.get('schema')!=1 or report.get('scope')!='GOOGLE_PUBLISHED_POMS_ONE_IMMUTABLE_AOSP_RELEASE' or
            report.get('metadata_sha256')!=m.batch.digest(metadata) or report.get('prior_review_sha256')!=prior_pin or
            report.get('repository')!=m.REPOSITORY or report.get('commit')!=m.COMMIT or
            report.get('tag_object')!=m.TAG_OBJECT or report.get('automatic_authentication') is not False or
            [tuple(x['identity']) for x in report['results']]!=expected):
        raise ValueError('Source capture scope, pins, order or publisher anchor changed')
    current=prior_bytes;verified=[]
    for index,item in enumerate(report['results']):
        identity=tuple(item['identity']);row=rows[identity];label='pom-'+str(index).zfill(3)
        path='common/m2/repository/'+row['group'].replace('.','/')+'/'+row['name']+'/'+row['version']+'/'+row['file']
        if (item['id']!=label or item['expected_sha256']!=row['sha256'] or
                item['url']!=m.REPOSITORY+'/+/'+m.COMMIT+'/'+path):
            raise ValueError('Source capture exact identity or route changed')
        if item['status']!='MATCHED_FROZEN_BYTES':continue
        registry=m.batch.core.bounded_bytes(evidence/(label+'.registry.json'),65536)
        directory=m.batch.core.bounded_bytes(evidence/(label+'.directory.json'),65536)
        data=m.batch.core.bounded_bytes(evidence/(label+'.pom'),262144)
        record,_=m.review(metadata,current,registry,directory,data,m.batch.digest(current),item['registry_sha256'],allowed_identity=identity)
        current=(json.dumps(record,indent=2)+'\n').encode();verified.append(list(identity))
    record=json.loads(current)
    record['publisher_source_copy_batches']=record.get('publisher_source_copy_batches',[])+[{
        'prior_review_sha256':prior_pin,'capture_report_sha256':report_pin,
        'collector_sha256':report['collector_sha256'],'reviewer_sha256':m.batch.digest(Path(__file__).read_bytes()),
        'repository':m.REPOSITORY,'commit':m.COMMIT,'captured_rows':len(expected),
        'outcomes':[{'identity':x['identity'],'status':x['status']} for x in report['results']],
        'verified_files':verified,'verified_new_files':len(verified),
        'pgp_signature_claim':False,'namespace_wide_trust':False,'active_gradle_policy_changed':False}]
    return record,m.batch.core.candidate_xml(record['reviewed_artifacts'])


if __name__=='__main__':
    p=argparse.ArgumentParser()
    for name in ('metadata','prior','evidence','output'):p.add_argument(name,type=Path)
    p.add_argument('--expected-prior',required=True);p.add_argument('--expected-report',required=True);a=p.parse_args()
    record,xml=review(m.batch.core.bounded_bytes(a.metadata,3_000_000),m.batch.core.bounded_bytes(a.prior,2_000_000),
                      m.batch.core.bounded_bytes(a.evidence/'report.json',512000),a.evidence,a.expected_prior,a.expected_report)
    a.output.mkdir(parents=True,exist_ok=False)
    (a.output/'dependency-provenance-review.json').write_text(json.dumps(record,indent=2)+'\n')
    (a.output/'reviewed-verification-candidate.xml').write_bytes(xml)
    print(json.dumps({'reviewed':record['reviewed_artifact_count'],'pending':record['remaining_unreviewed_artifact_count'],'new_verified':record['publisher_source_copy_batches'][-1]['verified_new_files'],'active_policy_changed':False}))
