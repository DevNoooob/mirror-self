# M2-QG quality observation

Condition: REAL_LOCAL_BGE_M3; human reference review pending; formal baseline=false; V0.3 ready=false.

| Split/mode | Candidate R@10 | P | R | F1 | False SAME_AS | Failed gates |
|---|---:|---:|---:|---:|---:|---|
| DEV_KEYWORD | 0.8333 | 1.0000 | 0.8333 | 0.9091 | 0 | [CANDIDATE_RECALL] |
| DEV_HYBRID | 1.0000 | 1.0000 | 0.8333 | 0.9091 | 0 | [] |
| HOLDOUT_KEYWORD | 0.6000 | 1.0000 | 0.6000 | 0.7500 | 0 | [CANDIDATE_RECALL, RELATION_RECALL, RELATION_F1] |
| HOLDOUT_HYBRID | 1.0000 | 1.0000 | 0.6400 | 0.7805 | 0 | [RELATION_RECALL, RELATION_F1] |

Each JSON detail separates CANDIDATE_RECALL from RELATION_CLASSIFICATION, with raw keyword/vector/fused fixture IDs and abstention codes. Vector similarity is not an entailment label.
