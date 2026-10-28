import contextlib
import importlib.util
import io
import os
from pathlib import Path
import unittest
from unittest.mock import patch

path=Path(__file__).resolve().parents[1]/'local-retrieval.py'
spec=importlib.util.spec_from_file_location('local_retrieval',path);runner=importlib.util.module_from_spec(spec);spec.loader.exec_module(runner)

class LocalRetrievalPreflightTest(unittest.TestCase):
    def test_dry_run_never_checks_network_or_invokes_models(self):
        with patch.object(runner,'verify_model',side_effect=AssertionError('network')),patch.object(runner.subprocess,'run',side_effect=AssertionError('process')):
            with contextlib.redirect_stdout(io.StringIO()) as output:self.assertEqual(0,runner.main(['--dry-run']))
            self.assertIn('OFFLINE_PREFLIGHT_ONLY',output.getvalue());self.assertIn('"modelRequests": 0',output.getvalue())
    def test_explicit_local_flag_is_required(self):
        with patch.dict(os.environ,{},clear=True):
            with self.assertRaises(ValueError):runner.check_execution_environment(runner.contract())
    def test_mac_cannot_execute(self):
        with patch.dict(os.environ,{'MIRROR_RUN_LOCAL_RETRIEVAL':'true'},clear=True),patch.object(runner.platform,'system',return_value='Darwin'):
            with self.assertRaises(ValueError):runner.check_execution_environment(runner.contract())
    def test_database_override_is_rejected_before_any_process(self):
        env={'MIRROR_RUN_LOCAL_RETRIEVAL':'true','MIRROR_ISOLATED_TEST_ENVIRONMENT':'new-testcontainers','MIRROR_DB_URL':'fictional-forbidden-url'}
        with patch.dict(os.environ,env,clear=True),patch.object(runner.platform,'system',return_value='Linux'),patch.object(runner.subprocess,'run',side_effect=AssertionError('process')):
            with self.assertRaises(ValueError):runner.check_execution_environment(runner.contract())
    def test_cloud_endpoint_override_is_rejected(self):
        env={'MIRROR_RUN_LOCAL_RETRIEVAL':'true','MIRROR_ISOLATED_TEST_ENVIRONMENT':'new-testcontainers','MIRROR_EMBEDDING_BASE_URL':'https://example.invalid/v1'}
        with patch.dict(os.environ,env,clear=True),patch.object(runner.platform,'system',return_value='Linux'):
            with self.assertRaises(ValueError):runner.check_execution_environment(runner.contract())

if __name__=='__main__':unittest.main()
