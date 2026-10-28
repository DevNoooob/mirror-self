# Memory Quality Evaluation v1

66 个虚构多轮案例、34 个场景标签。标准答案与固定 Mock 预测分别存储；运行时不会用标准答案构造模型响应。标准答案为工程参考标注；v1.1 中的指定九例已由用户亲自审核，**其余 57 例仍未声明人工审核**。审核仅代表标注结论，不授权模型外发；不是被评估模型的自评结果。所有测试姓名、城市、经历及敏感场景均为虚构。

- 数据契约：[dataset.schema.json](dataset.schema.json)，运行前另有 Java 交叉字段校验。
- 旧标准数据：[datasets/memory-quality-v1.json](datasets/memory-quality-v1.json)，原文件保留。
- 正式 v1.1：[datasets/memory-quality-v1.1.json](datasets/memory-quality-v1.1.json)；[九例审核范围与结论](reviews/memory-quality-v1.1-reviewed.md)。
- 固定、故意包含错误的 Mock：[fixtures/mock-v1.json](fixtures/mock-v1.json)。
- 首次实测 Baseline：[baselines/](baselines/)，由真实执行生成，不能通过修改 Mock 来掩盖退化。
- 完整定义与命令：[V0.2.1 设计和使用说明](../docs/V0.2.1_MEMORY_EVALUATION.md)。

## 默认离线运行

