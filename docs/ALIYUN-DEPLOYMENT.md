# 阿里云 Ubuntu 正式部署手册

> 本文件是新增文档。环境分工与安全边界见 [现行运行环境说明](ALIYUN-ENVIRONMENT.md)；本地开发见 [Windows 本地开发手册](LOCAL-DEV.md)。既有 [AGENTS.md](../AGENTS.md) / [DEPLOYMENT.md](DEPLOYMENT.md) 仍是 Mac + VMware 版本，原文备份在 [docs/backups/](backups/README.md)。
>
> **授权边界**：本文描述可执行步骤，但**部署、正式库备份/恢复、安全组与防火墙变更、Schema 变更都必须先获得明确授权**。本文写作时没有连接任何远程环境、没有部署、没有执行数据库操作。

## 1. ECS 基线（一次性只读检查）

```powershell
ssh <SSH_USER>@<ECS_IP>

lsb_release -a                          # 期望 Ubuntu 22.04/24.04 LTS，x86_64
docker version --format '{{.Server.Version}}'
docker compose version                  # 需要 Compose v2 插件
timedatectl                             # 期望 System clock synchronized: yes
free -m; df -h /                        # 单用户 2 vCPU / 4 GiB 起步
sudo ufw status                         # 记录现状，不擅自改动
```

- 若 `timedatectl` 显示时钟未同步，先修 NTP（`chrony` / `systemd-timesyncd`）再考虑部署：项目有记忆时间语义与评估时钟要求，宿主机钟差会污染 `occurred_at`/报告时间。
- 架构必须与本地构建一致（`uname -m` 为 `x86_64` 时，本地/远程 Compose 构建都无跨架构问题）；ARM 机型必须全程在云端构建。
- 不要为了"方便"安装宝塔等会额外开放端口的运维面板。

## 2. 安全组与防火墙（阿里云的主要边界）

| 项 | 要求 |
|---|---|
| 入方向 | **只放行 22/tcp（SSH）**，来源尽量限制为你的固定出口 IP |
| 8080 / 5432 / 55432 | **不放行**；Compose 只把它们发布到 ECS 的 `127.0.0.1` |
| 2375 / 2376（Docker API） | 绝不开放，也不做 TCP socket 转发 |
| 出方向 | 默认即可；ECS 需要访问镜像仓库与 Maven Central 做构建 |
| 主机 ufw | 保持现状；不要用"关防火墙"来绕过访问问题 |

即使安全组误开，Compose 的 loopback 发布也不会让服务直接公网可达；但仍必须关闭。**跨机访问一律 SSH 隧道**，见第 6 节。

## 3. 从 Windows 只读核对目标

从 **Windows 仓库根目录**执行（不是 ECS 上）：

```powershell
docker context show
docker context inspect aliyun-ubuntu --format '{{.Name}} {{.Endpoints.Docker.Host}}'
docker --context aliyun-ubuntu info --format '{{.Name}} {{.OperatingSystem}}'
docker --context aliyun-ubuntu compose ls
docker --context aliyun-ubuntu ps --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}'
docker --context aliyun-ubuntu volume ls
```

核对：Context endpoint 指向预期的 ECS、Engine 是 Ubuntu、现有容器/卷里哪些属于正式项目 `mirror-self`、哪些属于开发项目 `mirror-self-dev`。**Context 缺失、SSH 失败、Engine 不可用时停止操作并报告原因**；禁止自动回退到本机 Docker Desktop、创建猜测的 Context 或换服务器。

## 4. 固定 Compose 项目名与卷名（最容易出错的环节）

- Compose 项目名默认取"运行 Compose 的目录名"。本仓库在 Windows 上名为 `mirror-self`，所以默认项目名是 `mirror-self`，正式卷默认是 `mirror-self_pgdata`。
- 若之前从其它目录名/项目名部署过，直接 `up` 会**新建一个空卷**，看起来像"数据丢了"（旧数据其实还在旧卷里）。因此：
  1. 先 `docker --context aliyun-ubuntu compose ls` 与 `volume ls` 查看是否已有正式项目/卷；
  2. 若已存在，**显式复用**其项目名：所有命令都带 `-p <既有项目名>`；
  3. 若首次部署，明确选定 `-p mirror-self` 并写入你的本机运维笔记（**不要写进 Git**）。
- 项目名、卷名一旦确定，后续部署、备份、恢复都必须沿用；不得通过改项目名/删卷来"解决"构建或迁移问题。

## 5. 部署

