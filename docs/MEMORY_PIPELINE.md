# Memory Processing Pipeline V0.1

## 1. 写入

```text
conversations.json
    └─ ChatGptDecoder（逐个 conversation 解析）
         ├─ L0: 原始 message node JSON、author role、title、时间 → source_record
         │    └─ 来源定位符+JSON哈希去重
         └─ role==user 且是新增记录
              └─ RuleBasedExtractor（只识别极少数明确声明）
                   └─ SensitiveDetector（局部风险词表，不能替代专业分类）
                        └─ MemoryPolicy
                             ├─ USER_EXPLICIT + NORMAL → ACTIVE
                             ├─ MODEL_INFERRED → PENDING_REVIEW
                             └─ SENSITIVE/SECRET → PENDING_REVIEW
                                  └─ memory_item + memory_evidence
```

每条消息 + 所提取的记忆在一次 Spring 数据库事务中提交。批次整体使用 `ingestion_job` 跟踪，并**不**提供跨文件的原子回滚：出错时已写入的消息保留，幂等导入可安全重试。

## 2. 审核状态机

```text
提取候选 → 规则判定 → ACTIVE 或 PENDING_REVIEW
PENDING_REVIEW --APPROVE--> ACTIVE
PENDING_REVIEW --REJECT---> REJECTED
删除来源/记忆 → 关联证据与派生判断清理
```

V0.1 只开放审核 `PENDING_REVIEW`，已确认结论若要修改必须作为新记忆/新声明重新建模，而不是直接覆盖历史内容。

## 3. 读取

```text
Owner Token → API → owner_id + ACTIVE 过滤 → 关键词/中文双字召回
     → 证据 JOIN（原文摘录 + 来源标识）→ 本地 EXTRACTIVE_NO_LLM 答案
```

**关键词检索不是向量 RAG**。`/api/v1/ask` 仅生成带证据的摘录模板，不具备通用开放式推理能力，也没有接入云端模型。

## 4. L2 认知模型

`identity_claim` 可存九维判断；Java `IdentityClaimRepository.propose` 会要求模型推断提供既有且属于 owner 的 ACTIVE L1 evidence，并将其置为 PENDING_REVIEW。自动发现人格特征、反证挖掘、思维模拟尚未实现。

## 5. 下一步建议

1. 为正常 ChatGPT 导出包制作覆盖率更高的解析测试（含部分工具/多模态/编辑版本）。
2. 实现真正的 `CandidateExtractor` 可替换接口与本地 LLM 提取，在用户审核下建立认知候选。
3. 加上独立的中文切词器及本地 Embedding，建立 hybrid search、重排和准确性评测。
4. 引入用户审核 UI 和主动认知提问队列，之后再做数字自我模拟。
