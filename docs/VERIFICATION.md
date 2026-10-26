# V0.2 验证记录（2026-10-26，Asia/Shanghai）

对原仓库 `/Users/devnoob/project/mirror-self` 的最终代码执行验证。

- 运行时：Java 21.0.10（maven:3.9.13-eclipse-temurin-21），Spring Boot 4.0.8，Spring AI 2.0.1。
- 数据库：Testcontainers 临时 PostgreSQL 17.11 + pgvector 0.8.6，与已有服务/卷隔离。
- `mvn verify`：总计 48 项，47 项通过，0 失败、0 错误，1 项跳过。
- 唯一跳过项为 LiveModelIT：没有显式设置 MIRROR_RUN_LIVE_LLM=true，未发起付费/云端模型调用。
- 既有 JDK21 `scripts/smoke-domain.sh`：通过。
- `python3 scripts/validate_structure.py`：通过。
- V1 迁移 SHA-256 保持 `88cdc7c531d6f14b016da1d69e2f34dc683623f223792dbc42b87b10f8ed9daf`。

| 测试套件 | 项数 | 失败 | 错误 | 跳过 |
|---|---:|---:|---:|---:|
| ChatGptDecoderTest | 1 | 0 | 0 | 0 |
| ConversationBranchesTest | 1 | 0 | 0 | 0 |
| LlmMemoryExtractorTest | 12 | 0 | 0 | 0 |
| MemoryPolicyTest | 3 | 0 | 0 | 0 |
| MemoryValidatorTest | 5 | 0 | 0 | 0 |
| RuleBasedExtractorTest | 2 | 0 | 0 | 0 |
| SearchTermsTest | 2 | 0 | 0 | 0 |
| SourceConversationsTest | 2 | 0 | 0 | 0 |
| ModelAuthorizationTest | 2 | 0 | 0 | 0 |
| SpringAiModelGatewayTest | 3 | 0 | 0 | 0 |
| CloudAuthorizationIT | 3 | 0 | 0 | 0 |
| LiveModelIT | 1 | 0 | 0 | 1 |
| LlmMemoryIT | 8 | 0 | 0 | 0 |
| PostgresMemoryIT | 3 | 0 | 0 | 0 |

## 验证内容

完整集成链路：虚构多轮对话导入 → Mock LLM（保留 assistant 上下文）→ 真实原文与语义验证流程 → PostgreSQL 持久化 → 检索 user 原文证据。
另验证候选审核、推断改写去重、并发重复执行、批次回滚、历史观点、敏感候选、来源伪造、助手回答误归因、非法 JSON、超时、语义拒绝。

SpringAiModelGatewayTest 通过真实 Spring AI HTTP 适配器连接本机模拟服务，验证请求协议、无 tools、store=false、实际请求超时和非敏感错误处理。
模型提取质量测试使用 Mock；HTTP 模拟服务不是实际训练模型，不能替代选定真实模型的质量验收。

云端授权测试覆盖全局关闭、无 grant、来源不匹配、提供商/模型变更、到期、撤销、跨 owner、未授权敏感内容和 SECRET 外发拒绝。
V1→V2 升级测试先写入虚构旧来源、旧记忆、旧证据，再迁移并核对原数据保留。
新增端点鉴权使用实际随机端口的 Spring Boot HTTP 服务，未携带 Token 时返回401。

## 现有数据保护

初始与再次只读核对：原运行库均为3条来源、3条记忆、0条L2判断；仅有V1迁移。
没有重启现有实例，没有修改 `.env`，没有在原数据库运行迁移或破坏性重置。
原仓库在任务开始时没有任何提交，全部源码未跟踪；本次未创建提交、未整理或覆盖无关文件。

## Compose 验证

已通过原仓库 Dockerfile 的源码镜像构建及独立 Compose 启动（项目 mirror-self-v02-validation）。
构建时发现原 go-offline 预下载步骤长时间无进展，已改为使用 BuildKit Maven 缓存直接 package；修正后的冷构建成功。

实际 HTTP 验证：

- 健康检查200；未认证读取来源401。
- 首次导入3条来源、生成3条记忆。
- 检索返回 user 原文证据；/ask 保持 EXTRACTIVE_NO_LLM。
- 相同文件再次导入 importedSources=0、newMemories=0。
- 来源元数据列表不包含原文；显式重提取 COMPLETE、新增0条，版本 rule-0.2.0。
- 默认规则模式创建云端授权返回403。
- 认知自述进入 PENDING_REVIEW，原有审核接口可批准；手动删除后仍保留最初3条测试记忆。
- 测试数据库 flyway_schema_history 包含成功的V1和V2。
- 原服务仍运行，原库再次只读核对仍为3条来源、3条记忆、0条L2判断。

验证实例使用新建命名数据卷和随机 localhost 端口。验收后仅清理本次验证项目，未操作原 pgdata 卷。

## 复现

```bash
mvn test
mvn verify
python3 scripts/validate_structure.py
bash scripts/smoke-domain.sh
```

本次使用 Docker 的 Java21 Maven 容器挂载原源码及 Maven缓存，提供 Docker socket 和 TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal。
默认测试的虚构数据只进入临时数据库和本地模拟服务，未导出真实私人历史。
真实模型验收需显式设置相应模型配置及 MIRROR_RUN_LIVE_LLM=true，见 V0.2_MEMORY_ENGINE.md。
