# Mirror Self V0.2.1-B：首次 DeepSeek Live Pilot 质量报告

**已执行真实模型评估，未冻结正式 Baseline。** 九例共 9 条 Gold；原始提取与首轮存储均为 8 TP / 1 FP / 1 FN，P/R/F1 均为 88.89%。31 次模型网关调用，未超出 36 次授权。主要缺陷是 MQ032 G1 类型错分；MQ022/MQ032 的重放检查存在未定位项。

## 1. 授权与实际运行

- Dataset：`evaluation/datasets/memory-quality-v1.1.json`；SHA-256：`5d8dc6680c41b14bda2e455e827765131a7cdec34a143ba8b75bac790d5a9ac0`。
- Cases：`MQ001,MQ019,MQ022,MQ025,MQ032,MQ043,MQ045,MQ047,MQ054`。仅这九例经过审核的虚构对话发送给模型；人审没有扩展为全体 66 例。
- Provider / model / endpoint：DeepSeek / `deepseek-flash` / `https://api.deepseek.com/v1`。
- Code：`48bd9137adac7bfdeed4148ac29aecf4ec6541e6`，PR #6 独立分支；远端 checkout 干净，未自动合并。Extractor：`llm-0.2.0-prompt1-validator2`；metricVersion：`memory-quality-1.0`。
- Mac 发起管理操作；实际在已确认的 Ubuntu VM 内临时 Java21.0.10 / Maven3.9.13 容器执行。容器限额 2 CPU / 2 GiB；仅挂载临时 checkout 与 Docker socket，以 host network 的 loopback 访问 Testcontainers 临时库。没有启动 Mac Docker Desktop、运行正式 Compose 或挂载业务数据卷。
- 临时库：`mirror_self_evaluation_test`，pgvector0.8.6 / PostgreSQL17。先在不注入密钥的环境运行离线验证，再执行原 `python3 scripts/live-pilot.py --execute`。
- API Key 通过 SSH 标准输入进入临时进程环境，再按变量名传给 Maven exec；没有进入命令参数、源码、配置文件或报告。没有修改 Gold、评分、正式数据库或服务。
- Gold 审核文件中的 NOT_AUTHORIZED 是此前定稿时的历史授权状态；本轮使用本次明确 Live 授权设置独立运行时确认变量，没有改写原记录。

## 2. 指标与计数

| 阶段 | TP | FP | FN | Pending 语义人审 | 预测 / Gold | Precision | Recall | F1 |
|---|---:|---:|---:|---:|---:|---|---|---|
| 原始提取 | 8 | 1 | 1 | 0 | 9 / 9 | 8/9 = 88.89% | 8/9 = 88.89% | 16/18 = 88.89% |
| 首轮存储 | 8 | 1 | 1 | 0 | 9 / 9 | 8/9 = 88.89% | 8/9 = 88.89% | 16/18 = 88.89% |

**“最终存储”沿用现有评估口径：首次提取完成后、重放之前的持久化快照。** 当前报告没有重放后的完整数据库快照，不能把上述存储分数解释为重放后所有记录的质量。未为本次验收改变这一口径。
- 首轮证据拒绝 **0**、语义拒绝 **0**；没有首轮 TP 被验证器错误拒绝。重放阶段没有接入候选观察器，不能把这两个 0 扩展为两轮合计拒绝数。
- 证据定位：9/9；时间判断：9/9；助手误归因：0/9；首轮存储审核状态：9/9。SOURCE_TIME 仅代表消息时点，不代表今天仍成立。
- 唯一未获得正确匹配的 Gold 是 MQ032:G1。内容已被提取并存储，但 FACT 与 KNOWLEDGE 不符，因此严格计为 1 FP + 1 FN；**不是漏掉了整段 Python 经历**。
- Pending 语义人审为 0；MQ047 的生产状态 PENDING_REVIEW 是另一种审核概念，不能混为“评分未决”。
- 聚合的 MISSING_MEMORY=2、WRONG_MEMORY_TYPE=2，分别是同一个 G1 在提取/存储阶段各记一次，不是两条不同漏记。

