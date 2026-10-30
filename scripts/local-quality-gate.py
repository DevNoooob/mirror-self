#!/usr/bin/env python3
"""Offline dry-run by default; a NEW scoped authorization is mandatory for local inference."""
import argparse,hashlib,importlib.util,json,os,platform,subprocess,sys,uuid
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
PLAN='evaluation/plans/m2-qg-bge-v1.json'
def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def contract(root=ROOT):
 p=json.loads((root/PLAN).read_text());manifest=json.loads((root/'evaluation/reviews/m2-qg-input-manifest.json').read_text())
 if digest(root/PLAN)!=manifest['planSha256']:raise ValueError('plan changed')
 for path,key in [('datasetPath','datasetSha256'),('referencePath','referenceSha256'),('gatePath','gateSha256')]:
  expected={'datasetPath':'evaluation/datasets/consolidation-blind-v1.json','referencePath':'evaluation/references/consolidation-blind-reference-v1.json','gatePath':'evaluation/gates/m2-quality-gate-v1.json'}
  if p[path]!=expected[path] or digest(root/p[path])!=p[key]:raise ValueError('frozen input changed')
 data=json.loads((root/p['datasetPath']).read_text());refs=json.loads((root/p['referencePath']).read_text());gates=json.loads((root/p['gatePath']).read_text())
 if not data['fictional'] or len(data['cases'])!=120 or len(refs['answers'])!=120 or not refs['createdBeforeModelExecution'] or refs['annotationStatus']!='SYNTHETIC_REFERENCE_PENDING_HUMAN_REVIEW':raise ValueError('invalid scope')
 if len({c['id'] for c in data['cases']})!=120 or sum(len(c['memories']) for c in data['cases'])!=600:raise ValueError('invalid input count')
 families={s:{c['family'] for c in data['cases'] if c['split']==s} for s in ['DEV','HOLDOUT']}
 if families['DEV']&families['HOLDOUT'] or any(sum(c['split']==s for c in data['cases'])!=60 for s in families):raise ValueError('split leakage')
 if p['endpoint']!='http://127.0.0.1:11434/v1' or p['model']!='bge-m3' or p['modelVersion']!='ollama-790764642607' or p['modelDigest']!='7907646426070047a77226ac3e684fbbe8410524f7b4a74d02837e43f2146bab' or p['maxEmbeddingRequests']!=600:raise ValueError('model scope changed')
 if not gates['frozenBeforeEvaluation'] or gates['candidateK']!=10:raise ValueError('gate contract changed')
 return p,manifest['planSha256']
def execution_guard(p,sha,env,system,code,dirty):
 if env.get('MIRROR_RUN_LOCAL_QG')!='true' or env.get('MIRROR_ISOLATED_TEST_ENVIRONMENT')!='new-testcontainers' or system!='Linux' or dirty:raise ValueError('isolated approved clean Linux checkout required')
 pins={'MIRROR_QG_APPROVED_PLAN_SHA':sha,'MIRROR_QG_APPROVED_DATASET_SHA':p['datasetSha256'],'MIRROR_QG_APPROVED_REFERENCE_SHA':p['referenceSha256'],'MIRROR_QG_APPROVED_GATE_SHA':p['gateSha256'],'MIRROR_QG_APPROVED_CODE_SHA':code}
 if len(code)!=40 or any(env.get(k)!=v for k,v in pins.items()):raise ValueError('fresh scope approval missing or changed')
 if any(v and (k.startswith('SPRING_DATASOURCE_') or k in ['MIRROR_DB_URL','PGHOST','DATABASE_URL']) for k,v in env.items()):raise ValueError('database overrides prohibited')
 for k,v in [('MIRROR_EMBEDDING_BASE_URL',p['endpoint']),('MIRROR_EMBEDDING_MODEL',p['model']),('MIRROR_EMBEDDING_VERSION',p['modelVersion'])]:
  if k in env and env[k]!=v:raise ValueError('model configuration differs')
 if env.get('DOCKER_HOST','unix:///var/run/docker.sock')!='unix:///var/run/docker.sock' or env.get('DOCKER_CONTEXT','default')!='default':raise ValueError('local Linux Docker socket required')
