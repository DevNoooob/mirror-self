# Mac 开发与 Ubuntu 远程运行

本项目已迁移至 VMware 内的 Ubuntu 虚拟机。**Mac 是开发端，Ubuntu 是唯一正式运行环境。** 根目录 [AGENTS.md](../AGENTS.md) 将本文约定作为后续任务的长期指令。

## 环境分工

| 环境 | 职责 | 数据与容器 |
|---|---|---|
| Mac | IntelliJ IDEA / Codex / Git / Java 21 / Maven 开发，SSH 与 Docker CLI / Compose 人工管理 | 保存源码和本机配置输入；不运行本项目正式服务，不需要启动 Docker Desktop |
| Ubuntu VMware 虚拟机 | Docker Engine / Compose 构建镜像、运行和部署服务；已安装 Git、SSH | API、PostgreSQL17 + pgvector、正式持久化 Volume；已有数据和 Flyway V1/V2 |
| GitHub Actions | 代码质量、单元测试、独立 PostgreSQL/pgvector 集成测试及离线评估 | 临时测试环境，不是正式部署目标，不接入业务库或正式 Volume |

历史文档中的 Mac Docker Desktop 验证只记录当时事实，不是现行操作步骤。没有明确部署要求时，只做任务授权的开发或检查，不更新正式服务。

## 从 Mac 确认远程目标

以下命令在 **Mac 的 mirror-self 仓库根目录**执行。项目约定默认 Context 为 `mirror-ubuntu`，但当前会话可能不同，必须现查：

```bash
docker context show
docker context inspect mirror-ubuntu --format '{{.Name}} {{.Endpoints.docker.Host}}'
docker --context mirror-ubuntu info --format '{{.Name}} {{.OperatingSystem}}'
docker --context mirror-ubuntu compose ps
```

确认 Context endpoint、SSH 身份与预期 Ubuntu Docker Engine 一致，且 Compose 中显示的是已有服务。使用者需自行替换下文 `Ubuntu用户名@VM_IP`；不要把 Docker Context 名称当作已配置好的 SSH 主机别名。

若 Context 不存在、SSH 权限不足、VM/SSH/Engine 不可达，报告实际原因，停止依赖远程环境的操作；不要自动改用 `default` / `desktop-linux`、启动 Docker Desktop、创建猜测的 Context 或切换其他服务器。本机只有 Docker CLI 和 Compose 客户端也可以管理远程 Engine。

沿用已有 Compose 项目名，包括现有 `COMPOSE_PROJECT_NAME` 或 `-p` 约定。若 `compose ps` 为空，应先用只读检查核对已有项目（如 `docker --context mirror-ubuntu compose ls`），不要直接创建一套新的空库或猜测原数据已丢失。

文档维护任务无需执行这些远程连接检查；只读查看本机 Context 元数据后，应明确说明没有验证 Ubuntu 在线状态。本次文档更新不构成部署或连接成功的证据。

## Compose 的源码、配置和卷在哪台机器

当前 `compose.yaml` 使用 `build: .`。从 Mac 执行远程 Compose 时：

1. Mac Compose 客户端读取当前仓库的 `compose.yaml`、Dockerfile、源码，以及 Mac 项目目录中的现有 `.env`（并按 Compose 规则处理当前进程环境变量）。
2. Docker 客户端将构建上下文传给 Ubuntu Docker Engine；镜像构建、缓存、容器及正式命名 Volume 均在 Ubuntu。
3. `.env` 是本机配置输入，不代表该文件已复制到 Ubuntu；构建上下文传输也不等于在 Ubuntu 创建相同源码目录。

不要假设 Ubuntu 有 `/Users/...` 路径、相同 Git checkout、Maven 缓存或 `.env`。普通远程 `docker run -v` / bind mount 解析的是 **Ubuntu 宿主路径**，不会把 Mac 文件自动传过去；不要把历史 Mac Maven 容器挂载命令简单改成 `--context mirror-ubuntu`。

已有 `.env` 不得覆盖。此处不提供重新初始化配置或数据的命令。若缺少配置，先确认授权的配置来源与缺失项；不要打印完整 `.env`、含密钥的 Compose 展开结果或容器环境。需要本机校验 Compose 语法及必填变量时可用 `docker --context mirror-ubuntu compose config --quiet`，这不验证远程 Engine、服务健康或数据库迁移兼容性。

## 已获明确授权时部署

只有用户明确要求部署并确认目标和范围后，才执行本节。先核对 Context、Engine、现有项目名、配置与待部署代码；涉及重要数据库变更时，还必须完成下节备份和迁移检查。

从 **Mac 仓库根目录**发起，实际在 **Ubuntu Engine** 构建和运行：

```bash
docker --context mirror-ubuntu compose up --build -d
docker --context mirror-ubuntu compose ps
docker --context mirror-ubuntu compose logs -f api
```

显式 `--context mirror-ubuntu` 用于避免当前默认 Context 漂移。日志可能包含业务信息，应只检查所需范围，不在报告中复制私人原文、令牌或完整 provider 异常。`logs -f` 持续跟随，按 Ctrl-C 仅退出日志查看。

部署后检查容器健康、API 健康和已授权范围内的迁移结果，汇报 Mac 发起了什么、Ubuntu 执行了什么。不要为了“验证部署”导入真实聊天、清理旧记忆或做其他未经授权的业务写入。

## 数据保护与迁移