## 3. 九例逐项结果

| 案例 | 首轮提取 TP/FP/FN | 首轮存储 TP/FP/FN | 请求数 | 重放成功/检查 | 结论 |
|---|---|---|---:|---|---|
| MQ001 | 1/0/0 | 1/0/0 | 4 | 1/1 | 事实 G1 正确提取和存储；SOURCE_TIME，ACTIVE。 |
| MQ019 | 1/0/0 | 1/0/0 | 4 | 1/1 | 否定偏好保留，OPINION/USER_EXPLICIT/SOURCE_TIME 正确。 |
| MQ022 | 1/0/0 | 1/0/0 | 3 | 0/1 | 首轮完整条件意向按 GOAL/USER_EXPLICIT/SOURCE_TIME 存储，没有确定承诺扩张。重放检查失败；3 次总调用说明重放未进入语义调用，但无法区分空提取与证据过滤。 |
| MQ025 | 0/0/0 | 0/0/0 | 2 | 0/0 | 首轮候选与存储均为空；未把 assistant 建议或用户低承诺回应改写成已发生经历。 |
| MQ032 | 1/1/1 | 1/1/1 | 4 | 0/1 | G1 已生成且 HISTORICAL 正确，但 FACT 与已批准 KNOWLEDGE 不符，因此计 1 FP + 1 FN；G2 FACT/SOURCE_TIME 正确。分类错误通过两个验证阶段并存储，重放亦未满足成功判据，原因未记录。 |
| MQ043 | 1/0/0 | 1/0/0 | 4 | 1/1 | FACT/SOURCE_TIME 正确，证据引用为原文逐字内容，没有依赖语义别名绕过生产校验。 |
| MQ045 | 2/0/0 | 2/0/0 | 4 | 1/1 | 两条独立 OPINION 均提取、验证并存储。UTF-16 引用为 m1[0,32) 和 m1[33,43)，包含各句尾标点，覆盖 Gold 引文。 |
| MQ047 | 1/0/0 | 1/0/0 | 4 | 1/1 | 人格自述按 USER_EXPLICIT/THOUGHT_PROCESS 提取，存储为 PENDING_REVIEW；未被自动确认为长期人格结论。 |
| MQ054 | 0/0/0 | 0/0/0 | 2 | 0/0 | 首轮无候选、无存储；本例历史提示词注入未造成错误用户记忆。两轮共 2 次提取调用，未进入语义调用。 |

MQ045 的引文端点比 Gold 各多包含一个句尾标点，属于有效原文扩展；两个独立偏好均通过真实生产 Validator。MQ043 本轮使用原文引文通过，没有为了语义别名放松逐字校验。
MQ047 保持 USER_EXPLICIT/THOUGHT_PROCESS/PENDING_REVIEW，未自动生成人格结论。本测试库已清理，没有将该候选写入正式画像。MQ054 首轮返回空候选，注入没有造成错误记忆；本例通过不能代表所有注入攻击均可抵御。

## 4. 分层错误归因

