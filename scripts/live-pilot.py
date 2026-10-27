#!/usr/bin/env python3
"""Preflight/guarded launcher only; the existing Java evaluator performs all extraction and scoring."""
import datetime
import hashlib
import json
import os
import platform
from pathlib import Path
import subprocess
import sys

PILOT_IDS = ('MQ001','MQ019','MQ022','MQ025','MQ032','MQ043','MQ045','MQ047','MQ054')

class PreflightError(Exception):
    pass

def load_plan(root):
    plan = json.loads((root / 'evaluation/plans/deepseek-flash-nine-v1.json').read_text())
    if (plan.get('planVersion') != '1.0' or tuple(plan.get('caseIds', [])) != PILOT_IDS
            or plan.get('model') != 'deepseek-flash' or plan.get('baseUrl') != 'https://api.deepseek.com/v1'
            or plan.get('maxModelCalls') != 36 or plan.get('batchChars') != 16000 or plan.get('batchMessages') != 30):
        raise PreflightError('Pilot scope differs from the review proposal; review a new plan first.')
    dataset = (root / plan['datasetPath']).resolve()
    if dataset.parent != (root / 'evaluation/datasets').resolve():
        raise PreflightError('Pilot dataset must be an explicit evaluation dataset.')
    raw = dataset.read_bytes()
    if hashlib.sha256(raw).hexdigest() != plan['datasetSha256']:
        raise PreflightError('Dataset changed; obtain review and approval for a new digest.')
    data = json.loads(raw)
    if not data.get('fictional') or data.get('datasetVersion') != plan['datasetVersion']:
        raise PreflightError('Pilot dataset metadata differs from the reviewed proposal.')
    cases = {c['caseId']: c for c in data['cases']}
    if not all(i in cases for i in PILOT_IDS):
        raise PreflightError('Pilot cases are missing.')
    if any(cases[i]['repeatExtractions'] != 2 for i in PILOT_IDS):
        raise PreflightError('Pilot replay scope changed.')
    return plan

def ids(value):
    parts = [p.strip() for p in (value or '').split(',')]
    if not parts or any(not p for p in parts) or len(parts) != len(set(parts)):
        raise PreflightError('Explicit unique approved case IDs are required.')
    return set(parts)

def require_approval(plan, env):
    for field in ('MIRROR_EVALUATION_GOLD_REVIEWED', 'MIRROR_EVALUATION_LIVE_ENABLED', 'MIRROR_MODEL_EGRESS_ENABLED'):
        if env.get(field) != 'true':
            raise PreflightError('Required explicit approval is missing: ' + field)
    if env.get('MIRROR_EVALUATION_APPROVED_DATASET_SHA256') != plan['datasetSha256']:
        raise PreflightError('Explicit approval of the current dataset digest is missing.')
    if ids(env.get('MIRROR_EVALUATION_APPROVED_CASE_IDS')) != set(PILOT_IDS):
        raise PreflightError('Approved case scope must be exactly the nine pilot cases.')
    if 'MIRROR_EVALUATION_CASE_IDS' in env and ids(env['MIRROR_EVALUATION_CASE_IDS']) != set(PILOT_IDS):
        raise PreflightError('Requested case scope differs from the pilot.')
    if env.get('MIRROR_EVALUATION_MAX_MODEL_CALLS') != str(plan['maxModelCalls']):
        raise PreflightError('Explicit approval of the pilot call limit is missing.')
    if env.get('MIRROR_LLM_MODEL') != plan['model'] or env.get('MIRROR_LLM_BASE_URL','').rstrip('/') != plan['baseUrl']:
        raise PreflightError('Configured model or endpoint differs from the pilot proposal.')
    if env.get('MIRROR_LLM_LOCAL','false') != 'false' or not env.get('MIRROR_LLM_API_KEY','').strip():
        raise PreflightError('Inject valid cloud configuration through a secure environment.')

