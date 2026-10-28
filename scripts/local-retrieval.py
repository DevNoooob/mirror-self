#!/usr/bin/env python3
"""Dry-run by default. Explicit LOCAL execution uses only the pinned fictional corpus."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import subprocess
import sys
import urllib.request

ROOT=Path(__file__).resolve().parent.parent

def contract(root=ROOT):
    plan=json.loads((root/'evaluation/plans/bge-m3-retrieval-v2.json').read_text())
    for path,key in [('datasetPath','datasetSha256'),('referencePath','referenceSha256')]:
        if hashlib.sha256((root/plan[path]).read_bytes()).hexdigest()!=plan[key]:
            raise ValueError('dataset/reference digest mismatch; do not execute')
    data=json.loads((root/plan['datasetPath']).read_text());refs=json.loads((root/plan['referencePath']).read_text())
    if not data.get('fictional') or len(data['queries'])!=plan['queryCount'] or plan['queryCount']<30:
        raise ValueError('invalid fictional query scope')
    if not refs['createdBeforeModelExecution'] or refs['annotationStatus']!='SYNTHETIC_REFERENCE_PENDING_HUMAN_REVIEW':
        raise ValueError('invalid independent reference status')
    if plan['endpoint']!='http://127.0.0.1:11434/v1' or plan['model']!='bge-m3' or not 1<=plan['maxEmbeddingRequests']<=400:
        raise ValueError('local execution target or request limit changed')
    if len(data['memories'])+2*(plan['queryCount']*plan['repeats']+1)>plan['maxEmbeddingRequests']:
        raise ValueError('request upper bound exceeds the pinned limit')
    if plan['modelVersion']!='ollama-'+plan['modelDigest'][:12]:
        raise ValueError('model version and digest do not match')
    return plan

def check_execution_environment(plan):
    if os.environ.get('MIRROR_RUN_LOCAL_RETRIEVAL')!='true':
        raise ValueError('set MIRROR_RUN_LOCAL_RETRIEVAL=true for explicit local execution')
    if platform.system()!='Linux':
        raise ValueError('execute only in the confirmed isolated Ubuntu/Linux process, not Mac')
    if os.environ.get('MIRROR_ISOLATED_TEST_ENVIRONMENT')!='new-testcontainers':
        raise ValueError('confirm new-testcontainers isolation explicitly')
    for key in ('MIRROR_DB_URL','SPRING_DATASOURCE_URL'):
        if os.environ.get(key):
            raise ValueError('database URL overrides are prohibited; the test creates a new temporary database')
    for key,expected in [('MIRROR_EMBEDDING_BASE_URL',plan['endpoint']),('MIRROR_EMBEDDING_MODEL',plan['model']),('MIRROR_EMBEDDING_VERSION',plan['modelVersion'])]:
        if key in os.environ and os.environ[key]!=expected:
            raise ValueError('embedding execution configuration differs from the pinned plan')
    version=subprocess.run(['mvn','-v'],capture_output=True,text=True,check=True).stdout
    if 'Java version: 21.' not in version:
        raise ValueError('Maven must use Java 21')
    subprocess.run(['docker','info','--format','{{.Name}} {{.OperatingSystem}}'],check=True,capture_output=True,text=True)

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self,*args,**kwargs):
        raise ValueError('local model metadata redirects are prohibited')

def verify_model(plan):
    opener=urllib.request.build_opener(urllib.request.ProxyHandler({}),NoRedirect())
    try:
        with opener.open('http://127.0.0.1:11434/api/tags',timeout=5) as response:
            models=json.loads(response.read(100001))['models']
        if not any(m['name']==plan['model']+':latest' and m['digest']==plan['modelDigest'] for m in models):
            raise ValueError()
    except Exception:
        raise ValueError('installed local BGE-M3 digest verification failed') from None

def main(argv=None):
    parser=argparse.ArgumentParser();group=parser.add_mutually_exclusive_group()
    group.add_argument('--dry-run',action='store_true');group.add_argument('--execute',action='store_true')
    args=parser.parse_args(argv);plan=contract()
    summary={k:plan[k] for k in ('datasetSha256','referenceSha256','queryCount','model','modelVersion','modelDigest','maxEmbeddingRequests')}
    summary['requestUpperBound']=plan['memories']+2*(plan['queryCount']*plan['repeats']+1)
    summary.update(mode='LOCAL_EXECUTE' if args.execute else 'OFFLINE_PREFLIGHT_ONLY',cloudCalls=0,formalBaseline=False)
    if not args.execute:
        summary.update(modelRequests=0,remoteEnvironmentChecked=False);print(json.dumps(summary,indent=2));return 0
    check_execution_environment(plan);verify_model(plan)
    lock=ROOT/'target/local-retrieval-execute.lock';lock.parent.mkdir(parents=True,exist_ok=True)
    with lock.open('x') as f:f.write('one explicit local execution in this isolated checkout\n')
    child=os.environ.copy()
    for key in ('MIRROR_LLM_API_KEY','OPENAI_API_KEY','OLLAMA_API_KEY','MIRROR_DB_URL','SPRING_DATASOURCE_URL'):
        child.pop(key,None)
    child.update(MIRROR_APPROVED_RETRIEVAL_DATASET_SHA=plan['datasetSha256'],MIRROR_APPROVED_RETRIEVAL_REFERENCE_SHA=plan['referenceSha256'],
        MIRROR_EXTRACTION_MODE='RULE',MIRROR_MODEL_EGRESS_ENABLED='false',MIRROR_RUN_LIVE_LLM='false',MIRROR_EVALUATION_LIVE_ENABLED='false',
        MIRROR_EMBEDDING_MODE='LOCAL',MIRROR_EMBEDDING_BASE_URL=plan['endpoint'],MIRROR_EMBEDDING_MODEL=plan['model'],MIRROR_EMBEDDING_VERSION=plan['modelVersion'],MIRROR_EMBEDDING_API_KEY='')
    print(json.dumps(summary,indent=2),flush=True)
    result=subprocess.run(['mvn','--batch-mode','--no-transfer-progress','-Dtest=EvidenceSupportJudgeTest,RetrievalCorpusTest',
        '-Dit.test=LocalBgeRetrievalIT','verify'],cwd=ROOT,env=child)
    verify_model(plan)
    return result.returncode

if __name__=='__main__':
    try:sys.exit(main())
    except Exception:
        print('Local retrieval preflight/execution failed; check pinned inputs, local runtime and model metadata. No automatic retry.',file=sys.stderr)
        sys.exit(2)