Ubuntu PostgreSQL 已含正式持久化数据，`pgdata` 是 Compose 中的卷逻辑名，实际卷名还受现有项目名影响。不得因构建或测试失败更换项目名、删除卷或重置数据库。

未经针对具体对象和风险的明确授权，禁止：

- `docker compose down -v`、删除业务数据库 Volume、`docker volume prune`。
- 清空/重新初始化 PostgreSQL、执行破坏性业务 SQL。
- 覆盖已有 `.env`、提交真实 API Key、私人聊天或数据库备份。

真实密钥和个人数据不应进入 Git。普通部署、测试或开发请求不自动授权上述操作。

Schema 变更必须新增 Flyway 迁移；已执行的 V1/V2 等历史文件不能修改。重要数据库变更的部署需先：

1. 核对目标 Engine、项目、数据库与当前迁移版本。
2. 审阅新增迁移及新旧应用兼容性，明确失败后的恢复方案。
3. 使用受保护位置生成业务备份，校验备份可读并确认恢复步骤；备份不进入 Git、CI 构建产物或日志。
4. 在明确授权范围内执行部署，并检查迁移结果。镜像回退不等于数据库回退，不能自动运行破坏性反向迁移。

本文不执行或提供默认清库流程。备份、迁移和恢复属于独立授权的运维操作。

## Mac 通过 SSH 访问 API

Compose 将宿主机端口发布在 **Ubuntu 的 `127.0.0.1:8080` 和 `127.0.0.1:5432`**。Mac 的 `127.0.0.1` 需要通过隧道连接过去，不能直接将其解释为 Ubuntu。

在一个 Mac 终端保持 SSH 会话：

```bash
ssh -N -o ExitOnForwardFailure=yes \
  -L 127.0.0.1:8080:127.0.0.1:8080 Ubuntu用户名@VM_IP
```

另一个 Mac 终端访问：

```bash
curl http://127.0.0.1:8080/api/v1/health
```

8080 已占用时，可以仅改 Mac 端口：

```bash
ssh -N -o ExitOnForwardFailure=yes \
  -L 127.0.0.1:18080:127.0.0.1:8080 Ubuntu用户名@VM_IP
# 另一 Mac 终端：
curl http://127.0.0.1:18080/api/v1/health
```

其他 API 继续携带现有 `X-Mirror-Token`。使用 18080 时同步调整客户端 URL，无需修改 Compose、Ubuntu 端口或防火墙。

Compose 中 API 容器内部监听 `0.0.0.0` 与 Ubuntu 宿主机端口绑定是不同层次；当前宿主机仍仅发布 loopback。不得为调试将 API 8080 或 PostgreSQL 5432 暴露到校园网/公网，不擅自修改防火墙、开放端口或暴露 Docker Engine API。数据库管理连接如确有需要，也应另行授权使用受控 SSH 隧道。

## 测试与 CI/CD

### Mac 单元测试

先确认 Maven 实际使用 Java 21，再运行普通单元测试或纯领域检查：

```bash
mvn -v
mvn test
bash scripts/smoke-domain.sh
```

若缺少 Java 21，说明本机限制，使用已配置的 CI 验证或经授权准备 JDK；不要自动启动 Docker Desktop 代替本机环境。

### PostgreSQL / pgvector 集成与评估

优先使用现有 [GitHub Actions 工作流](../.github/workflows/memory-quality.yml)。runner 的 Docker 与 Ubuntu 正式 Engine 独立，工作流先检查 Docker，再用 Java 21 执行 `mvn verify`，明确关闭两个 Live 模型入口并保存虚构数据评估报告。

`docker context show` 只反映 Docker CLI 选择；**Testcontainers 不一定读取该 Context**。不能仅因 Mac CLI 能运行远程 Compose，就断言 Mac Maven 已连接 Ubuntu。不要自动开放 Docker TCP API 或转接正式 socket 来绕过访问限制。

如确需在 Ubuntu 执行集成测试，须在已授权任务中单独确认该机测试源码与 Java 21 / Maven、测试进程对 Docker Engine 的访问、容器/数据库隔离。源码路径可与 Mac 不同；不要假设远程存在相同目录或 `.env`。所有测试仅使用临时独立数据库、虚构数据，禁止使用正式连接或挂载 `pgdata`。

仅在 **CI 或上述已确认隔离的 Ubuntu 测试进程**中运行：

```bash
MIRROR_RUN_LIVE_LLM=false MIRROR_EVALUATION_LIVE_ENABLED=false mvn verify
bash scripts/evaluate-memory.sh offline
bash scripts/evaluate-memory.sh rules
```

`disabledWithoutDocker=true` 可能使集成测试跳过。结果必须分别报告通过、失败、错误、跳过，不把跳过算作通过。真实付费模型测试需要另外明确启用及数据授权，不能顺带随日常 CI 调用；详见 [评估指南](../evaluation/README.md)。

普通 PR 测试仅负责验证，不能变成生产部署或接入正式数据库。未来自动部署必须采用独立、安全、受控的授权流程。没有明确部署需求时，Ubuntu 正式服务保持原状。

## 任务结束记录

每次涉及环境的任务应记录：操作发生在哪台机器、实际 Context/目标 Engine、授权范围、验证与跳过情况、连接失败原因及是否改变服务/数据。没有实际连接 Ubuntu 时明确写“未验证远程状态”，不能沿用历史记录宣称当前服务正常。
