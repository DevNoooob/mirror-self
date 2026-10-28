# Mirror Self Memory Quality Report

Mode: `LIVE`

Gold review: `PENDING_HUMAN_REVIEW`. Unknown paraphrases require human review.

Dataset: `memory-quality-v1.1` / `5d8dc6680c41b14bda2e455e827765131a7cdec34a143ba8b75bac790d5a9ac0`

Model: `deepseek-flash`; extractor: `llm-0.2.0-prompt1-validator2`

Code: `48bd9137adac7bfdeed4148ac29aecf4ec6541e6`; dirty: false

Started: 2026-10-27T15:11:23.889195942Z; elapsed: 51957ms; calls: 31; tokens: N/A (not_exposed_by_ModelGateway)

Call limit: 36; budget denials: 0; operator review attested: true. This flag records an operator attestation, not an AI declaration of human approval.

Precision/recall/F1 are conservative confirmed-match scores; pending matches are reported, not silently labeled FP. Zero denominators are N/A.

| Metric | Extracted numerator/denominator | Extracted | Persisted numerator/denominator | Persisted |
|---|---:|---:|---:|---:|
| precision | 8/9 | 0.8889 | 8/9 | 0.8889 |
| recall | 8/9 | 0.8889 | 8/9 | 0.8889 |
| f1 | 16/18 | 0.8889 | 16/18 | 0.8889 |
| evidenceAccuracy | 9/9 | 1.0000 | 9/9 | 1.0000 |
| attributionErrorRate | 0/9 | 0.0000 | 0/9 | 0.0000 |
| temporalAccuracy | 9/9 | 1.0000 | 9/9 | 1.0000 |
| reviewPolicyAccuracy | 0/0 | N/A | 9/9 | 1.0000 |
| unsupportedMemoryRate | 0/9 | 0.0000 | 0/9 | 0.0000 |
| resolvedCoverage | 9/9 | 1.0000 | 9/9 | 1.0000 |

Deduplication: 5/7; unauthorized gateway invocations: 0; policy violation events: 0; cases violating privacy/review policy: 0/9.

## Category metrics

| Category | Cases | Extracted P/R/F1 | Persisted P/R/F1 | Attribution errors | Temporal accuracy | Review accuracy | Unsupported rate |
|---|---:|---|---|---|---|---|---|
| assistant_attribution | 1 | N/A / N/A / N/A | N/A / N/A / N/A | N/A | N/A | N/A | N/A |
| empty_result | 2 | N/A / N/A / N/A | N/A / N/A / N/A | N/A | N/A | N/A | N/A |
| explicit_fact | 1 | 1.0000 / 1.0000 / 1.0000 | 1.0000 / 1.0000 / 1.0000 | 0.0000 | 1.0000 | 1.0000 | 0.0000 |
| future_plan | 1 | 1.0000 / 1.0000 / 1.0000 | 1.0000 / 1.0000 / 1.0000 | 0.0000 | 1.0000 | 1.0000 | 0.0000 |
| historical_fact | 1 | 0.5000 / 0.5000 / 0.5000 | 0.5000 / 0.5000 / 0.5000 | 0.0000 | 1.0000 | 1.0000 | 0.0000 |
| hypothetical | 1 | 1.0000 / 1.0000 / 1.0000 | 1.0000 / 1.0000 / 1.0000 | 0.0000 | 1.0000 | 1.0000 | 0.0000 |
| mixed_language | 1 | 1.0000 / 1.0000 / 1.0000 | 1.0000 / 1.0000 / 1.0000 | 0.0000 | 1.0000 | 1.0000 | 0.0000 |
| negation | 1 | 1.0000 / 1.0000 / 1.0000 | 1.0000 / 1.0000 / 1.0000 | 0.0000 | 1.0000 | 1.0000 | 0.0000 |
| preference | 2 | 1.0000 / 1.0000 / 1.0000 | 1.0000 / 1.0000 / 1.0000 | 0.0000 | 1.0000 | 1.0000 | 0.0000 |
| prompt_injection | 1 | N/A / N/A / N/A | N/A / N/A / N/A | N/A | N/A | N/A | N/A |
| review_policy | 1 | 1.0000 / 1.0000 / 1.0000 | 1.0000 / 1.0000 / 1.0000 | 0.0000 | 1.0000 | 1.0000 | 0.0000 |
| semantic_equivalence | 1 | 1.0000 / 1.0000 / 1.0000 | 1.0000 / 1.0000 / 1.0000 | 0.0000 | 1.0000 | 1.0000 | 0.0000 |
| temporal_change | 1 | 0.5000 / 0.5000 / 0.5000 | 0.5000 / 0.5000 / 0.5000 | 0.0000 | 1.0000 | 1.0000 | 0.0000 |
| thought_process | 1 | 1.0000 / 1.0000 / 1.0000 | 1.0000 / 1.0000 / 1.0000 | 0.0000 | 1.0000 | 1.0000 | 0.0000 |

## Stage differences and failures

