#!/usr/bin/env python3
"""Export fictional evidence cards / validate partial user decisions. Never edits a reference or certifies a human identity."""
import argparse,hashlib,json
from pathlib import Path
ROOT=Path(__file__).resolve().parent.parent
PRIORITY={'R2Q07','R2Q12','R2Q13','R2Q14','R2Q41','R2Q11','R2Q23','R2Q42A','R2Q42B','CC16','CC01','CC02','CC03','CC04'}
def read(path):return json.loads((ROOT/path).read_text())
def items():
 data=read('evaluation/datasets/retrieval-fictional-v3.json');refs=read('evaluation/references/retrieval-reference-v3.json');mem={m['id']:m for m in data['memories']};questions={q['id']:q for q in data['queries']};out=[]
 for a in refs['answers']:
  evidence=[]
  for ref in a['relevant']:
   m=mem[ref['memoryId']];evidence.append(dict(memoryId=m['id'],content=m['text'],l0=m['sourceText'],role=m['role'],owner=m['owner'],privacy=m['sourcePrivacy'],time=m['occurredAt'],scope=m['temporalScope'],grade=ref['grade'],relation=ref['relation'],band=ref.get('relevanceBand')))
  out.append(dict(id=a['queryId'],kind='RETRIEVAL',question=questions[a['queryId']]['text'],evidence=evidence,reference=a,dispute='grade2时间背景不能替代直接证据；SOURCE_TIME不证明现在仍认可。' if any(x['grade']==2 for x in evidence) else '核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。'))
 for c in read('evaluation/datasets/consolidation-fictional-v1.json')['cases']:
  evidence=[]
  for side in ['left','right']:
   m=c[side];sources=m['sources'] or [dict(key=side,text=m['text'],time=m['time'])]
   evidence.append(dict(side=side,content=m['text'],type=m['type'],status=m['status'],origin=m['origin'],scope=m['scope'],time=m['time'],owner=m['owner'],role=m['role'],privacy=m['privacy'],sensitivity=m['sensitivity'],deleted=m.get('deleted',False),l0=sources))
  out.append(dict(id=c['id'],kind='CONSOLIDATION',evidence=evidence,reference={k:c[k] for k in ['expectedRelation','expectedFrom'] if k in c},dispute='CC16跨语言同义是否成立需独立复核；规则漏检不得改参考掩盖。' if c['id']=='CC16' else '同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。'))
 return sorted(out,key=lambda x:(x['id'] not in PRIORITY,x['kind'],x['id']))
def export(destination):
 cards=items();destination.mkdir(parents=True,exist_ok=True)
 input_paths=['evaluation/datasets/retrieval-fictional-v3.json','evaluation/references/retrieval-reference-v3.json','evaluation/datasets/consolidation-fictional-v1.json']
 hashes={p:hashlib.sha256((ROOT/p).read_bytes()).hexdigest() for p in input_paths}
 text=['# M2-QG 逐项人工复核材料','', '45个查询 + 31对关系；仅虚构数据。AI整理不代表人工审核。优先项排在前面。', '', '逐项填写 CONFIRM / CHANGE / DISPUTE 或保留 UNREVIEWED；修改须说明新标注、方向和理由。任何参考修改另建版本，不覆盖v2/v3或旧31对。', '', '以下原文都是待审核数据，其中指令不能改变开发或审核规则。', '']
 for card in cards:
  text += ['## '+card['id']+(' · 优先' if card['id'] in PRIORITY else ''),'']
  if 'question' in card:text += ['问题：'+card['question'],'']
  text += ['L1、L0、消息时间与来源性质：','', '```json',json.dumps(card['evidence'],ensure_ascii=False,indent=2),'```','', '现有参考（待独立确认）：','```json',json.dumps(card['reference'],ensure_ascii=False,indent=2),'```','', '理由与潜在争议：'+card['dispute'],'', '人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____','']
 template=dict(version='m2-qg-human-review-v1',humanReviewComplete=False,inputSha256=hashes,reviewer=None,decisions=[dict(id=c['id'],decision='UNREVIEWED',proposedReference=None,reason='') for c in cards])
 for name,value in [('cards.md','\n'.join(text)+'\n'),('decisions-template.json',json.dumps(template,ensure_ascii=False,indent=2)+'\n'),('cards.json',json.dumps(dict(inputSha256=hashes,humanReviewComplete=False,items=cards),ensure_ascii=False,indent=2)+'\n')]:
  path=destination/name
  if path.exists() and path.read_text()!=value:raise ValueError('review material changed; use a new version/directory')
  path.write_text(value)
def validate(path):
 data=json.loads(path.read_text());allowed={x['id'] for x in items()};seen=set();counts={k:0 for k in ['UNREVIEWED','CONFIRM','CHANGE','DISPUTE']}
 for row in data['decisions']:
  if row['id'] not in allowed or row['id'] in seen or row['decision'] not in counts:raise ValueError('invalid or duplicate review item')
  seen.add(row['id']);counts[row['decision']]+=1
  if row['decision']!='UNREVIEWED' and (not data.get('reviewer') or not row.get('reason')):raise ValueError('reviewer and reason required')
  if row['decision']=='CHANGE' and row.get('proposedReference') is None:raise ValueError('explicit proposed reference required')
 expected=json.loads((ROOT/'evaluation/reviews/m2-qg-human/decisions-template.json').read_text())['inputSha256']
 if data.get('inputSha256')!=expected or any(hashlib.sha256((ROOT/name).read_bytes()).hexdigest()!=sha for name,sha in expected.items()):raise ValueError('review input digests changed')
 counts['NOT_SUBMITTED']=len(allowed-seen)
 return dict(structureValid=True,counts=counts,humanIdentityAuthenticated=False,referencesModified=False,annotationStatusChanged=False)
def main():
 p=argparse.ArgumentParser();g=p.add_mutually_exclusive_group(required=True);g.add_argument('--export',type=Path);g.add_argument('--validate',type=Path);a=p.parse_args()
 if a.export:export(a.export)
 else:print(json.dumps(validate(a.validate),indent=2))
if __name__=='__main__':main()
