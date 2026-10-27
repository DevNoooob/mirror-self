# Mirror Self 长期工程约定

本文件适用于仓库根目录及所有子目录，是后续开发、测试、部署与运维任务的长期指令。更新时保留已有有效约定；历史验证记录不能替代当前环境检查。具体操作见 [远程部署与环境指南](docs/DEPLOYMENT.md)。

## 1. 环境职责

- **Mac 是开发端，Ubuntu 是唯一正式运行环境。**
- Mac 使用 IntelliJ IDEA、Codex、Git、Java 21 / Maven 编辑和验证源码，通过 SSH、Docker CLI / Compose 管理 Ubuntu。
- Ubuntu 是 VMware 虚拟机，已安装 Docker Engine、Docker Compose、Git、SSH；负责镜像构建、API 与 PostgreSQL17 + pgvector 容器运行。
- 正式业务数据库和持久化 Docker Volume 位于 Ubuntu，已有数据及 Flyway V1/V2 必须保留。
- GitHub Actions 的临时 Docker 仅用于隔离测试，不是正式部署环境。
- 不再使用 Mac Docker Desktop 运行本项目服务；不得为开发或测试自动启动 Docker Desktop。

## 2. 每次操作前识别目标

涉及 Docker、数据库、集成测试、部署或运维时，先识别当前执行端是 Mac、GitHub Actions runner 还是 Ubuntu，检查 Docker Context 与目标 Engine。不得假定 Codex 一定有 `mirror-ubuntu` Context、SSH 凭据或 Ubuntu 访问权限。

部署前从 Mac 仓库根目录检查：

```bash
docker context show
docker context inspect mirror-ubuntu --format '{{.Name}} {{.Endpoints.docker.Host}}'
docker --context mirror-ubuntu info --format '{{.Name}} {{.OperatingSystem}}'
docker --context mirror-ubuntu compose ps
```

项目约定默认 Context 为 `mirror-ubuntu`，但每次都要现查；重要操作仍显式指定 `--context mirror-ubuntu`。Context 缺失、SSH 失败或远程 Engine 不可用时，说明失败原因并停止依赖该环境的操作。**禁止悄悄回退到 Mac Docker、自动启动 Docker Desktop或猜测另一台服务器。**

只更新文档时，可以只读核对执行端与 Context 元数据，不必连接 Engine、运行 Compose 或启动任何服务；必须说明没有验证 Ubuntu 在线状态。

## 3. 远程 Compose 的输入与状态

- 在 Mac 当前仓库根目录执行 Compose，使用 **Mac 的源码、Dockerfile、compose.yaml 和现有 `.env`** 作为构建与配置输入；构建上下文发送到 Ubuntu Engine，镜像构建和容器实际运行在 Ubuntu。
- 不假设 Ubuntu 有相同源码路径或 `.env`，不擅自复制私密配置。远程 `docker run -v` / bind mount 的宿主路径指 Ubuntu 文件系统，不会自动传输 Mac 文件；不能套用 Mac 本地挂载测试命令。
- 保持已有 Compose 项目名和 Volume 映射。若 `compose ps` 为空，先核对现有项目和卷，不能据此初始化一个新业务库。
- 仅在用户明确要求部署且授权目标与范围后执行以下操作；普通开发、测试、修复或文档任务不包含正式更新授权：

```bash
docker --context mirror-ubuntu compose up --build -d
docker --context mirror-ubuntu compose ps
docker --context mirror-ubuntu compose logs -f api
```

不得把 `.env`、完整解析后的 Compose 配置、真实聊天、数据库备份或密钥输出到日志、报告或 Git。

## 4. 开发与测试

- 普通 Java 单元测试可在 Mac 上使用 Java 21 / Maven 运行；先确认 Maven 实际使用的 JDK。
- PostgreSQL / pgvector 集成测试使用独立容器、隔离数据库和虚构数据；优先由 GitHub Actions 执行。
- **Testcontainers 不一定继承 Docker CLI 的 Context。** `docker context show` 正确不等于测试进程连接正确，不能直接把 Mac 的 `mvn verify` 当成已配置的远程集成测试。
- 如需在 Ubuntu 运行 Testcontainers，先确认测试进程能访问该机 Docker Engine，并单独确认测试源码、Java 21 / Maven、连接配置与测试库隔离。禁止连接正式数据库或挂载正式业务卷来通过测试。
- 不删除、重建或修改正式数据库来解决测试失败。Docker 不可用导致的跳过必须报告为跳过，不能声称测试通过。
- 默认单元/集成/CI 测试不调用真实付费模型；保留模型出站授权和安全日志边界。Live 测试需独立显式授权并使用审核过的虚构数据。

## 5. 远程访问与端口

Compose 的 API 8080、PostgreSQL 5432 绑定在 **Ubuntu 的 `127.0.0.1`**，不是 Mac 的 loopback。Mac 访问 API 使用已授权的 SSH 端口转发：

```bash
ssh -N -o ExitOnForwardFailure=yes -L 127.0.0.1:8080:127.0.0.1:8080 Ubuntu用户名@VM_IP
```

保持该 SSH 会话运行，再从 Mac 访问 `http://127.0.0.1:8080/api/v1/health`。不得为调试擅自将 API 8080 或 PostgreSQL 5432 暴露到校园网/公网，不擅自修改防火墙、开放端口或暴露 Docker Engine API。

## 6. 数据与 Schema 安全

以下操作必须有针对具体对象和风险的明确授权，不能从一般开发、测试或部署请求中推断：

- `docker compose down -v`，删除正式数据库 Volume 或 `docker volume prune`。
- 清空、重新初始化 PostgreSQL，或对业务数据执行破坏性 SQL。
- 覆盖已有 `.env`、将真实 API Key 或个人数据提交到 Git。

默认禁止上述行为。真实密钥和个人数据不应进入 Git；即使得到其他操作授权，也不能顺带提交它们。

数据库 Schema 变更只能新增 Flyway 迁移，不能修改已执行的历史迁移。涉及重要数据库变更的部署，必须先准备可审阅的迁移与兼容性说明，完成备份校验并确认恢复方案，再在明确授权范围内执行。测试库隔离不等于可以重置业务库。

## 7. CI/CD 与授权

- GitHub Actions 负责代码质量、自动化测试和虚构数据评估；Ubuntu 负责正式服务；Mac 负责开发和人工管理。
- 普通 PR 测试不能直接变成生产部署任务，也不能注入正式数据库连接或部署密钥。未来自动部署必须采用独立、受控且明确授权的流程。
- 远程执行、正式部署和数据库变更遵循平台权限与用户确认要求。已有明确授权可在其范围内继续；缺失必要授权时，先准备具体可审阅的改动、备份/验证方案，再说明缺少的授权，不重复询问同一项已获授权。
- 没有明确部署要求时，不更新正式运行服务，不借测试顺带部署。

## 8. 完成时汇报

说明修改内容、验证结果、跳过项和操作实际发生的机器。区分 Mac 本地检查、GitHub Actions 隔离测试、Ubuntu 正式运维；报告连接限制，不把旧测试结果、未执行的部署或未验证的远程状态写成已通过。
