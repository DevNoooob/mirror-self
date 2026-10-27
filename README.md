# Mirror Self / 镜像自我 — V0.2.1 记忆质量评估

> **第一个可运行的记忆垂直切片**：保存 ChatGPT 原始消息 → 规则或 LLM 多轮提取候选记忆 → 保存 L0 证据关联 → 分级审核 → 本地检索 / 摘录式回答。

这是个人数字认知孪生系统的后端 **M1/M2 起步工程**，不是已经能够完整模拟你人格的 AI 产品。

## 运行环境

- Mac：IntelliJ IDEA / Codex / Git / Java 21 / Maven 开发；Docker CLI / Compose 与 SSH 远程管理。
- Ubuntu VMware 虚拟机：唯一正式运行环境，Docker Engine 构建并运行 API、PostgreSQL + pgvector；正式数据及 Volume 均在 Ubuntu。
- GitHub Actions：隔离 Docker 测试环境，不承担正式部署；Mac 不再使用 Docker Desktop 运行本项目服务。
- PostgreSQL 17 + pgvector 0.8.6、Spring Boot 4.0.8、Spring AI 2.0.1（实际 OpenAI-compatible 模型适配）。
- 默认离线处理个人资料：`RULE`，不会调用外部 LLM；显式切换模型模式并授权后才调用。
- 本版 API 只支持单个用户，不支持第三方授权访问。所有非健康检查 API 都要求访问令牌。

## Mac → Ubuntu 远程部署入口

长期开发、测试和部署规则见根目录 [AGENTS.md](AGENTS.md)，完整步骤见 [远程部署与环境指南](docs/DEPLOYMENT.md)。没有明确部署要求时，不更新正式服务，不启动 Mac Docker Desktop。

从 **Mac 当前仓库根目录**只读确认目标；项目约定默认 Context 为 `mirror-ubuntu`，仍需每次现查并显式指定：

```bash
docker context show
docker --context mirror-ubuntu compose ps
```

仅在已明确授权部署、核对 Engine/现有项目/配置并完成必要备份后：

```bash
docker --context mirror-ubuntu compose up --build -d
```

Compose 使用 **Mac 的源码与现有 `.env`** 作为输入，实际镜像构建与容器运行在 **Ubuntu Engine**。不要假设 Ubuntu 有相同目录或 `.env`；不要覆盖现有配置、重置数据库或删除 Volume。远程不可用时报告原因，不自动回退到 Mac 本地 Docker。

API 8080 和数据库 5432 只绑定 **Ubuntu 的 `127.0.0.1`**。Mac 访问 API 时，先保持 SSH 隧道运行，再在另一终端检查健康：

```bash
ssh -N -o ExitOnForwardFailure=yes \
  -L 127.0.0.1:8080:127.0.0.1:8080 Ubuntu用户名@VM_IP
# 另一 Mac 终端：
curl http://127.0.0.1:8080/api/v1/health
```

不要为调试开放校园网/公网端口或擅自修改防火墙。隧道端口冲突、测试隔离、备份与迁移要求详见 [部署指南](docs/DEPLOYMENT.md)。

## 第一个演示：导入 → 记忆 → 查询

```bash
# 在已授权的 Ubuntu 部署完成、Mac 配置就绪且 SSH API 隧道保持运行后，从 Mac 执行：
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

Mac 使用 Java 21 / Maven 运行普通单元测试和纯领域验证，先确认 `mvn -v`：

```bash
mvn -v
mvn test                       # 单元测试，含本机模拟 HTTP 模型服务
bash scripts/smoke-domain.sh   # 无 Maven 也能运行的 JDK21 纯领域验证
```

PostgreSQL/pgvector 集成测试优先使用 [GitHub Actions](.github/workflows/memory-quality.yml)。以下完整测试仅在 **CI 或已确认 Engine 与隔离配置的 Ubuntu 测试进程**执行：

```bash
MIRROR_RUN_LIVE_LLM=false MIRROR_EVALUATION_LIVE_ENABLED=false mvn verify
```

Testcontainers 不一定继承 CLI 的 `mirror-ubuntu` Context，不要直接假定 Mac Maven 已连上远程 Engine，也不要为测试启动 Docker Desktop。环境约定见 [部署指南](docs/DEPLOYMENT.md#测试与-cicd)。

集成测试启用了 `disabledWithoutDocker=true`，没有 Docker 时会跳过；必须分别报告通过、失败、错误与跳过，不能把跳过误认为 PostgreSQL 测试通过。

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
当前使用 Mac 远程 Compose 部署时，模型配置来自 Mac 进程环境或项目现有 `.env`，由 Compose 传入 Ubuntu 容器；不代表 Ubuntu 有同一文件。修改配置不会自动更新正在运行的容器，应用配置变更仍需明确部署授权。

## V0.2.1 记忆质量评估

集成评估优先在 GitHub Actions 运行；手动运行需先确认 Ubuntu 测试进程及隔离 Engine，不能仅依据 Mac CLI Context。

新增 66 个虚构多轮案例、独立参考标注与固定 Mock 快照、分阶段评分、JSON/Markdown 报告及 Baseline 回归比较。默认离线，不需要 API Key。规则提取器可以在同一测试集上独立实测；真实模型评估需显式开启并审核数据摘要。

```bash
bash scripts/evaluate-memory.sh offline
bash scripts/evaluate-memory.sh rules
```

报告写入 `target/evaluation/`。Mock 分数不代表真实 LLM 质量；当前工程参考答案仍待独立人工审核。使用方式、Live 授权及隔离测试要求见 [评估指南](evaluation/README.md)，指标定义与边界见 [V0.2.1 设计](docs/V0.2.1_MEMORY_EVALUATION.md)。现有 API、业务数据库结构与隐私授权保持兼容。

## 设计文档

- [长期开发、测试与部署约定](AGENTS.md)
- [Mac → Ubuntu 远程部署与环境指南](docs/DEPLOYMENT.md)
- [V0.2.1 评估设计、评分定义与 Baseline](docs/V0.2.1_MEMORY_EVALUATION.md)
- [V0.2 模块设计、配置与 API 使用](docs/V0.2_MEMORY_ENGINE.md)
- [本次验证记录](docs/VERIFICATION.md)
- [架构决策 ADR](docs/ADR.md)
- [数据库设计 Schema](docs/DATABASE.md)
- [记忆处理与状态机](docs/MEMORY_PIPELINE.md)
- [Memory Domain Model](docs/DOMAIN.md)
- [REST API 契约](docs/API.md)
- [隐私、安全与已知边界](docs/SECURITY.md)

本版由单用户、私有部署开始；对外共享相关表只是未来架构占位，不代表已经具备安全的对外分享能力。
