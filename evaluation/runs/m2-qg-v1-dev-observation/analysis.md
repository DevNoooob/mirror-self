# M2-QG DEV：实际离线观察与逐例归因

本报告不是BGE-M3成绩，不是正式Baseline。未打开留出成绩，未调用真实模型，全部参考仍待独立人工复核。

[Java21 CI 41043599934](https://github.com/DevNoooob/mirror-self/actions/runs/41043599934)通过，source head `c329e4182a8c4bf1d513c89503767eea8d6604e3`；实际checkout合并版本见manifest及原始JSON。136单元通过，61隔离集成通过、4个真实模型入口跳过，21 Python预检通过。CI绿色表示工具与安全断言通过，不表示覆盖率或人工/真实验收完成。

## 冻结条件

开发集60对（30正关系），每对三个干扰；留出60对未评测。全部向量为同一1024维fixture，cosine相同，仅验证工程与规则防线。Mock provider调用291次，真实/云模型0，模型失败0。门禁、输入及参考SHA在评分前固定，文件不改。小pair池不代表大库性能。

| 模式 | pair候选R@10 | 查询R@3 | 分类P/R/F1（已召回） | 端到端P/R/F1 | 冻结数值门禁 |
|---|---:|---:|---|---|---|
| KEYWORD | 25/30=83.33% | 83.33% | 100% / 100% / 100% | 100% / 83.33% / 90.91% | FAIL：候选R@10低于90% |
| HYBRID Mock | 30/30=100% | 96.67% | 100% / 83.33% / 90.91% | 100% / 83.33% / 90.91% | PASS，仅该DEV Mock条件 |

pair召回任一方向命中即可；每anchor查询口径与pair口径分开。纯向量诊断R@10两模式均100%，因为使用完全相同fixture向量，不能解读为真实语义能力。KEYWORD诊断也读取已索引向量以对照，但不参与KEYWORD排名/分类，不额外调用Embedding。

两模式错误SAME_AS=0、错误无关关系=0（机会分母KEYWORD60、HYBRID350）、owner/隐私/证据错误=0；证据各55/55，默认待审策略与方向冲突审核正确率均100%。方向guard使用已批准输入中的隔离素材，2类×(双向阻断+拒绝后解决)共6个检查，不混入候选质量分母。L2和memory_relation写入均0。

## 每个有效关系遗漏

相同5对在KEYWORD是召回遗漏，在HYBRID Mock已被召回、却被关系规则弃答。不能将后者归为BGE漏检。

| ID | 两条虚构记忆 | KEYWORD | HYBRID Mock |
|---|---|---|---|
| QG0100 | 我不喜欢安静的图书馆。 / I dislike quiet libraries. | 无目标候选 | DIFFERENT_PROPOSITION_OR_CONTEXT，分类弃答 |
| QG0101 | 我不喜欢慢节奏旅行。 / I dislike slow travel. | 无目标候选 | DIFFERENT_PROPOSITION_OR_CONTEXT，分类弃答 |
| QG0102 | 我不喜欢手绘地图。 / I dislike handmade maps. | 无目标候选 | DIFFERENT_PROPOSITION_OR_CONTEXT，分类弃答 |
| QG0103 | 我不喜欢深夜会议。 / I dislike late meetings. | 无目标候选 | DIFFERENT_PROPOSITION_OR_CONTEXT，分类弃答 |
| QG0104 | 我不喜欢小组辩论。 / I dislike group debates. | 无目标候选 | DIFFERENT_PROPOSITION_OR_CONTEXT，分类弃答 |

参考将这些双语否定偏好标SAME_AS，仍须人工确认词义与范围。当前有限词表未统一对应对象，规则没有凭相似度确认事实。不针对题目加硬编码、不改参考、不降低门槛。全部逐例尝试、原始keyword/vector/fused fixture key、弃答原因及案例集合保留于report.json；负例的保守弃答也独立统计。

## 安全与方向性修复

V5仅扩展幂等键加入direction，V4字节不改；已APPROVED完整行在V4→V5测试中保持不变。同类型SUPPORTS/EVOLVED_FROM反向假设分别保存、显示RELATION_DIRECTION_CONFLICT，不能独立双批准。已有批准不自动改方向。真实执行器固定Linux Unix Docker socket/loopback，独立Testcontainers库；本次未连接Ubuntu正式环境。

本轮同时加强了对keyword/vector原始候选的权限计数，以及空临时库数据库名称校验；参考、评分阈值和生产命题规则不改。任何未授权来源或错误SAME_AS都会使安全回归失败，不能以观察模式绕过。

## 当前准入判断

`v03Ready=false`。原因包括：KEYWORD覆盖门禁失败、本轮真实BGE和独立留出未验收、76项旧参考与新120对参考均未完成人工确认。新参考潜在语义争议另见review concerns，不据此回改分数。已批准关系图可继续设计审阅，不应宣称当前具有可信九维推断基础。

后续先人工确认标注边界，再明确授权固定四SHA、最终代码SHA、完整BGE摘要及最多600次本地请求。若决定KEYWORD仅作对照而采用不同验收口径，应事前创建新门禁版本，不能回写本次冻结结果。真实结果与本Mock、旧31对、旧v2/v3均分开保留，不跨数据集宣称模型提升。
