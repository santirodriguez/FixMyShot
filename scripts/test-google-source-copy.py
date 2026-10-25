#!/usr/bin/env python3
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('source', Path(__file__).with_name('review-google-source-copy.py'))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


class SourceCopy(unittest.TestCase):
    def setUp(self):
        self.data = b'<project>synthetic pinned POM</project>\n'
        self.blob = hashlib.sha1(b'blob '+str(len(self.data)).encode()+b'\0'+self.data).hexdigest()
        row = dict(zip(('group','name','version','file'),m.IDENTITY),sha256=m.batch.digest(self.data),proof='unreviewed fixture')
        self.metadata = m.batch.core.candidate_xml([row])
        self.prior = {'status':'PARTIAL_PUBLISHER_REVIEW__NOT_ACTIVE','active_gradle_policy_changed':False,
                      'baseline_metadata_sha256':m.batch.digest(self.metadata),'reviewed_artifacts':[],
                      'reviewed_artifact_count':0,'captured_artifact_count':1,'remaining_unreviewed_artifact_count':1}
        self.directory = b")]}'\n"+json.dumps({'id':'f'*40,'entries':[{'name':m.IDENTITY[-1],'type':'blob','mode':0o100644,'id':self.blob}]}).encode()
        self.registry = {'schema':1,'identity':list(m.IDENTITY),'repository':m.REPOSITORY,'commit':m.COMMIT,
                         'tag':'studio-2026.2.1','tag_object':m.TAG_OBJECT,'path':m.SOURCE_PATH,
                         'url':m.REPOSITORY+'/+/'+m.COMMIT+'/'+m.SOURCE_PATH,'directory_sha256':m.batch.digest(self.directory),
                         'directory_tree':'f'*40,'blob_sha1':self.blob,'bytes':len(self.data),'sha256':m.batch.digest(self.data)}

    def review(self, **changes):
        prior=json.dumps(self.prior).encode();registry=json.dumps(self.registry).encode()
        args={'metadata':self.metadata,'prior_bytes':prior,'registry_bytes':registry,'directory_bytes':self.directory,
              'data':self.data,'prior_pin':m.batch.digest(prior),'registry_pin':m.batch.digest(registry),**changes}
        return m.review(**args)

    def test_exact_file_only_without_pgp_or_active_policy_claim(self):
        before=copy.deepcopy(self.prior);result,_=self.review()
        self.assertEqual(result['reviewed_artifact_count'],1)
        self.assertEqual(self.prior,before)
        self.assertFalse(result['publisher_source_copy_reviews'][-1]['pgp_signature_claim'])
        self.assertFalse(result['active_gradle_policy_changed'])

    def test_changed_bytes_even_same_size_and_changed_directory_refused(self):
        for changes in ({'data':b'X'*len(self.data)},{'directory_bytes':self.directory+b' '}):
            with self.subTest(changes=list(changes)),self.assertRaises(ValueError):self.review(**changes)

    def test_other_publisher_commit_or_file_scope_refused(self):
        for key,value in [('repository','https://example.com'),('commit','a'*40),('path','other.pom')]:
            original=self.registry[key];self.registry[key]=value
            with self.subTest(key=key),self.assertRaises(ValueError):self.review()
            self.registry[key]=original

    def test_explicit_pins_and_unique_blob_membership_required(self):
        for changes in ({'prior_pin':'0'*64},{'registry_pin':'0'*64}):
            with self.subTest(changes=changes),self.assertRaises(ValueError):self.review(**changes)
        d=json.loads(self.directory.split(b'\n',1)[1]);d['entries']*=2
        self.directory=b")]}'\n"+json.dumps(d).encode();self.registry['directory_sha256']=m.batch.digest(self.directory)
        with self.assertRaises(ValueError):self.review()

    def test_other_publishers_are_refused_even_with_an_explicit_identity(self):
        with self.assertRaises(ValueError):
            self.review(allowed_identity=('org.example','library','1','library-1.pom'))


if __name__=='__main__':unittest.main()
