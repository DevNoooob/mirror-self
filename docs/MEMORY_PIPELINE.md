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
