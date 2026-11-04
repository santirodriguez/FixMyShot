#!/usr/bin/env python3
"""Bounded data-only lookup of unreviewed Google POMs in one immutable AOSP release."""
import argparse
import base64
from concurrent.futures import ThreadPoolExecutor
import hashlib
import importlib.util
import json
from pathlib import Path
import threading
import time
import urllib.error
import urllib.request

spec=importlib.util.spec_from_file_location('reviewer',Path(__file__).with_name('review-google-source-copy.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self,*args,**kwargs):raise ValueError('Source redirect refused')


def collect(metadata,prior_bytes,prior_pin,output):
    prior,rows,known=m.batch.checked_receipt(prior_bytes,metadata,prior_pin)
    attempted={tuple(x['identity']) for entry in prior.get('publisher_source_copy_batches',[]) if entry.get('commit')==m.COMMIT for x in entry.get('outcomes',[])}
    selected=[r for identity,r in sorted(rows.items()) if identity not in known and
              identity not in attempted and r['group'].startswith(('androidx.','com.android.')) and r['file']==r['name']+'-'+r['version']+'.pom']
    if not selected:raise ValueError('No unattempted POMs in this publisher release')
    if len(selected)>96:raise ValueError('Source lookup row budget exceeded')
    output.mkdir(parents=True,exist_ok=False)
    deadline=time.monotonic()+120;remaining=[16*1024*1024];guard=threading.Lock()
    def get(url,limit):
        if time.monotonic()>=deadline:raise TimeoutError('Source lookup budget exhausted')
        opener=urllib.request.build_opener(NoRedirect())
        with opener.open(urllib.request.Request(url,headers={'User-Agent':'FixMyShot-source-review/1'}),timeout=min(15,deadline-time.monotonic())) as response:
            if response.geturl()!=url or response.status!=200:raise ValueError('Source origin/status changed')
            data=response.read(limit+1)
        with guard:
            remaining[0]-=len(data)
            if len(data)>limit or remaining[0]<0 or time.monotonic()>deadline:raise ValueError('Source lookup byte/time budget exceeded')
        return data
    def one(pair):
        index,row=pair;identity=tuple(row[k] for k in ('group','name','version','file'))
        # Canonical path validation is independent of the network response.
        m.batch.planner.remote_filename(row)
        path='common/m2/repository/'+row['group'].replace('.','/')+'/'+row['name']+'/'+row['version']+'/'+row['file']
        url=m.REPOSITORY+'/+/'+m.COMMIT+'/'+path;item={'id':'pom-'+str(index).zfill(3),'identity':list(identity),'expected_sha256':row['sha256'],'url':url,'status':'UNAVAILABLE'}
        try:
            directory=get(url.rsplit('/',1)[0]+'/?format=JSON',65536)
            encoded=get(url+'?format=TEXT',180000);data=base64.b64decode(encoded,validate=True)
            if len(data)>131072:raise ValueError('Source POM too large')
            (output/(item['id']+'.directory.json')).write_bytes(directory);(output/(item['id']+'.pom')).write_bytes(data)
            parsed=json.loads(directory.split(b'\n',1)[1]);blob=hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()
            registry={'schema':1,'identity':list(identity),'repository':m.REPOSITORY,'commit':m.COMMIT,'tag':'studio-2026.2.1','tag_object':m.TAG_OBJECT,'path':path,'url':url,'directory_sha256':m.batch.digest(directory),'directory_tree':parsed['id'],'blob_sha1':blob,'bytes':len(data),'sha256':m.batch.digest(data)}
            (output/(item['id']+'.registry.json')).write_text(json.dumps(registry,indent=2)+'\n')
            item.update(status='MATCHED_FROZEN_BYTES' if registry['sha256']==row['sha256'] else 'DIFFERENT_SOURCE_BYTES_NO_APPROVAL',registry_sha256=m.batch.digest((output/(item['id']+'.registry.json')).read_bytes()))
        except urllib.error.HTTPError as error:item['status']='HTTP_'+str(error.code)
        except (OSError,ValueError,KeyError,IndexError) as error:item['status']='UNAVAILABLE_'+type(error).__name__
        return item
    with ThreadPoolExecutor(max_workers=6) as pool:results=list(pool.map(one,enumerate(selected)))
    report={'schema':1,'scope':'GOOGLE_PUBLISHED_POMS_ONE_IMMUTABLE_AOSP_RELEASE','metadata_sha256':m.batch.digest(metadata),'prior_review_sha256':prior_pin,'collector_sha256':m.batch.digest(Path(__file__).read_bytes()),'repository':m.REPOSITORY,'commit':m.COMMIT,'tag_object':m.TAG_OBJECT,'automatic_authentication':False,'results':results}
    (output/'report.json').write_text(json.dumps(report,indent=2)+'\n');return report


if __name__=='__main__':
    p=argparse.ArgumentParser()
    for name in ('metadata','prior','output'):p.add_argument(name,type=Path)
    p.add_argument('--expected-prior',required=True);a=p.parse_args()
    r=collect(m.batch.core.bounded_bytes(a.metadata,3_000_000),m.batch.core.bounded_bytes(a.prior,2_000_000),a.expected_prior,a.output)
    print(json.dumps({'rows':len(r['results']),'matched_bytes':sum(x['status']=='MATCHED_FROZEN_BYTES' for x in r['results']),'automatic_authentication':False}))
