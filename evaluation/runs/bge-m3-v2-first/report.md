# M2-A retrieval quality observation

Condition: REAL_LOCAL_BGE_M3. Formal baseline: false.

Inputs SHA: `4b0cde4f771356ea86c2bf70cfab27703c7bf7d06bbebb7acd308482526f2ad4`

Reference SHA: `cd1c9933d61460657eaed2247b490ae502bc7fc7dbccbce8a0ffc1e83f1285b2`

| Mode | Recall@3 | MRR | nDCG@3 | Precision@3 | Empty rate | Evidence | S/R accuracy | p50 / p95 ms |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| KEYWORD | 0.6036 | 0.7568 | 0.6256 | 0.3694 | 0.5714 | 1.0000 | 0.8750 | 2.17 / 4.86 |
| VECTOR | 0.9414 | 0.9595 | 0.9386 | 0.5676 | 0.0000 | 1.0000 | 0.7692 | 42.13 / 81.62 |
| HYBRID | 0.9009 | 0.9009 | 0.8748 | 0.5586 | 0.0000 | 1.0000 | 0.8000 | 40.09 / 63.88 |

Precision@3 uses denominator 3, averaged over nonempty-reference queries. Empty queries scored separately. S/R accuracy only counts retrieved annotated direct pairs; end-to-end direct recall and oracle judgement are separate in JSON.

Model requests: 298. Privacy violations: 0. Wall clock unverified; durations are monotonic. Reference is synthetic and pending human review.
