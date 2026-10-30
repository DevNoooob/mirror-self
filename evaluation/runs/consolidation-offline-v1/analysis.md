# M2-B 离线整合候选观察与错误分析

本文件归档实际 CI 结果，不是正式质量基线，也不是 BGE-M3 测量。参考由虚构案例建立，尚未独立人工复核。原始 JSON/Markdown 报告逐字保留；未修改 Gold、M2-A/v2 输入或历史报告。

运行：[GitHub Actions 40732403937](https://github.com/DevNoooob/mirror-self/actions/runs/40732403937)，执行head `68af38927b0ca28031dcd57119c29859bbf8cc37`。Java21.0.13、PostgreSQL17.11、pgvector0.8.6，Testcontainers临时库。没有Ubuntu业务库、正式API部署或真实模型调用。

## 条件与指标

31对独立虚构案例，9个期望关系、22个无关系。所有符合条件的1024维Mock向量故意相同，强制把高相似错误对交给第二阶段。它检验SQL/证据/关系规则，而不是自然语义Embedding能力。

| 指标 | 实测 |
|---|---|
| 完整关系候选 TP / FP / FN | 8 / 0 / 1 |
| Precision / Recall / F1 | 100% / 88.89% / 94.12% |
| 错误 SAME_AS 候选率 | 0/3 = 0%；未执行任何实际合并 |
| L0证据正确率 | 17/17 = 100% |
| 默认待审与relation-only策略正确率 | 8/8 = 100% |
| 实际/扫描模型请求 | 0 / 0 |

SAME_AS 3/4，SUPPORTS 2/2，CONTRADICTS 1/1，EVOLVED_FROM 2/2。22个无关系案例均未生成候选；包括助手、另一owner、SECRET/RESTRICTED、待审/拒绝/撤回/删除、条件不同、双重否定、独立事实、伪造L1及时间不一致。

## 唯一漏检：CC16

“我不喜欢重复劳动。”与“I dislike monotonous tasks.”应为SAME_AS。两方向均召回（comparedPairs=2），不是向量召回失败。第二阶段可解析两句，但有限词汇表不能把monotonous tasks映射至重复劳动，因此 `DIFFERENT_PROPOSITION_OR_CONTEXT` 保守弃答。没有候选写入，也没有被持久化/审核策略错误拒绝。

不针对该题硬编码新词，不改参考提升指标。后续先建立独立盲测及人工等价复核，再决定通用词汇规范或受控NLI扩展；继续把相似度与事实关系分离。

## 实际候选示例

| 案例 | 双方虚构记忆 | 候选 | 边界 |
|---|---|---|---|
| CC01 | 我不喝咖啡。 / I do not drink coffee. | SAME_AS | 相同消息时点、完整否定命题；仍待审 |
| CC02 | 我喜欢园艺。 / 我不喜欢园艺。 | CONTRADICTS | 相反极性不合并，不裁定哪条现在正确 |
| CC03 | 我喜欢园艺。 / 我不喜欢园艺。 | EVOLVED_FROM | 新→旧，只表示可能演化，保留缺上下文提示 |
| CC07 | 完全相同的有条件低承诺意向 | SAME_AS | 条件及可能性完整保留，不变为完成事件 |
| CC10 | 我觉得自己很耐心。 / I consider myself patient. | SAME_AS | 仅已审核显式自述，不建立客观人格结论 |
| CC11 | 同一主张增加可核实L0来源 | SUPPORTS | 增加支持，不证明跨时间持续成立 |

精确文本、ID、方向、来源和固定原因码见report.json及analysis.json。所有八条候选均PENDING_REVIEW。独立审核回归确认：APPROVED后原memory_item完整行不变，memory_relation不增加；重复/并发/切换模型不会重开已拒绝候选。

## 回归与工程修复

Java21：130单元通过；50集成通过，3个真实模型测试按保护开关跳过（MemoryEvaluationLiveIT、LiveModelIT、LocalBgeRetrievalIT）。15 Python预检通过。整合专项12项通过，CI类级耗时4.614秒包含启动与测试，不能视作生产延迟/吞吐量。

首轮CI仅在新质量报告写出时暴露Gson对Instant反射访问失败；已改为复用EvaluationJson.GSON。V4迁移、原数据保留、候选并发/审核/生命周期回归在修复后的完整CI通过。Mac单元验证实际使用JDK27，因此Java21兼容性以该CI为据。

## v3与后续

v3共45查询，R2Q13保留肯定问句/R25=REFUTES，42A历史学习/42B表达愿望。grade1保留另评、grade2时间背景仍待审。输入与参考SHA见manifest及独立审核表。用户批准的是修订范围，全部最终参考仍待人工复核；未运行v3真实模型，也不把查询数/口径改变称为模型提升。

进入V0.3前建议：先完成人工参考复核与候选盲测，建立带时间/反证/撤销的已审核关系版本图；每条L2九维判断单独绑定L1/L0并重新待审。候选关系批准不能替代人格或当前状态确认。保持类型纠正与图维护为独立授权命令，不自动合并、覆盖或建立正式质量基线。
