# V0.2 验证记录（2026-10-26，Asia/Shanghai）

> 历史验证记录：下述容器构建、挂载与测试结果仅描述当时环境，不代表现行部署方式或本次已重新执行。当前统一遵循 [Mac → Ubuntu 远程部署与测试约定](DEPLOYMENT.md)，禁止据此自动启动 Mac Docker Desktop 或更改正式业务库。

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

## 验证命令与现行执行环境

Mac 可运行普通单元测试及纯领域检查；`mvn verify` 只在 CI 或已确认隔离配置与 Engine 访问的 Ubuntu 测试进程执行。Testcontainers 不一定继承 Mac CLI Context。

```bash
mvn test
mvn verify
python3 scripts/validate_structure.py
bash scripts/smoke-domain.sh
```

历史验证当时使用 Docker 的 Java21 Maven 容器挂载原源码及 Maven缓存，提供 Docker socket 和 TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal。
默认测试的虚构数据只进入临时数据库和本地模拟服务，未导出真实私人历史。
真实模型验收需显式设置相应模型配置及 MIRROR_RUN_LIVE_LLM=true，见 V0.2_MEMORY_ENGINE.md。

## V0.2.2-M2-B：隔离候选整合验收

[CI 40732403937](https://github.com/DevNoooob/mirror-self/actions/runs/40732403937)，head `68af38927b0ca28031dcd57119c29859bbf8cc37`：Java21单元130通过，集成50通过/3个真实模型入口跳过，Python预检15通过。Mac离线单元实际JDK27，不能冒称Mac Java21。V4只在CI临时数据库执行，V1→V4虚构旧数据保留验证通过。

[31对离线观察](../evaluation/runs/consolidation-offline-v1/analysis.md)：Precision100%、Recall88.89%、错误SAME_AS0/3、证据17/17、审核策略8/8。所有测试向量故意相同，0真实模型调用，不是BGE质量或正式基线。未知跨语言同义表达CC16被命题规则漏检，参考不改。首轮Gson报告日期序列化错误已修复，完整重跑通过。

v3已按批准范围创建45查询，全部最终人工参考仍待审、grade2另审。未改v2/首次真实报告、未连接Ubuntu业务库、未部署/合并/校时。

## PR #11 合并前修复验收

[CI 41036371289](https://github.com/DevNoooob/mirror-self/actions/runs/41036371289)，head `7b7dba4e7a6406a6fd8aab53dd7db64f71c613a6`：Java21单元131通过，隔离集成58通过/3个真实模型入口跳过，Python15通过；新增资格拥挤/关系冲突回归8项通过。Mac实际JDK27只运行离线单元，不冒称Mac Java21。

[独立新版报告](../evaluation/runs/consolidation-offline-v1-pr11-review-fixes/analysis.md)保留固定31对的原参考；P100%/R88.89%、错误SAME_AS0/3，与首次观察相同。旧报告及V4、v2/v3 SHA逐字节校验通过。没有新真实模型/正式Baseline/Schema迁移或Ubuntu业务库操作。

## M2-QG 开发集与方向保护验收

[CI 41043599934](https://github.com/DevNoooob/mirror-self/actions/runs/41043599934)，source head `c329e4182a8c4bf1d513c89503767eea8d6604e3`：Java21单元136通过，集成61通过/4个真实模型入口跳过，Python21通过。Mac实际JDK27只运行离线单元。V4→V5批准行保留及双向审核保护通过，只在临时库执行。

[DEV完整观察](../evaluation/runs/m2-qg-v1-dev-observation/analysis.md)：KEYWORD候选R@10=83.33%，未达冻结90%门禁；HYBRID相同向量Mock召回100%，分类R=83.33%，数值门禁通过但不代表BGE/留出验收。两模式均P100%/端到端R83.33%/F1 90.91%；错误SAME_AS/无关/owner/隐私/证据错误0。安全零容忍在观察模式也强制；人工审核、留出与本轮真实模型均未完成，V0.3准入false。

全部旧数据/参考/报告及V1–V4按字节保留，未调用真实模型、访问业务库、部署或校时。
