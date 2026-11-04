#!/usr/bin/env python3
"""Offline reconciliation boundaries for captured security evidence."""
import copy
import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('review', Path(__file__).with_name('review-dependency-security.py'))
review = importlib.util.module_from_spec(spec)
spec.loader.exec_module(review)


class Replay(unittest.TestCase):
    def setUp(self):
        self.rows = [{'coordinate': 'g:a:1', 'scope': 'capture_without_lockfile_selection', 'query': {'version': '1'}}]
        self.component = {**self.rows[0], 'scope': 'capture_only_unselected', 'query_complete': True,
                          'database_match_ids': [], 'active_match_ids': [], 'status': 'NO_DATABASE_MATCH_AT_SCAN_TIME'}
        self.report = {'components': [self.component]}
        self.responses = [{'ids': [], 'incomplete': False}]

    def test_no_match_is_time_scoped_and_label_is_corrected(self):
        result = review.reconcile(self.rows, self.report, self.responses, {})
        self.assertEqual(result[0]['status'], 'NO_DATABASE_MATCH_AT_SCAN_TIME')
        self.assertEqual(result[0]['scope'], 'capture_without_lockfile_selection')

    def test_missing_results_and_pagination_refused(self):
        with self.assertRaises(ValueError):
            review.reconcile(self.rows, self.report, [], {})
        self.responses[0]['incomplete'] = True
        with self.assertRaises(ValueError):
            review.reconcile(self.rows, self.report, self.responses, {})

    def test_changed_scope_or_false_clean_refused(self):
        self.component['scope'] = 'app_release_runtime'
        with self.assertRaises(ValueError):
            review.reconcile(self.rows, self.report, self.responses, {})
        self.component['scope'] = 'capture_only_unselected'
        self.responses[0]['ids'] = ['GHSA-example']
        with self.assertRaises(ValueError):
            review.reconcile(self.rows, self.report, self.responses, {'GHSA-example': {}})

    def test_withdrawn_match_is_separate_from_clean_or_active(self):
        self.component.update(database_match_ids=['GHSA-example'], status='WITHDRAWN_MATCHES_ONLY')
        self.responses[0]['ids'] = ['GHSA-example']
        result = review.reconcile(self.rows, self.report, self.responses, {'GHSA-example': {'withdrawn': '2026-01-01'}})
        self.assertEqual(result[0]['active_match_ids'], [])
        self.assertEqual(result[0]['status'], 'WITHDRAWN_MATCHES_ONLY')
        with self.assertRaises(ValueError):
            review.reconcile(self.rows, self.report, self.responses, {})


if __name__ == '__main__':
    unittest.main()