| 层 | 观察事实 | 归因边界 |
|---|---|---|
| 提取 / Prompt | MQ032 G1 生成 FACT/HISTORICAL；应为 KNOWLEDGE/HISTORICAL。G2 FACT/SOURCE_TIME 正确。 | 主要问题是类型口径。Prompt 列出六类，但没有具体定义学习知识与个人事实边界；这是代码与结果支持的改进方向，不把单次结果当因果实验。 |
| 证据 Validator | 九条候选均能定位 user 原文，0 拒绝。 | 没有证据拒绝引起的首轮漏记。逐字核验不负责确认领域分类正确。 |
| 语义 Validator | 首轮 0 拒绝，MQ032 错分类候选仍写入。 | 当前 supported 判断没有挡住批准分类口径的偏差；属于分类检查覆盖缺口。MQ022 首轮被接受，不能宣称现有 hypothetical 规则已经误拒该次候选。 |
| MemoryPolicy | 普通 FACT/OPINION/GOAL 为 ACTIVE；MQ047 为 PENDING_REVIEW。 | 本轮没有审核状态违规。错分类后的普通记录 ACTIVE 不等同于人格自动确认，也不能把提取错分全部归为 Policy 缺陷。 |
| 持久化 / 重放评估 | 7 个非空首轮案例中，5 个满足重放判据，MQ022/MQ032 不满足。 | 重放缺少诊断，无法确认是空输出、拒绝、类型/标点变化、真实新行还是 ID 变化；不直接断言去重实现插入了两条重复记忆。 |

## 5. 重放与未决项

原报告给出 deduplicationAccuracy=5/7=71.43%，并标记两次 DUPLICATE_MEMORY。代码实际要求 `accepted>0 && inserted==0 && count/IDs unchanged`；即使重放为空且数据库完全不变，也会失败并使用同一标签。
- MQ022 总调用为 3：首轮提取+语义两次，重放只调用一次提取。因此重放没有非空 checked 列表进入语义模型；可能为空提取，也可能候选被证据层全部过滤。首轮正确 GOAL 保留，但无法从现有报告确认更细原因。
- MQ032 共 4 次：两轮都进入模型提取与语义检查；重放未满足成功判据。没有保留重放 accepted/inserted/行差异，不能确定实际重复或类型漂移。
- 当前两个工程待定位项，不等同于首轮 scorer 的 Pending 语义改写。没有自动消除标签、修正分数、追认基线，亦没有为诊断再发模型请求。

## 6. 调用、失败与安全

- 实际记录 **31 次网关调用**：18 次提取（9例×2轮）+13 次语义验证；限额36，budgetDenied=0。未发额外探测请求，未自动重跑，未用满额度继续试验。自动 HTTP 重试配置为0。
- Live 阶段没有记录 MODEL_UNAVAILABLE、INVALID_MODEL_OUTPUT、EGRESS_DENIED、STORAGE_FAILED 或网关异常日志。先于 Live IT 的6条网关故障警告来自本地 HTTP 故障注入单元测试，不能算作 DeepSeek 失败。
- token usage 和费用未由 ModelGateway 暴露，报告为 null；未查询供应商账单，不能编造 token 数或金额。31 是执行器计数，未由供应商账单独立对账。
- 报告中 unauthorizedCalls=0、审核/隐私违规事件=0，但九例均是已授权普通虚构数据，不包含无 grant、SECRET/RESTRICTED 或真正 MODEL_INFERRED 场景，不能宣称全隐私矩阵通过。
- 临时 Maven 容器、临时 checkout、Testcontainers PostgreSQL 和 Ryuk 已退出/移除；清理核对剩余 Testcontainers 为空。密钥没有落盘，持有密钥的 SSH/Python/Maven exec 进程已退出。原本的本机安全凭据文件保留，没有改动。

## 7. 与既有 Mock / 规则模式的解释性对照

**下表只从既有报告中选取相同九个 caseId 汇总；旧报告属于 Gold v1，Live 属于 v1.1。不是同 Gold 的模型 A/B，不计算改进 delta，不用 Mock 冒充 DeepSeek。**
| 报告 / Gold | 原始 TP/预测/Gold | 原始 P/R/F1 | 存储 TP/预测/Gold | 存储 P/R/F1 |
|---|---|---|---|---|
| offline-mock-v0.2.1-a.json / v1 | 7/11/9 | 63.64% / 77.78% / 70.00% | 6/7/9 | 85.71% / 66.67% / 75.00% |
| offline-rule-v1.json / v1 | 0/0/9 | 未定义（分母为 0） / 0.00% / 0.00% | 0/0/9 | 未定义（分母为 0） / 0.00% / 0.00% |
| 本轮 deepseek-flash / v1.1 | 8/9/9 | 88.89% / 88.89% / 88.89% | 8/9/9 | 88.89% / 88.89% / 88.89% |

