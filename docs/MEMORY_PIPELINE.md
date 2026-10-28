# Memory Processing Pipeline V0.2

当前管线的完整职责、事务、证据规则和授权见 [V0.2 智能记忆引擎](V0.2_MEMORY_ENGINE.md)。

```text
原始多轮对话 → L0 独立保存 → 按分支/角色分批 → RULE 或 LLM 提取
    → JSON 校验 → 真实 user 来源/逐字引用校验 → LLM 语义校验（仅 LLM 模式）
    → 分级策略 → 批次事务内去重、L1/evidence/CONTRADICTS、审计
    → owner + ACTIVE 检索 → 本地 EXTRACTIVE_NO_LLM 问答
```

THOUGHT_PROCESS、MODEL_INFERRED 和敏感候选必须审核；不能由模型自行确认人格或价值观。
`PENDING_REVIEW --APPROVE--> ACTIVE`，`PENDING_REVIEW --REJECT--> REJECTED`。
重提取不会恢复 REJECTED。删除来源仍清理无证据的衍生记忆。
当前不自动调用 IdentityClaimRepository，不把 L1 候选直接写成 L2 人格结论。

## V0.2.2-M1 检索分支

```text
L1 创建 / 审核通过 → 成为增量索引候选（无自动模型调用）
  → POST embeddings/jobs → SQL 可见性筛选 → 本地 EmbeddingGateway
  → 1024 维/有限值/非零校验 → 修订 CAS + SQL 可见性复核 → memory_embedding

查询 → 关键词排名 +（启用时）当前模型版本向量余弦排名 → RRF(k=60)
  → 再次 SQL 过滤 / L0 evidence 装载 → 原 retrieval 数组或 explain 审计结果
```

本阶段通过显式有界增量任务补齐新建/审核后的记忆；无后台调度，无写入事务内模型调用。拒绝/撤回/删除/来源隐私变化立即使旧向量失效，且所有查询仍按当前状态过滤。切换模型版本不混合向量空间；回填前允许关键词继续服务。来源删除保留原有“仍有证据则保留 L1”的语义；剩余证据发生变化后重新索引，后续 Consolidation 必须审查其语义支持，不能靠向量判断。
