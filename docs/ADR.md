# Mirror Self — Architecture Decision Records

项目定位：数字认知孪生，不只是可搜索的私人文档库。

| ID | 决策 | 状态 | 说明 |
|---|---|---|---|
| ADR-001 | 三渠道认识用户 | Accepted | 历史导入 + AI 主动访谈 + 日常持续学习；V0.1 实现历史聊天导入入口 |
| ADR-002 | L0/L1/L2 记忆分层 | Accepted | 原始资料、语义记忆、认知判断分层；L1/L2 有来源证据 |
| ADR-003 | 分级自主写入 | Accepted | 明确非敏感声明可自动记录；敏感/AI 推断须审核 |
| ADR-004 | 本地+云端混合 | Accepted | 个人数据在自己数据库；云端调用须授权、最小化、审计；V0.1 关闭外发 |
| ADR-005 | 九维认知模型 | Accepted | EXPERIENCE, KNOWLEDGE, COGNITION, PERSONALITY, VALUES, EMOTION, RELATIONSHIPS, MOTIVATION, SELF_PERCEPTION |
| ADR-006 | 可授权数字分身 | Accepted | 可以受限分享经过审核的资料；代表本人对外发布/承诺须本人确认；V0.1 不开放共享 |
| ADR-007 | 模块化单体 | Proposed implementation | Java21 + Spring Boot 4.0.8 + PostgreSQL/pgvector + Flyway；避免过早微服务 |
| ADR-008 | 完全证据驱动 | Proposed implementation | AI 说过的话不是用户事实；所有模型推断保留候选状态与来源 |
| ADR-009 | 默认 fail closed | Proposed implementation | 无授权的模型外发不实现、不启用；API 默认需要 Owner Token |

> ADR-007～009 为工程实现决策，可在技术验证后修订；ADR-001～006 为已与用户确认的产品决策。

## 已明确不做的事（本阶段）

- 不训练自有基础模型，不做自动人格定性。
- 不将用户某一次情绪表达当作永久人格。
- 不把历史 assistant 答案直接写成用户事实。
- 不部署公网可访问的、无独立身份认证的数字分身。
- 不使用外部模型生成未经核实的‘记忆事实’。