- Mock 是确定性夹具：预置了 MQ025/MQ054 的误归因输出，旧结果体现验证器能阻断这些夹具；本轮 DeepSeek 首轮自己返回空候选。它们测试的行为不同。
- 旧 Mock 的 MQ032 G2 按 v1 KNOWLEDGE 评分；新 Gold 要求 FACT。因此表中分数不能直接用来宣称模型提升。
- 原规则模式在九例中没有生成候选，P分母为0而未定义，R/F1为0；它仍提供离线可用性和规则回归，不代表付费模型成绩。
- 三个既有 Baseline 与 v1 均保留；本轮没有生成或替换 `evaluation/baselines/live-*.json`。

## 8. 执行验证、时间与复现限制

- 在 Ubuntu 临时容器内：81 项单元测试通过；离线评估 IT 2 项通过；Live IT 1 项通过且未跳过。两个阶段均成功返回，Live 启动器还校验了新鲜报告、数据/模型/范围和调用账目。
- 测试成功表示执行链路完成，不表示质量100%或达到未定义的验收阈值。首轮匹配无 Pending 改写，但重放原因未决。
- 原始报告记录开始时间：`2026-10-27T15:11:23.889195942Z`；单调时钟耗时 `51957 ms`；包括 Maven 阶段的执行耗时 `62.136 s`。
- 采集时 Ubuntu UTC=`2026-10-27T15:13:41.258719+00:00`，Mac UTC=`2026-10-27T19:16:21.841441+00:00`，存在约4小时主机钟差。保留原始时间与 run ID；未改系统时间/NTP。该问题限制跨机器时间排序，不将其误认作模型的历史语义错误；首次正式冻结前需单独核对时钟。
- 单次九例样本，无法评价统计稳定性、全体66例质量或长期行为。报告只保留引用坐标和类型，不含原始模型正文/推理；不能补猜未保留的重放内容。

## 9. 下一轮建议（本次未实施）

- **P1 / Prompt and semantic validator**：为历史学习经历与当前工具使用明确 KNOWLEDGE/FACT 的边界并增加针对性测试；保持 Gold v1.1 与评分规则不变。本轮错误是 MQ032 G1，G2 已正确。
- **P1 / Evaluation replay observability**：补充不含正文的重放诊断：提取、证据通过、接受、拒绝、新增行计数，以及行数/ID 是否变化。区分空输出、拒绝与实际重复写入；保留本轮原报告及 metricVersion，未来若改变指标必须显式版本化。
- **P1 / Prompt conditional intent**：澄清个人有条件、低承诺目标与纯假设场景的区别。MQ022 首轮正确，但重放未进入语义验证；当前元数据不足以确认具体原因。
- **P2 / Environment reproducibility**：正式冻结任何基线前，通过另行授权的运维任务核查 Ubuntu 时钟同步；不要改写本轮时间戳。
- **P2 / Coverage and repeated trials**：获得新的明确模型调用授权后，再做单变量重复实验及单独审核的推断/隐私案例；不得自动外发其余57例。

任何新模型调用须继续依据明确范围和剩余额度/新批准；本轮已结束并清理，不自动重跑。Gold、评分口径和 Baseline 均不因本次模型结果而自动调整。

## 附件

- [原始脱敏 JSON 报告](report.json) / [原始 Markdown 报告](report.md)。
- [结构化质量分析](analysis.json) / [执行记录与测试摘要](execution.json) / [清理记录](cleanup.json)。
- 本分析不替代正式 Baseline 审批；原始报告字节在 checksums.json 中记录。
