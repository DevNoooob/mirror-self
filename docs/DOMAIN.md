# Memory Domain Model V0.1

本阶段的领域对象主要在 `memory/`、`ingestion/`、`identity/`、`retrieval/` 包中；使用 Java `record` 和枚举。

| 类型 | 描述 |
|---|---|
| `SourceInput` | 归一化的 L0 原始节点资料输入，包含来源与发生时间 |
| `SourceRepository.Stored` | 来源存储结果及是否新建 |
| `MemoryCandidate` | 候选记忆，包含原文摘录与发生时间 |
| `MemoryPolicy` | 不依赖 Spring 的记忆初始状态与审核规则 |
| `MemoryRepository.MemoryView` | L1 查询对象，包含 owner 授权下的 evidence |
| `MemoryRepository.Evidence` | 可追溯到 source_record 的文字证据 |
| `IdentityClaimRepository.Claim` | L2 的九维认知属性及状态 |
| `RetrievalService.ExtractiveAnswer` | 本地摘录式回答及支持证据 |

## 枚举

- MemoryType：`FACT`, `EVENT`, `KNOWLEDGE`, `OPINION`, `GOAL`, `THOUGHT_PROCESS`
- AssertionOrigin：`USER_EXPLICIT`, `OBSERVED`, `MODEL_INFERRED`
- Sensitivity：`NORMAL`, `SENSITIVE`, `SECRET`（**NORMAL != PUBLIC**）
- MemoryStatus：`CANDIDATE`, `PENDING_REVIEW`, `ACTIVE`, `REJECTED`, `SUPERSEDED`, `RETRACTED`
- ReviewDecision：`APPROVE`, `REJECT`
- IdentityDimension：`EXPERIENCE`, `KNOWLEDGE`, `COGNITION`, `PERSONALITY`, `VALUES`, `EMOTION`, `RELATIONSHIPS`, `MOTIVATION`, `SELF_PERCEPTION`

## 服务责任

- `ChatGptDecoder`：只负责解析 ChatGPT 的常见 JSON 结构，不判断人格。
- `IngestionUnitService`：每条原始消息与候选记忆的事务边界。
- `MemoryService`：原始资料手动补充、记忆生成、审核、删除、审计。
- `MemoryRepository`：持久化、owner scoped 搜索，绑定证据。
- `IdentityClaimRepository`：L2 认知候选和人工审核。
- `RetrievalService`：只调用已验证的 owner-scoped repository，不访问其他来源。
- `ModelGateway`：未来外部模型适配接口；V0.1 不提供已授权实现，绝不绕过 Privacy Gateway。

**V0.2 接口演进**：`CandidateExtractor`、`MemoryConsolidationPolicy`、`PrivacyGateway`、`EvidenceVerifier`、`CognitiveEvolutionService` 应逐步成为独立扩展点，不要让外部模型在 JDBC 层直接写入认知事实。
