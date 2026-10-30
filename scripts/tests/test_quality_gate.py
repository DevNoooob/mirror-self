import contextlib,copy,importlib.util,io,json,shutil,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2]
def module(name,file):
 spec=importlib.util.spec_from_file_location(name,ROOT/'scripts'/file);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
q=module('qg_preflight','local-quality-gate.py');review=module('qg_review','review-quality.py')
class QualityGatePreflightTest(unittest.TestCase):
 def approved(self):
  p,sha=q.contract();env={'MIRROR_RUN_LOCAL_QG':'true','MIRROR_ISOLATED_TEST_ENVIRONMENT':'new-testcontainers','MIRROR_QG_APPROVED_PLAN_SHA':sha,'MIRROR_QG_APPROVED_DATASET_SHA':p['datasetSha256'],'MIRROR_QG_APPROVED_REFERENCE_SHA':p['referenceSha256'],'MIRROR_QG_APPROVED_GATE_SHA':p['gateSha256'],'MIRROR_QG_APPROVED_CODE_SHA':'f'*40};return p,sha,env
 def test_dry_run_does_not_touch_model_or_runtime(self):
  with patch.object(q,'local_helper',side_effect=AssertionError('no network')),patch.object(q.subprocess,'check_output',side_effect=AssertionError('no process')),contextlib.redirect_stdout(io.StringIO()) as out:
   self.assertEqual(0,q.main(['--dry-run']))
  self.assertEqual(0,json.loads(out.getvalue())['modelRequests'])
 def test_past_live_flags_are_not_consent(self):
  p,sha=q.contract()
  with self.assertRaises(ValueError):q.execution_guard(p,sha,{'MIRROR_RUN_LOCAL_RETRIEVAL':'true'},'Linux','f'*40,False)
 def test_each_scope_or_environment_change_fails_closed(self):
  p,sha,env=self.approved();q.execution_guard(p,sha,env,'Linux','f'*40,False)
  for key in [k for k in env if 'APPROVED' in k]:
   changed=env.copy();changed[key]='changed'
   with self.assertRaises(ValueError):q.execution_guard(p,sha,changed,'Linux','f'*40,False)
  for key,value in [('SPRING_DATASOURCE_URL','fictional-business-url'),('MIRROR_EMBEDDING_BASE_URL','https://example.test'),('DOCKER_HOST','ssh://other')]:
   changed=env.copy();changed[key]=value
   with self.assertRaises(ValueError):q.execution_guard(p,sha,changed,'Linux','f'*40,False)
  with self.assertRaises(ValueError):q.execution_guard(p,sha,env,'Darwin','f'*40,False)
  with self.assertRaises(ValueError):q.execution_guard(p,sha,env,'Linux','f'*40,True)
 def test_modified_inputs_cannot_reuse_plan(self):
  with tempfile.TemporaryDirectory() as folder:
   root=Path(folder)
   for name in ['evaluation/plans/m2-qg-bge-v1.json','evaluation/reviews/m2-qg-input-manifest.json','evaluation/datasets/consolidation-blind-v1.json','evaluation/references/consolidation-blind-reference-v1.json','evaluation/gates/m2-quality-gate-v1.json']:
    target=root/name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(ROOT/name,target)
   path=root/'evaluation/references/consolidation-blind-reference-v1.json';path.write_text(path.read_text()+' ')
   with self.assertRaises(ValueError):q.contract(root)
 def test_all_76_review_cards_are_unreviewed(self):
  cards=review.items();self.assertEqual(76,len(cards));self.assertEqual(76,len({x['id'] for x in cards}))
  template=json.loads((ROOT/'evaluation/reviews/m2-qg-human/decisions-template.json').read_text());self.assertFalse(template['humanReviewComplete']);self.assertTrue(all(x['decision']=='UNREVIEWED' for x in template['decisions']))
 def test_partial_review_is_only_validated_never_applied(self):
  path=ROOT/'evaluation/reviews/m2-qg-human/decisions-template.json';template=json.loads(path.read_text());template['decisions']=template['decisions'][:1];template['reviewer']='fictional-validator-test';template['decisions'][0].update(decision='CONFIRM',reason='fictional test response')
  with tempfile.TemporaryDirectory() as folder:
   target=Path(folder)/'partial.json';target.write_text(json.dumps(template));result=review.validate(target)
  self.assertFalse(result['referencesModified']);self.assertFalse(result['humanIdentityAuthenticated']);self.assertFalse(result['annotationStatusChanged']);self.assertEqual(75,result['counts']['NOT_SUBMITTED'])