def local_helper():
 spec=importlib.util.spec_from_file_location('mirror_existing_local_preflight',ROOT/'scripts/local-retrieval.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module

def main(argv=None):
 parser=argparse.ArgumentParser();g=parser.add_mutually_exclusive_group();g.add_argument('--dry-run',action='store_true');g.add_argument('--execute',action='store_true');parser.add_argument('--enforce',action='store_true');args=parser.parse_args(argv)
 p,sha=contract();summary={k:p[k] for k in ['datasetSha256','referenceSha256','gateSha256','model','modelVersion','modelDigest','endpoint','maxEmbeddingRequests']}
 summary.update(planSha256=sha,mode='LOCAL_EXECUTE' if args.execute else 'OFFLINE_DRY_RUN',devPairs=60,holdoutPairs=60,maxInputMemories=600,queryEmbeddingRequests=0,maxMetadataGets=4,cloudCalls=0,formalBaseline=False,humanReviewComplete=False,modelRequests=0,remoteEnvironmentChecked=False,reportPattern='target/evaluation/m2-qg-v1/local-with-holdout/<run-id>/report.{json,md}')
 if not args.execute:print(json.dumps(summary,indent=2));return 0
 code=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip();dirty=bool(subprocess.check_output(['git','status','--porcelain'],cwd=ROOT,text=True).strip())
 execution_guard(p,sha,os.environ,platform.system(),code,dirty)
 if 'Java version: 21.' not in subprocess.check_output(['mvn','-v'],text=True):raise ValueError('Java21 required')
 if subprocess.check_output(['docker','context','show'],text=True).strip()!='default':raise ValueError('local Linux context required')
 subprocess.run(['docker','info','--format','{{.Name}} {{.OperatingSystem}}'],capture_output=True,text=True,check=True)
 helper=local_helper();helper.verify_model(p)
 lock=ROOT/'target/qg-local-execute.lock';lock.parent.mkdir(parents=True,exist_ok=True)
 with lock.open('x') as f:f.write('Single scoped local evaluation; never retry automatically.\n')
 child={k:v for k,v in os.environ.items() if not (k.startswith('MIRROR_LLM_') or k.startswith('OPENAI_') or k.startswith('SPRING_AI_') or k in ['OLLAMA_API_KEY','MIRROR_EMBEDDING_API_KEY'])}
 child.update(MIRROR_EXTRACTION_MODE='RULE',MIRROR_MODEL_EGRESS_ENABLED='false',MIRROR_RUN_LIVE_LLM='false',MIRROR_EVALUATION_LIVE_ENABLED='false',MIRROR_RUN_LOCAL_RETRIEVAL='false',MIRROR_EMBEDDING_MODE='LOCAL',MIRROR_EMBEDDING_BASE_URL=p['endpoint'],MIRROR_EMBEDDING_MODEL=p['model'],MIRROR_EMBEDDING_VERSION=p['modelVersion'],MIRROR_EMBEDDING_API_KEY='',MIRROR_QG_RUN_ID=str(uuid.uuid4()),DOCKER_HOST='unix:///var/run/docker.sock',DOCKER_CONTEXT='default',TESTCONTAINERS_HOST_OVERRIDE='127.0.0.1',TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE='/var/run/docker.sock')
 command=['mvn','--batch-mode','--no-transfer-progress','-Dtest=QualityContractTest','-Dit.test=LocalQualityGateIT']
 if args.enforce:command.append('-Dmirror.qg.enforce=true')
 command.append('verify');summary.update(codeSha=code,remoteEnvironmentChecked=True);print(json.dumps(summary,indent=2),flush=True)
 try:return subprocess.run(command,cwd=ROOT,env=child).returncode
 finally:helper.verify_model(p)
if __name__=='__main__':
 try:sys.exit(main())
 except Exception:print('Quality-gate preflight/execution refused or incomplete. No automatic retry; inspect frozen scope and isolated environment.',file=sys.stderr);sys.exit(2)