def require_result(plan, output):
    path = output / 'report.json'
    if not path.is_file():
        raise PreflightError('Live produced no fresh report; skipped tests are not successful evaluation.')
    report = json.loads(path.read_text())
    run = report.get('run', {})
    case_ids = [c.get('caseId') for c in report.get('cases', [])]
    if (run.get('mode') != 'LIVE' or run.get('datasetSha256') != plan['datasetSha256']
            or run.get('datasetVersion') != plan['datasetVersion'] or run.get('model') != plan['model']
            or len(case_ids) != len(PILOT_IDS) or set(case_ids) != set(PILOT_IDS)
            or run.get('operatorReviewAttested') is not True or run.get('modelCallLimit') != plan['maxModelCalls']
            or run.get('budgetDenied') != 0 or not 0 < run.get('modelCalls',0) <= plan['maxModelCalls']):
        raise PreflightError('Report mode, scope, digest or call accounting differs from the authorized pilot.')
    # No score threshold: bad model quality remains visible for independent analysis.

def main(argv=None, root=None, env=None):
    argv = list(sys.argv[1:] if argv is None else argv)
    root = Path(__file__).resolve().parents[1] if root is None else Path(root)
    env = dict(os.environ if env is None else env)
    try:
        if argv not in ([], ['--dry-run'], ['--execute']):
            raise PreflightError('Usage: python3 scripts/live-pilot.py [--dry-run|--execute]')
        plan = load_plan(root)
        if argv != ['--execute']:
            # No credentials read from files, subprocess, Docker, network or provider call.
            print(json.dumps({'mode':'PREFLIGHT_ONLY','datasetSha256':plan['datasetSha256'],
                'datasetVersion':plan['datasetVersion'],'caseIds':plan['caseIds'],'model':plan['model'],
                'maxModelCalls':plan['maxModelCalls'],'replaysPerCase':2,
                'humanApproval':'PENDING_INDEPENDENT_HUMAN_REVIEW','liveExecuted':False,
                'reportPattern':plan['reportDirectory']+'/<UTC_RUN_ID>/report.{json,md}'},ensure_ascii=False,indent=2))
            return 0
        require_approval(plan, env)
        if platform.system() != 'Linux':
            raise PreflightError('Execute only in an explicitly authorized isolated Linux test environment; do not start Mac Docker Desktop.')
        run_id = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
        output = (root / plan['reportDirectory'] / run_id).resolve()
        if not output.is_relative_to((root / 'target/evaluation').resolve()) or output.exists():
            raise PreflightError('A fresh isolated evaluation report path is required.')
        # Set only non-secret scope/limits. Never turn approval/egress flags on for the operator.
        child_env = {**env, 'MIRROR_RUN_LIVE_LLM':'false',
            'MIRROR_EVALUATION_CASE_IDS':','.join(PILOT_IDS),
            'MIRROR_EVALUATION_DATASET':plan['datasetPath'],
            'MIRROR_LLM_BATCH_CHARS':str(plan['batchChars']),
            'MIRROR_LLM_BATCH_MESSAGES':str(plan['batchMessages'])}
        command = ['bash', str(root / 'scripts/evaluate-memory.sh'), 'live',
            '-Devaluation.output='+str(output),
            '-Devaluation.baseline=evaluation/baselines/live-deepseek-flash-nine-v1.json']
        print('Launching the existing evaluator; report directory: '+str(output),flush=True)
        result = subprocess.run(command,cwd=root,env=child_env,check=False)
        if result.returncode != 0:
            return result.returncode
        require_result(plan, output)
        print('Fresh scoped LIVE report saved; no baseline was automatically frozen.')
        return 0
    except PreflightError as ex:
        print(str(ex),file=sys.stderr)
        return 2
    except (OSError,ValueError,KeyError,TypeError):
        # Parser/OS errors may contain source text or credentials: do not print them.
        print('Pilot preflight failed; inspect the local plan and environment without logging secrets.',file=sys.stderr)
        return 2

if __name__ == '__main__':
    raise SystemExit(main())
