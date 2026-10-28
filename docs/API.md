# Memory API V1 契约

**Base URL**：`http://127.0.0.1:8080/api/v1`

正式 API 运行在 Ubuntu，宿主机仅绑定 loopback。Mac 使用此地址前需保持 [SSH 端口转发](DEPLOYMENT.md#mac-通过-ssh-访问-api) 运行；如 Mac 转发端口选择 18080，同步调整 Base URL，不改变 Ubuntu 端口或防火墙。

认证：除 `/health` 外的每个请求均须带 `X-Mirror-Token: <MIRROR_OWNER_TOKEN>`。这是单用户开发版本的令牌方案，不是多用户 OAuth。所有 API 始终绑定后端 `MIRROR_OWNER_ID`，客户端不能覆盖 owner。

| Method | Path | 功能 | 输入/备注 |
|---|---|---|---|
| GET | `/health` | 健康状态 | 不含隐私数据 |
| POST | `/import/chatgpt` | 导入 ChatGPT 历史消息 | multipart `file=conversations.json`，最大 50 MB |
| GET | `/sources/{id}` | 查看完整原始消息 | owner only，含原文，敏感 |
| DELETE | `/sources/{id}` | 删除来源并清理无证据衍生记忆 | 幂等性目前通过 400 not found 表示 |
| POST | `/memories` | 手动添加长期记忆 | `type`, `text`, `sensitivity`, 可选 `occurredAt` |
| GET | `/memories` | 查看长期记忆 | 可选 `status`, `limit`（1–200） |
| POST | `/memories/{id}/review` | 审核候选记忆 | `{"decision":"APPROVE"}` 或 `REJECT` |
| DELETE | `/memories/{id}` | 删除指定记忆与关联 evidence | 需 Owner Token |
| GET | `/retrieval` | 检索 ACTIVE 记忆 | `query`, 可选 `limit`（1–30） |
| POST | `/ask` | 生成本地摘录式回答 | `{"question":"我学过 Java 吗"}`；返回 `mode=EXTRACTIVE_NO_LLM` |
| GET | `/identity/claims` | 查看认知判断 | 可选 `status` |
| POST | `/identity/claims/{id}/review` | 审核候选认知 | `{"decision":"APPROVE"}` 或 `REJECT` |

## 示例结果（只说明形状）

导入：

```json
{"jobId":"...","importedSources":3,"existingSources":0,"newMemories":3}
```

新增记忆：

```json
{"id":"...","inserted":true,"status":"ACTIVE"}
```

摘录式回答：

```json
{
  "mode": "EXTRACTIVE_NO_LLM",
  "answer": "找到以下可能相关的历史记忆（请根据原始证据核对）：...",
  "evidence": [
    {"id":"...", "type":"KNOWLEDGE", "origin":"USER_EXPLICIT", "status":"ACTIVE",
     "evidence":[{"sourceId":"...","locator":"demo-conversation-001:msg-user-001","excerpt":"我正在学习 Java 的并发编程"}]}
  ]
}
```

**版本边界**：没有开放的第三方分享、聊天会话持久化或消息发送 API；它们属于后续版本。API 不接受调用者任意指定 `owner_id`；未来加多用户时必须先引入标准认证、租户隔离和表级访问策略。


## V0.2 扩展

新增来源元数据列表、提取任务执行/查询、逐来源模型出站授权/撤销。
原有导入计数字段保留，新增 extractionJobIds；记忆读取追加 temporalScope、extractorVersion、modelId、conflicts。
完整请求、状态与授权步骤见 [V0.2 API 使用说明](V0.2_MEMORY_ENGINE.md#api-使用)。

## V0.2.2-M2-B 关系候选

沿用 X-Mirror-Token 和 owner 配置，新增 POST `/api/v1/consolidation/jobs`、GET `/api/v1/consolidation/jobs/{id}`、GET `/api/v1/consolidation/candidates`、GET `/api/v1/consolidation/candidates/{id}`、POST `/api/v1/consolidation/candidates/{id}/review`。审核体需 decision 与 expectedVersion，返回 approvedEffect=RELATION_REVIEW_ONLY；不会修改原记忆。参数、错误及完整响应字段见 [M2-B API](V0.2.2-M2B_CONSOLIDATION.md#api)。
