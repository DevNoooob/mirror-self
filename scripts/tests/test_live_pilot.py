import contextlib
import importlib.util
import io
import json
from pathlib import Path
import unittest
import tempfile
import shutil
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('live_pilot', ROOT / 'scripts/live-pilot.py')
pilot = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pilot)

class LivePilotTests(unittest.TestCase):
    def valid_env(self):
        plan = pilot.load_plan(ROOT)
        return {'MIRROR_EVALUATION_GOLD_REVIEWED':'true','MIRROR_EVALUATION_LIVE_ENABLED':'true',
            'MIRROR_MODEL_EGRESS_ENABLED':'true','MIRROR_EVALUATION_APPROVED_DATASET_SHA256':plan['datasetSha256'],
            'MIRROR_EVALUATION_APPROVED_CASE_IDS':','.join(pilot.PILOT_IDS),'MIRROR_EVALUATION_MAX_MODEL_CALLS':'36',
            'MIRROR_LLM_MODEL':'deepseek-flash','MIRROR_LLM_BASE_URL':'https://api.deepseek.com/v1',
            'MIRROR_LLM_API_KEY':'FICTIONAL-UNIT-TEST-NOT-A-KEY'}
    def invoke(self,args,env):
        out,err = io.StringIO(),io.StringIO()
        with contextlib.redirect_stdout(out),contextlib.redirect_stderr(err):
            status = pilot.main(args,root=ROOT,env=env)
        return status,out.getvalue(),err.getvalue()
    def test_default_preflight_never_launches_a_process_even_with_old_live_flags(self):
        with patch.object(pilot.subprocess,'run') as run:
            status,out,err = self.invoke([],self.valid_env())
            self.assertEqual(0,status);run.assert_not_called()
            info=json.loads(out)
            self.assertFalse(info['liveExecuted']);self.assertNotIn('FICTIONAL-UNIT',out+err)
            self.assertEqual('APPROVED_NINE_CASES_ONLY',info['humanApproval'])
            self.assertEqual('NOT_AUTHORIZED',info['liveAuthorization'])
            self.assertEqual(57,info['unreviewedCaseCount']);self.assertFalse(info['corpusHumanReviewComplete'])
    def test_each_missing_approval_stops_before_subprocess(self):
        for field in ['MIRROR_EVALUATION_GOLD_REVIEWED','MIRROR_EVALUATION_LIVE_ENABLED','MIRROR_MODEL_EGRESS_ENABLED',
                      'MIRROR_EVALUATION_APPROVED_DATASET_SHA256','MIRROR_EVALUATION_APPROVED_CASE_IDS','MIRROR_EVALUATION_MAX_MODEL_CALLS']:
            env = self.valid_env();env.pop(field)
            with self.subTest(field=field),patch.object(pilot.subprocess,'run') as run:
                status,out,err = self.invoke(['--execute'],env)
                self.assertEqual(2,status);run.assert_not_called();self.assertNotIn('FICTIONAL-UNIT',out+err)
    def test_wrong_scope_model_and_endpoint_are_denied(self):
        for field,value in [('MIRROR_EVALUATION_CASE_IDS','MQ001'),('MIRROR_EVALUATION_APPROVED_CASE_IDS','MQ001'),
                ('MIRROR_LLM_MODEL','other-model'),('MIRROR_LLM_BASE_URL','https://unapproved.invalid/v1'),
                ('MIRROR_EVALUATION_MAX_MODEL_CALLS','100')]:
            env=self.valid_env();env[field]=value
            with self.subTest(field=field),patch.object(pilot.subprocess,'run') as run:
                self.assertEqual(2,self.invoke(['--execute'],env)[0]);run.assert_not_called()
    def test_execution_is_refused_on_mac(self):
        with patch.object(pilot.platform,'system',return_value='Darwin'),patch.object(pilot.subprocess,'run') as run:
            self.assertEqual(2,self.invoke(['--execute'],self.valid_env())[0]);run.assert_not_called()
    def test_authorized_launcher_delegates_only_to_existing_evaluator(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            for name in [pilot.ACTIVE_PLAN,'evaluation/datasets/memory-quality-v1.1.json','evaluation/reviews/memory-quality-v1.1-human-review.json']:
                dest=root/name;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(ROOT/name,dest)
            def fake_maven(command,**kwargs):
                output=Path(next(a.split('=',1)[1] for a in command if a.startswith('-Devaluation.output=')))
                output.mkdir(parents=True)
                plan=pilot.load_plan(root)
                report={'run':{'mode':'LIVE','datasetSha256':plan['datasetSha256'],'datasetVersion':plan['datasetVersion'],
                    'model':plan['model'],'operatorReviewAttested':True,'modelCallLimit':36,'budgetDenied':0,'modelCalls':18},
                    'cases':[{'caseId':i} for i in pilot.PILOT_IDS]}
                (output/'report.json').write_text(json.dumps(report))
                return type('Result',(),{'returncode':0})()
            with patch.object(pilot.platform,'system',return_value='Linux'),patch.object(pilot.subprocess,'run',side_effect=fake_maven) as run:
                out,err=io.StringIO(),io.StringIO()
                with contextlib.redirect_stdout(out),contextlib.redirect_stderr(err):
                    self.assertEqual(0,pilot.main(['--execute'],root=root,env=self.valid_env()))
                command=run.call_args.args[0];env=run.call_args.kwargs['env']
                self.assertIn('evaluate-memory.sh',command[1]);self.assertEqual('live',command[2])
                self.assertEqual(','.join(pilot.PILOT_IDS),env['MIRROR_EVALUATION_CASE_IDS'])
                self.assertEqual('false',env['MIRROR_RUN_LIVE_LLM'])
                self.assertFalse(any('FICTIONAL-UNIT' in arg for arg in command))
    def test_zero_maven_exit_with_no_report_is_not_evaluation_success(self):
        with patch.object(pilot.platform,'system',return_value='Linux'),patch.object(pilot.subprocess,'run') as run:
            run.return_value.returncode=0
            status,out,err=self.invoke(['--execute'],self.valid_env())
            self.assertEqual(2,status);self.assertIn('no fresh report',err)
    def test_v1_digest_cannot_authorize_v11(self):
        env=self.valid_env()
        env['MIRROR_EVALUATION_APPROVED_DATASET_SHA256']='b5801339e5a182dacbfd1563a67b86e66a4a6520c64d02757d3c986ac756a652'
        with patch.object(pilot.subprocess,'run') as run:
            self.assertEqual(2,self.invoke(['--execute'],env)[0]);run.assert_not_called()
    def test_gold_review_alone_never_launches_live(self):
        with patch.object(pilot.subprocess,'run') as run:
            self.assertEqual(2,self.invoke(['--execute'],{})[0]);run.assert_not_called()
    def test_overbroad_human_review_record_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            for name in [pilot.ACTIVE_PLAN,'evaluation/datasets/memory-quality-v1.1.json','evaluation/reviews/memory-quality-v1.1-human-review.json']:
                dest=root/name;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(ROOT/name,dest)
            path=root/'evaluation/reviews/memory-quality-v1.1-human-review.json';original=json.loads(path.read_text())
            for change in [{'reviewedCaseIds':list(pilot.PILOT_IDS)+['MQ002']},{'corpusHumanReviewComplete':True}]:
                path.write_text(json.dumps({**original,**change}))
                with self.subTest(change=change):
                    self.assertRaises(pilot.PreflightError,pilot.load_plan,root)
    def test_bad_arguments_are_not_echoed(self):
        status,out,err=self.invoke(['PRIVATE_ARGUMENT_SENTINEL'],{})
        self.assertEqual(2,status);self.assertNotIn('PRIVATE_ARGUMENT_SENTINEL',out+err)

if __name__ == '__main__':
    unittest.main()
