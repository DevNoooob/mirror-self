# Mirror Self / 镜像自我 — V0.2 第一阶段

> **第一个可运行的记忆垂直切片**：保存 ChatGPT 原始消息 → 规则或 LLM 多轮提取候选记忆 → 保存 L0 证据关联 → 分级审核 → 本地检索 / 摘录式回答。

这是个人数字认知孪生系统的后端 **M1/M2 起步工程**，不是已经能够完整模拟你人格的 AI 产品。

## 运行环境

- Java 21、Maven 3.9+（本地运行时），或 Docker + Docker Compose（推荐）。
- PostgreSQL 17 + pgvector 0.8.6、Spring Boot 4.0.8、Spring AI 2.0.1（实际 OpenAI-compatible 模型适配）。
- 默认离线处理个人资料：`RULE`，不会调用外部 LLM；显式切换模型模式并授权后才调用。
- 本版 API 只支持单个用户，不支持第三方授权访问。所有非健康检查 API 都要求访问令牌。

## Docker 一键运行

```bash
# 仅首次安装且 .env 不存在时执行，升级时不要覆盖已有 .env
cp -n .env.example .env
# 修改 .env 中 POSTGRES_PASSWORD 和 MIRROR_OWNER_TOKEN（随机、唯一、至少32字符）
docker compose up --build -d
curl http://127.0.0.1:8080/api/v1/health
```

Docker Compose 只将数据库和 API 发布到 **127.0.0.1**，不对局域网直接暴露服务。切勿直接把开发版部署到公网。

若在 Mac 上已经安装 PostgreSQL，亦可在启动数据库后执行：

```bash
export MIRROR_DB_URL=jdbc:postgresql://127.0.0.1:5432/mirror_self
export MIRROR_DB_USER=mirror
export MIRROR_DB_PASSWORD='你的数据库密码'
export MIRROR_OWNER_TOKEN='至少32字符的独立随机令牌'
mvn spring-boot:run
```

注意：本地方式需自行创建带 pgvector 扩展的数据库，Flyway 会自动执行迁移。

## 第一个演示：导入 → 记忆 → 查询

```bash
# 在已完成 docker compose up 并配置 .env 后运行：
export MIRROR_OWNER_TOKEN='填入和.env一致的令牌'

# 1. 导入虚构的 ChatGPT 样例
curl -sS -X POST http://127.0.0.1:8080/api/v1/import/chatgpt \
  -H "X-Mirror-Token: $MIRROR_OWNER_TOKEN" \
  -F "file=@fixtures/conversations.sample.json"

# 2. 检索已经生成的用户记忆
curl -sS 'http://127.0.0.1:8080/api/v1/retrieval?query=Java&limit=10' \
  -H "X-Mirror-Token: $MIRROR_OWNER_TOKEN"

# 3. 基于记忆生成“摘录式”回答（没有 LLM）
curl -sS -X POST http://127.0.0.1:8080/api/v1/ask \
  -H "X-Mirror-Token: $MIRROR_OWNER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"question":"我学过 Java 吗？"}'

# 4. 查看待审核记忆
curl -sS 'http://127.0.0.1:8080/api/v1/memories?status=PENDING_REVIEW' \
  -H "X-Mirror-Token: $MIRROR_OWNER_TOKEN"
```

导入真实 ChatGPT 历史记录时，请先在官方导出包中找到 `conversations.json`，**仅上传此文件而非 ZIP**。文件不得超过 50 MB；更大的文件需要未来的文件级拆分或批处理入口。保存的 L0 是每个消息节点 JSON（尽量保留父子关系等元数据）的语义等价副本，**不是原始 ZIP 的字节级备份**。对话中的多模态附件目前只保留消息 JSON 中的引用信息，不下载附件本体。

反复上传相同文件会利用来源定位符和 SHA-256 去重，不应创建重复消息或重复记忆。**坏文件在中途失败时可能留下已经提交的消息**；本版本通过幂等导入保证修正后可以安全重试。

### 其他操作

