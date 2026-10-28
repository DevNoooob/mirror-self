# Database Schema V1

来源：`src/main/resources/db/migration/V1__memory_foundation.sql` 是唯一真实 DDL，本文是设计说明。

## 1. 数据层次

```text
L0 source_record (raw_message JSONB, text_content, source_locator, role, occurred_at)
            │
            │ source_record.id → memory_evidence.source_id
            ▼
L1 memory_item ← memory_evidence → source_record
            │
            │ identity_evidence.memory_id
            ▼
L2 identity_claim ← identity_evidence
```

**事件发生时间 `occurred_at`、写入系统时间 `imported_at` 与记忆有效期间 `valid_from/valid_to` 必须分开。** 迟到的历史数据不是‘今天发生’的数据。

## 2. 核心表

- `source_record`: 每条消息节点的完整 JSON（包含可用的 parent/children）、文本提取、来源定位符、SHA256、role（user/assistant/system/tool/unknown）、事件时间、私密等级。`UNIQUE(owner_id,source_system,source_locator,content_hash)` 支持重复导入去重。更改同一定位符的消息保存成另一个版本。
- `memory_item`: `FACT|EVENT|KNOWLEDGE|OPINION|GOAL|THOUGHT_PROCESS`；来源性质 `USER_EXPLICIT|OBSERVED|MODEL_INFERRED`；状态 `CANDIDATE|PENDING_REVIEW|ACTIVE|REJECTED|SUPERSEDED|RETRACTED`。目前创建直接进入 ACTIVE 或 PENDING_REVIEW；候选中间态是逻辑状态，不在当前接口持久化。
- `memory_evidence`: 关联多条原始消息与一条提取记忆。相同内容可出现不同证据；删除全部证据后删除相关 L1 记忆。
- `memory_relation`: L1 关系 RELATED_TO/SUPPORTS/CONTRADICTS/EVOLVED_FROM，尚未建立自动管理流程。
- `identity_claim` / `identity_evidence`: L2 九维认知判断和来自 L1 的证据。模型推断必须关联证据且进入 PENDING_REVIEW。
- `memory_embedding`: 固定 `vector(1024)` 的占位结构，计划可用 `bge-m3` 等 1024 维模型；没有生成任务，现阶段为空。
- `share_profile` / `share_grant`: 对外共享的未来数据结构；**不提供任何可用的共享 API**。
- `ingestion_job`: 本次 ChatGPT 文件导入的进度/状态记录。
- `audit_event`: 记录创建、审核、删除事件，不保存原始敏感正文。

## 3. 索引与约束

- PostgreSQL `pg_trgm` GIN 索引用于未来文本筛选优化。当前检索为简单关键词 `ILIKE`，中文两字片段召回质量有限。
- pgvector HNSW 索引用于未来 1024 维近似向量检索；未接入 Embedding 模型。
- 必要的 `owner_id` 与 `status` 条件必须写入每次查询 SQL，而不是只靠 prompt。
- 删除 L0 时 `memory_evidence` 会自动级联，再删除失去全部证据的 L1 记忆；对 L2 推断判断采取保守清理策略。

## 4. 已知局限 / 未来迁移

- 这是**单所有者应用**，虽每行有 `owner_id`，尚未实施强隔离的复合外键 / PostgreSQL RLS，不可直接作为 SaaS 多租户系统使用。
- `memory_item.dedupe_key` 按类型+归一化文本哈希去重，无法识别语义等价或观点冲突。重复历史观点会合并证据；后续需要事件级/时间级语义去重策略。
- 复杂人格建模还需要独立的认知修订事件、版本线、反证和时间化演化算法。
- 正式 PostgreSQL 数据及 Volume 位于 Ubuntu，需要独立备份、校验与恢复演练；不能为测试清理或重建正式库。重要迁移先备份并检查兼容性，按 [远程部署指南](DEPLOYMENT.md#数据保护与迁移) 执行。删除后外部离线备份中的旧版本需遵循备份保留策略再清除。


## V0.2 增量

`V2__intelligent_extraction.sql` 仅新增任务、任务来源及云端授权表，为 memory_item 追加提取指纹、
时间语义和提取器/模型/任务追踪字段。复用 memory_relation 的 CONTRADICTS，不改写 V1 或旧记录内容。
升级测试先在临时数据库执行 V1、写入虚构旧来源/记忆/证据，再执行 V2 并核对原数据保留。

## V0.2.2-M1 增量（V3）

`V3__embedding_retrieval.sql` 复用 `memory_embedding.vector_value vector(1024)`，追加 `model_version`、`memory_revision`，将目标唯一键扩为 owner + target + model + version；历史行保留并标记 `legacy`。`memory_item.embedding_revision` 用作并发写回检查。新增 `embedding_job` 仅存状态、计数和固定错误码，无输入正文。

数据库触发器在 L1 内容/状态/隐私/类型/时间语义变更、证据增加/修改/删除、L0 正文/角色/隐私/owner 变更时递增修订并删除该记忆所有版本向量。外键继续处理记忆/来源删除；删除部分证据也使旧向量失效。新索引写入先锁定 L1 行，并重新核对 owner、修订和检索可见性。生成期间发生撤回或证据变化的结果不能写回。

关键词、向量、索引选择和最终证据装载都使用 `RetrievalVisibility.SQL`：ACTIVE + NORMAL + 至少一条证据 + 所有证据同 owner、PRIVATE、user、逐字引用仍存在。敏感记忆即使审核通过也不进入本阶段通用检索；后续若增加显式授权查询需单独设计。原 owner 记忆管理列表仍可查看待审核/敏感内容。

向量检索用 `<=>` 计算余弦距离，MATERIALIZED 预过滤后精确排序；M1 保留原 HNSW 索引但暂不使用近似检索，避免小候选集过滤后的召回损失。未来大库优化需先测量，不能移除权限过滤。V3 只在隔离测试库验证，正式库迁移需备份与独立部署授权。