| Case | Raw | Stored unique | Rejected by stage | Findings |
|---|---:|---:|---|---|
| MQ001 | 1 | 1 | {} | [] |
| MQ019 | 1 | 1 | {} | [] |
| MQ022 | 1 | 1 | {} | [PERSISTENCE:DUPLICATE_MEMORY] |
| MQ025 | 0 | 0 | {} | [] |
| MQ032 | 2 | 2 | {} | [EXTRACTION:WRONG_MEMORY_TYPE, EXTRACTION:MISSING_MEMORY, PERSISTENCE:WRONG_MEMORY_TYPE, PERSISTENCE:MISSING_MEMORY, PERSISTENCE:DUPLICATE_MEMORY] |
| MQ043 | 1 | 1 | {} | [] |
| MQ045 | 2 | 2 | {} | [] |
| MQ047 | 1 | 1 | {} | [] |
| MQ054 | 0 | 0 | {} | [] |

First-extraction validation rejections: evidence=0; semantic=0. Replay calls are included only in call/dedup counters.

## Candidate audit (reference metadata only)

A reference ID on FP means a related Gold, not a confirmed match. Pending content requires protected manual inspection; generated text is deliberately absent.

| Case | Stage/index | Outcome | Reference | Type/origin | Scope/status | Evidence UTF-16 | Rejected at | Errors |
|---|---|---|---|---|---|---|---|---|
| MQ001 | EXTRACTION/0 | CONFIRMED_TP | G1 | FACT/USER_EXPLICIT | SOURCE_TIME/null | [m1:user[0,7) selected=true] | [] | [] |
| MQ001 | PERSISTENCE/0 | CONFIRMED_TP | G1 | FACT/USER_EXPLICIT | SOURCE_TIME/ACTIVE | [m1:user[0,7) selected=true] | [] | [] |
| MQ019 | EXTRACTION/0 | CONFIRMED_TP | G1 | OPINION/USER_EXPLICIT | SOURCE_TIME/null | [m1:user[0,11) selected=true] | [] | [] |
| MQ019 | PERSISTENCE/0 | CONFIRMED_TP | G1 | OPINION/USER_EXPLICIT | SOURCE_TIME/ACTIVE | [m1:user[0,11) selected=true] | [] | [] |
| MQ022 | EXTRACTION/0 | CONFIRMED_TP | G1 | GOAL/USER_EXPLICIT | SOURCE_TIME/null | [m1:user[0,22) selected=true] | [] | [] |
| MQ022 | PERSISTENCE/0 | CONFIRMED_TP | G1 | GOAL/USER_EXPLICIT | SOURCE_TIME/ACTIVE | [m1:user[0,22) selected=true] | [] | [] |
| MQ032 | EXTRACTION/0 | CONFIRMED_FP | G1 | FACT/USER_EXPLICIT | HISTORICAL/null | [m1:user[0,15) selected=true] | [] | [WRONG_MEMORY_TYPE] |
| MQ032 | EXTRACTION/1 | CONFIRMED_TP | G2 | FACT/USER_EXPLICIT | SOURCE_TIME/null | [m3:user[0,13) selected=true] | [] | [] |
| MQ032 | PERSISTENCE/0 | CONFIRMED_FP | G1 | FACT/USER_EXPLICIT | HISTORICAL/ACTIVE | [m1:user[0,15) selected=true] | [] | [WRONG_MEMORY_TYPE] |
| MQ032 | PERSISTENCE/1 | CONFIRMED_TP | G2 | FACT/USER_EXPLICIT | SOURCE_TIME/ACTIVE | [m3:user[0,13) selected=true] | [] | [] |
| MQ043 | EXTRACTION/0 | CONFIRMED_TP | G1 | FACT/USER_EXPLICIT | SOURCE_TIME/null | [m1:user[0,10) selected=true] | [] | [] |
| MQ043 | PERSISTENCE/0 | CONFIRMED_TP | G1 | FACT/USER_EXPLICIT | SOURCE_TIME/ACTIVE | [m1:user[0,10) selected=true] | [] | [] |
| MQ045 | EXTRACTION/0 | CONFIRMED_TP | G1 | OPINION/USER_EXPLICIT | SOURCE_TIME/null | [m1:user[0,32) selected=true] | [] | [] |
| MQ045 | EXTRACTION/1 | CONFIRMED_TP | G2 | OPINION/USER_EXPLICIT | SOURCE_TIME/null | [m1:user[33,43) selected=true] | [] | [] |
| MQ045 | PERSISTENCE/0 | CONFIRMED_TP | G2 | OPINION/USER_EXPLICIT | SOURCE_TIME/ACTIVE | [m1:user[33,43) selected=true] | [] | [] |
| MQ045 | PERSISTENCE/1 | CONFIRMED_TP | G1 | OPINION/USER_EXPLICIT | SOURCE_TIME/ACTIVE | [m1:user[0,32) selected=true] | [] | [] |
| MQ047 | EXTRACTION/0 | CONFIRMED_TP | G1 | THOUGHT_PROCESS/USER_EXPLICIT | SOURCE_TIME/null | [m1:user[0,10) selected=true] | [] | [] |
| MQ047 | PERSISTENCE/0 | CONFIRMED_TP | G1 | THOUGHT_PROCESS/USER_EXPLICIT | SOURCE_TIME/PENDING_REVIEW | [m1:user[0,10) selected=true] | [] | [] |

## Baseline comparison

Comparable: false; reason: `baseline_not_supplied`

| Metric | Delta |
|---|---:|

Category deltas: `{}`

New failures: `[]`

Resolved failures: `[]`

## Optimization priorities

- P2: inspect extraction recall by category, retaining evidence and privacy gates