```bash
# 手动创建明确事实；即使是 NORMAL，仍是 owner-private，不是公开数据
curl -sS -X POST http://127.0.0.1:8080/api/v1/memories \
  -H "X-Mirror-Token: $MIRROR_OWNER_TOKEN" -H 'Content-Type: application/json' \
  -d '{"type":"FACT","text":"我目前在学习分布式系统","sensitivity":"NORMAL"}'

# 审核某条 PENDING_REVIEW 记忆（替换为真实 UUID）
curl -sS -X POST http://127.0.0.1:8080/api/v1/memories/UUID/review \
  -H "X-Mirror-Token: $MIRROR_OWNER_TOKEN" -H 'Content-Type: application/json' \
  -d '{"decision":"APPROVE"}'

# 删除一条 L0 来源，自动移除没有其他证据支持的衍生记忆
curl -sS -X DELETE http://127.0.0.1:8080/api/v1/sources/UUID \
  -H "X-Mirror-Token: $MIRROR_OWNER_TOKEN"
```

**删除语义说明：** `DELETE /memories/{id}` 删除 L1 记忆；对于手动创建的记忆，也删除对应的专属 L0 来源。历史聊天来源仍保留（否则会影响原始历史），如果希望彻底清除一段历史记录，请另外删除对应的 `/sources/{id}`。将来重新构建记忆时，仍保存的历史原文可能再次生成相似记忆。

## 当前实现范围

✅ V0.2：多轮 LLM 提取、严格 JSON/原文校验、独立语义验证、逐来源出站授权、批处理、提取任务、确定性去重、基础矛盾标记；PostgreSQL/Flyway；原始节点 JSON 存储；幂等导入；user/assistant 分离；规则提取；证据关联；状态审核；敏感内容启发式阻断；时间字段；owner-only HTTP API；硬删除来源及孤立衍生记忆；中文关键词/子串检索；本地摘录式回答；审计事件；AI 框架边界接口。

🚧 尚未实现：向量生成与混合检索（虽然 Schema 已预留 pgvector）、用户界面、九维画像的自动生成、可信思维模拟、持续后台调度、多种文件格式、公开共享/第三方登录、复杂语义合并、完整的秘密检测和跨版本人格评估。

**敏感内容检测只是简单词表，不是可靠的隐私分类器。请在真实数据导入之前检查文件，避免包含 API Key、密码和不希望存储的内容。** 存储未加应用级字段加密，需使用操作系统磁盘加密、加密备份和严格的文件权限。

## 测试

```bash
mvn test                       # 单元测试，含本地模拟 HTTP 模型服务
mvn verify                     # 单元 + PostgreSQL/pgvector 集成测试，需要 Docker
bash scripts/smoke-domain.sh   # 无 Maven 也能运行的 JDK21 纯领域验证
```

注意：集成测试启用了 `disabledWithoutDocker=true`，没有 Docker 时会跳过；不能把跳过误认为 PostgreSQL 测试通过。

## 启用 V0.2 智能提取

默认无需配置模型，原有导入、检索和 `/ask` 均保持可用；`/ask` 仍为本地摘录式回答。

```bash
export MIRROR_EXTRACTION_MODE=LLM
export MIRROR_LLM_MODEL='你的提供商支持的模型标识'
export MIRROR_LLM_BASE_URL='https://你的模型服务/v1'
# MIRROR_LLM_API_KEY 通过部署环境或安全配置注入，不写进源码或日志
export MIRROR_MODEL_EGRESS_ENABLED=true
```

云端模式导入先保存 L0，提取任务初始会因未授权而 `BLOCKED`。通过 `GET /api/v1/sources`
找到目标对话的来源 ID，为实际要发送的完整上下文（含 assistant）创建短期授权，再调用
`POST /api/v1/extractions`。详细请求及本地模型配置见 [V0.2 使用说明](docs/V0.2_MEMORY_ENGINE.md)。
Spring Boot 本地启动不会自动读取 `.env`；Compose 会读取它的变量。

## 设计文档

- [V0.2 模块设计、配置与 API 使用](docs/V0.2_MEMORY_ENGINE.md)
- [本次验证记录](docs/VERIFICATION.md)
- [架构决策 ADR](docs/ADR.md)
- [数据库设计 Schema](docs/DATABASE.md)
- [记忆处理与状态机](docs/MEMORY_PIPELINE.md)
- [Memory Domain Model](docs/DOMAIN.md)
- [REST API 契约](docs/API.md)
- [隐私、安全与已知边界](docs/SECURITY.md)

本版由单用户、私有部署开始；对外共享相关表只是未来架构占位，不代表已经具备安全的对外分享能力。