前置：本地 `.env` 已就绪（`POSTGRES_PASSWORD`、`MIRROR_OWNER_TOKEN`，见 [LOCAL-DEV.md](LOCAL-DEV.md#22-生成本地-env不入-git)）；源码在预期 commit；已确认授权与项目名。

```powershell
# 1) 本机语法/必填变量校验（不验证远程 Engine 健康或迁移兼容性）
docker --context aliyun-ubuntu compose -p mirror-self config --quiet

# 2) 从 Windows 发起，镜像在 ECS Engine 构建、容器在 ECS 运行
docker --context aliyun-ubuntu compose -p mirror-self up --build -d

# 3) 状态与日志（只检查所需范围，不复制私人原文/令牌/完整 provider 异常）
docker --context aliyun-ubuntu compose -p mirror-self ps
docker --context aliyun-ubuntu compose -p mirror-self logs --tail 100 api
```

说明：

- Compose 使用 **Windows 的源码、`Dockerfile`、`compose.yaml` 与本地 `.env`** 作为输入；ECS 不需要相同仓库路径或 `.env`。不要假设 ECS 有 `E:\...` 路径。
- ECS 需要拉取 `maven:3.9.13-eclipse-temurin-21`、`eclipse-temurin:21-jre`、`pgvector/pgvector:0.8.6-pg17` 并访问 Maven Central。首次构建较慢属正常；需要加速时在 ECS 配置阿里云镜像加速（修改 `/etc/docker/daemon.json` 会重启 Docker，属运维变更，需记录影响）。
- `api` 依赖 `db` 健康后才能启动；API 启动时 Flyway 自动执行**仅新增**的迁移（首次部署会执行 V1/V2）。
- 修改 `.env` 不会自动影响已运行容器，必须重新 `up -d` 才生效；这属于配置变更，同样需要部署授权。
- 不要在日志/报告中输出 `.env`、完整展开的 Compose 配置、真实聊天内容或数据库备份。

## 6. 验收

开一个 PowerShell 窗口保持 SSH 隧道：

```powershell
ssh -N -o ExitOnForwardFailure=yes -o ServerAliveInterval=30 `
    -L 127.0.0.1:8080:127.0.0.1:8080 <SSH_USER>@<ECS_IP>
```

另开窗口验证（本地 8080 被本地开发 API 占用时，改用 `-L 127.0.0.1:18080:127.0.0.1:8080` 并同步 URL）：

```powershell
Invoke-RestMethod 'http://127.0.0.1:8080/api/v1/health'

# 需要 Owner Token 的只读接口（令牌从本地 .env 取，不要打印）
$env:MIRROR_OWNER_TOKEN = '<正式 Owner Token>'
curl.exe -sS 'http://127.0.0.1:8080/api/v1/memories?limit=1' -H "X-Mirror-Token: $env:MIRROR_OWNER_TOKEN"
```

同时确认：`compose ps` 中 `db` 为 `healthy`、`api` 为 `Up`；日志无迁移错误、无异常堆栈。验收只做**只读**检查，不要为了"验证部署"导入真实聊天、清理旧记忆或做其它未经授权的业务写入。

## 7. 数据保护与备份

正式库在 ECS 的命名卷中（默认 `mirror-self_pgdata`）。备份必须落到 **ECS 之外**，并做恢复验证：

```powershell
$ctx  = 'aliyun-ubuntu'
$proj = 'mirror-self'
$db   = (docker --context $ctx compose -p $proj ps -q db).Trim()

# 1) 容器内生成自定义格式 dump，并确认可读
docker --context $ctx exec $db pg_dump -U mirror -Fc -d mirror_self -f /tmp/mirror-self.dump
docker --context $ctx exec $db pg_restore --list /tmp/mirror-self.dump | Select-Object -First 5

# 2) 取回 Windows 本地受保护目录（不要放在 Git 仓库内）
New-Item -ItemType Directory -Path 'E:\backup' -Force | Out-Null
docker --context $ctx cp "${db}:/tmp/mirror-self.dump" "E:\backup\mirror-self-$(Get-Date -Format yyyyMMdd-HHmm).dump"

# 3) 上传 OSS / 加密离线介质；建议至少保留最近 7 天 + 每周快照
```

恢复演练（在 ECS 上用**临时容器 + 临时库**，绝不碰正式卷；演练完删除容器）：

```powershell
$tmp = (docker --context $ctx run -d -e POSTGRES_PASSWORD=temp `
        -e POSTGRES_DB=mirror_restore_check pgvector/pgvector:0.8.6-pg17).Trim()
docker --context $ctx cp "E:\backup\<上面的备份文件名>.dump" "${tmp}:/tmp/mirror-self.dump"
docker --context $ctx exec $tmp pg_restore -U postgres -d mirror_restore_check --no-owner --no-privileges /tmp/mirror-self.dump
docker --context $ctx exec $tmp psql -U postgres -d mirror_restore_check -c 'select count(*) from source_record;'
docker --context $ctx rm -f $tmp
```

- 备份文件名、时间、校验结果记入运维记录；**备份不进 Git、不进 CI 产物、不发到聊天工具**。
- 正式库恢复是破坏性操作：必须单独授权、停写窗口、已校验的备份、明确回滚方案；不能把"镜像回退"当成"数据库回退"。
- ECS 磁盘快照可作为兜底，但**不能替代**逻辑备份（`pg_dump`）；两者都要有。

## 8. Schema 变更规则

- 只允许**新增** Flyway 迁移（`V3__...sql` 起），不得修改已执行的 `V1`/`V2`，也不得手工改 `flyway_schema_history`。
- 重要变更按"扩展 → 迁移数据 → 收缩"设计，保证旧版应用在新迁移存在时仍可启动；部署前审阅新旧兼容性并准备失败恢复方案。
- 部署前：确认目标 Engine/项目/数据库与当前版本 → 备份并校验 → 明确演练恢复步骤 → 获授权后在窗口内执行 → 检查迁移结果。
- 迁移失败时不要删表、清库或回滚迁移来"修复"；保留现场、读日志、按恢复方案处置。

## 9. 日常运维

```powershell
docker --context aliyun-ubuntu compose -p mirror-self ps
docker --context aliyun-ubuntu compose -p mirror-self logs --tail 200 api
docker --context aliyun-ubuntu compose -p mirror-self restart api
docker --context aliyun-ubuntu system df          # 只读查看镜像/卷占用
```

- 不执行 `docker volume prune`、`down -v`、`docker system prune --volumes`；不要因为磁盘紧张删除正式卷或数据库。
- 定期核对：安全组是否仍只开 22、`.env` 是否仍只在本机、备份是否按计划落盘、时钟是否同步。
- 日志可能含业务信息：只检查所需范围，不在报告或聊天中复制私人原文。

## 10. 回滚

- **代码回滚**：在 Windows 切回上一个已验收的 commit，再 `docker --context aliyun-ubuntu compose -p mirror-self up --build -d`；不修改 compose 项目名、不删卷。
- **配置回滚**：恢复上一版 `.env`（本机受保护副本）后重新 `up -d`。
- **数据库回滚**：只能从已校验备份恢复，属破坏性操作，需要明确授权与维护窗口；不要执行反向 DDL 迁移。
- 回滚后重新执行第 6 节验收，并在记录中写明回滚到的 commit/镜像与数据库状态。

## 11. 阿里云特有注意事项

| 项 | 说明 |
|---|---|
| 安全组变更 | 属基础设施变更：记录变更人/时间/原因/规则；不要图省事放行 8080/5432 |
| 时钟 | 部署与评估前确认 `timedatectl`/`chronyc tracking`；钟差会影响记忆时间语义与报告时间 |
| 镜像加速 | 可选配置 `/etc/docker/daemon.json` 阿里云加速器；重启 Docker 会影响所有容器，需窗口与记录 |
| 磁盘 | 系统盘/数据盘容量与快照策略；`pgdata` 增长要看 `docker system df`，不要靠删卷清空间 |
| 带宽/计费 | 首次构建会拉基础镜像、Maven 依赖；评估流量与费用 |
| 公网暴露 | 不开放 Docker API、不装面板、不为调试加端口；需要公网 HTTPS 访问必须另做安全设计与授权 |

## 12. 任务结束记录

每次部署/备份/迁移/回滚后记录：

| 项 | 内容 |
|---|---|
| 执行端 | Windows 本地（哪台机器）、实际 SSH 用户与 ECS IP 的引用方式（不要在报告中写密钥） |
| 目标 Engine | `aliyun-ubuntu` Context、Ubuntu 版本、Docker/Compose 版本 |
| 授权范围 | 用户明确要求了什么、包含哪些对象（服务/数据/安全组） |
| 变更 | 代码 commit、镜像、迁移版本、`.env` 变更项（不写具体值） |
| 验证 | `compose ps`、健康检查、迁移结果、备份/恢复演练结果 |
| 跳过/失败 | 未执行的步骤、连接失败原因、被跳过的测试 |
| 数据影响 | 是否写正式库、是否有备份、是否可回滚 |

没有实际连接 ECS 时，明确写"未验证远程状态"，不要沿用历史记录宣称当前服务正常。