需要 Java 21、Maven 和隔离 Docker。**优先由 GitHub Actions 执行集成评估**；下列命令仅在 CI 或已确认 Engine 访问及数据库隔离的 Ubuntu 测试进程运行。Mac 仅运行普通单元测试，不能仅凭 CLI 的 `mirror-ubuntu` Context 认定 Testcontainers 已正确连接。完整环境规则见 [远程部署与测试约定](../docs/DEPLOYMENT.md#测试与-cicd)。执行前用 `mvn -v` 确认 Maven 使用 Java 21。

```bash
bash scripts/evaluate-memory.sh offline
bash scripts/evaluate-memory.sh rules
# CI：全部单元测试、原 V0.1/V0.2 集成测试及两种离线评估
MIRROR_RUN_LIVE_LLM=false MIRROR_EVALUATION_LIVE_ENABLED=false mvn verify
```

GitHub PR 与 main 推送会通过 `.github/workflows/memory-quality.yml` 自动执行 Java 21 / Maven verify。CI 显式关闭两个 Live 开关，并先检查 Docker；结束后上传虚构数据评估报告，保留 14 天。

报告在 `target/evaluation/offline-mock/`、`target/evaluation/offline-rule/`，每种均有 `report.json` 与 `report.md`。默认不读取 `.env`，不需要模型 Key，不调用付费服务。集成测试在 Docker 不可用时会被跳过，跳过不是通过。

本机缺少 Java 21 或无法连接目标 Engine 时，报告限制并使用已有 CI；不要启动 Mac Docker Desktop 或自动回退 `desktop-linux`。若需在 Ubuntu 执行测试，单独确认该机源码、Java 21 / Maven 与 Docker 访问，不假设存在 Mac 的源码路径、Maven 缓存或 `.env`。不要把 Mac 的 `docker run -v` 挂载命令直接用于远程 Engine。

临时 pgvector PostgreSQL 数据库名固定以 `mirror_self_evaluation_` 开头；业务数据库名会被运行器拒绝。新数据库自动执行现有 Flyway V1/V2，结束后由 Testcontainers 回收，不执行业务数据删除或重置。

## V0.2.1-A 九例审查与预检

[v1.1 九例人工审核记录](reviews/memory-quality-v1.1-reviewed.md)已按用户 2026-10-28 的明确批准定稿；[原提案](reviews/memory-quality-v1.1-proposal.json)已应用到新文件，v1 原件保留。审核范围只有这九例，顶层 PENDING_HUMAN_REVIEW 不被改成全体已审核。Live 和付费调用仍未授权。

```bash
# Mac 可安全执行：无模型、无 Docker、无数据库、无环境开关修改
python3 scripts/live-pilot.py --dry-run
```

九例限定、36 次调用上限、隔离 Linux 执行与分析流程见 [V0.2.1-A 指南](../docs/V0.2.1-A_LIVE_PILOT.md)。不要以预检成功或 PR 合并代替人工审核和付费授权。

## 独立审核与 Live

九例 v1.1 标注审核已完成，不需要重复宣称待人审；其余 57 例没有获得该结论。后续模型外发仍需独立批准数据摘要、案例范围、目标模型和预算；文件指纹不是外发许可。新的语义修订需新增数据版本及相应审核，不能更改旧数据掩盖指标。

以下 Live 命令仅用于另行明确授权的隔离测试进程，不部署正式 API，也不能连接业务库。普通 PR CI 仍关闭 Live，不为其注入正式部署或模型密钥。

真实评估可能每个非阻断案例产生多次提取、语义验证调用，且 repeatExtractions 会重放。**本次开发不默认运行 Live**。安全环境注入模型凭据后，另行明确启用：

```bash
export MIRROR_LLM_BASE_URL=https://api.deepseek.com/v1
export MIRROR_LLM_MODEL=deepseek-flash
# MIRROR_LLM_API_KEY 仅通过安全环境注入，不放在命令历史、报告或源码中
export MIRROR_MODEL_EGRESS_ENABLED=true
export MIRROR_EVALUATION_LIVE_ENABLED=true
shasum -a 256 evaluation/datasets/memory-quality-v1.1.json
# 审核后手工填写上一步摘要；脚本不会替你自动同意
export MIRROR_EVALUATION_APPROVED_DATASET_SHA256='审核后的64位摘要'
# 审核和模型调用都获明确批准后，操作者才设置以下确认；工具不会自动同意
export MIRROR_EVALUATION_GOLD_REVIEWED=true
export MIRROR_EVALUATION_CASE_IDS=MQ001,MQ019,MQ022,MQ025,MQ032,MQ043,MQ045,MQ047,MQ054
export MIRROR_EVALUATION_APPROVED_CASE_IDS=MQ001,MQ019,MQ022,MQ025,MQ032,MQ043,MQ045,MQ047,MQ054
export MIRROR_EVALUATION_MAX_MODEL_CALLS=36
python3 scripts/live-pilot.py --execute
```

Live 未设置案例时现在会拒绝，不会默认跑全量；未知、重复、不匹配批准范围的案例同样拒绝。调用限额耗尽前停止外发，保留部分报告并使该次运行失败，不能将不完整结果冻结为可信基线。人工确认环境变量只是操作者的声明，不是系统已验证独立人工审核的证明。

Live 仍通过现有 Spring AI ModelGateway、全局出站开关、逐来源短期授权及 SECRET/RESTRICTED 阻断。敏感案例只能在数据集声明 SENSITIVE_GRANT 时获取敏感授权；NO_GRANT 不创建授权。没有绕过生产隐私策略。只生成隔离 owner 的评估记忆，不创建新的 HTTP API。

## Baseline 与阈值

```bash
# 指定同一模式、相同数据摘要和案例集合的历史报告
bash scripts/evaluate-memory.sh offline -Devaluation.baseline=/absolute/path/baseline.json
# 可选质量门槛；示例是目标配置，不代表已经达到
bash scripts/evaluate-memory.sh live -Devaluation.thresholds=evaluation/thresholds.example.json
# 自定义输出目录
bash scripts/evaluate-memory.sh rules -Devaluation.output=target/evaluation/rule-experiment
```

默认离线 CI 比较仓库内 Baseline 的总体/分类指标及失败签名，忽略时长、运行时间和随机数据库 UUID。报告先落盘，再执行回归断言。Live 默认只报告，不套用离线基线或示例门槛；明确提供 Live baseline/thresholds 后再比较。

需要接纳新的离线基线时，先把 `-Devaluation.baseline` 指向不存在的新文件运行，检查每个差异、变更原因和测试结果，再将实际报告复制到对应 Baseline 并提交评审。不要静默重写预期值。

## V0.2.1-A 的基线迁移

MQ045 句界修复后，默认 Mock 回归使用新增的 `baselines/offline-mock-v0.2.1-a.json`，来自 GitHub Actions 实际执行；只改变 MQ045 的存储结果，不改变 Gold 或 Mock 快照。旧 `offline-mock-v1.json` 和 `offline-rule-v1.json` 保留，规则仍使用原基线。指定旧 Mock 基线会输出真实差异并按确定性门槛失败，这是有意的兼容性变化，不代表付费模型测量。

指标口径保持 memory-quality-1.0；报告 1.1 的审计元数据不作为评分算法变化。数据集 v1.1 已单独定稿，但这些旧基线仍属于 v1，不改写或冒充为 v1.1 的性能成绩。不同数据 SHA 的比较将被标为不兼容，不输出模型改进 delta。

## V0.2.1-C：重放诊断与历史观测

首次九例 Live 的 [原始资料](runs/deepseek-flash-nine-v1.1-first/README.md)按字节保存，尚未冻结基线。新报告1.2增加 replay-1.0：模型输出一致性与数据库行集合幂等性独立统计；旧5/7及评分不被改写。CLASSIFICATION_CONFLICT 阻断同命题/同证据的跨类型重放，保护已审核记录。

详见 [离线诊断与冻结方案](../docs/V0.2.1-C_DIAGNOSTICS.md)、[六类规范 types1](../docs/MEMORY_TYPES.md)。本次不调用模型；冻结提案仍为未批准且未就绪。

## V0.2.2-M1 检索离线基线

独立虚构数据 `datasets/retrieval-fictional-v1.json` 包含 18 个 L1/L0 样本、20 个查询和受控词义向量 fixture。使用真实 PostgreSQL/pgvector SQL 比较 KEYWORD / VECTOR / HYBRID，不生成答案。它与记忆提取 Gold v1/v1.1 和 DeepSeek Live 报告分开，不改变任何已有分数或基线。

仅在 GitHub Actions 或已确认隔离的 Java 21 + Docker 测试进程：

```bash
mvn --batch-mode --no-transfer-progress -Dit.test=RetrievalEvaluationIT verify
# 全量回归（推荐）
mvn --batch-mode --no-transfer-progress verify
```

输出 `target/evaluation/retrieval-offline-v1/report.json` 与 `report.md`，CI 作为现有 `memory-quality-reports` artifact 保存。固定 K=3、候选池100、RRF常数60、余弦阈值0.35；九个正例宏平均 Recall@3/MRR/nDCG@3，11个空集案例单独统计准确空结果，证据正确率核对实际 L0 ID/role/逐字引用。RQ20 故意暴露否定查询的相关性缺陷，不通过改评分隐藏。时间使用单调计时统计100次/模式的p50/p95（每查询5次，暖身不计入），不能外推生产吞吐。

受控 lexicon Mock 只验证检索基础设施及评分实现，**不代表 bge-m3、DeepSeek 或任何真实 Embedding 的语义质量**。数据为新建虚构预期，不声称完成人工独立标注。完整配置、风险见 [M1指南](../docs/V0.2.2-M1_RETRIEVAL.md)。

首次离线控制基线保存在 `baselines/retrieval-offline-v1.json`，包含来源CI run/head、原报告SHA和每例排名。回归测试校验数据SHA、三模式质量指标和确定性排名；延迟仅记录、不设硬阈值。它只固定当前离线fixture行为，不冻结或替代任何Live结果。
