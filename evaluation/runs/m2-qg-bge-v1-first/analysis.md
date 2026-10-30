# M2-QG 首次真实本地 BGE：留出分类覆盖未过门禁

**真实运行已完成，质量准入未通过；不是正式Baseline。** 本轮已获用户独立授权，但授权执行不代表用户确认参考答案。原始JSON/Markdown逐字保留，输入、参考、门槛和模型均未按结果修改。

## 执行证据与边界

批准代码 `0a4baa07e41891d61cd623d37d5931e80f596504`，计划SHA `a14530ff4c27b9973ad8ab57fcd69adc468ce2e2aa81f5b4d4aebb7357a8de87`。bge-m3 / ollama-790764642607，完整模型摘要 `7907646426070047a77226ac3e684fbbe8410524f7b4a74d02837e43f2146bab` 前后均匹配。

- 唯一完整推理运行：588次Embedding、0失败、0查询Embedding、0云请求；4次摘要核验GET均成功。600次推理上限未用满，没有推理重试或追加模型请求。
- DEV60/HOLDOUT60全部执行；每例含两个目标和三个干扰，共600条虚构L1，588条符合既有索引资格。召回/分类不读取参考；评分在尝试结束后加载标签。
- Ubuntu新建只读提交快照及临时Maven21容器，镜像摘要见manifest；2CPU/2GiB仅限制测试进程，Ollama仍为既有共享服务。源码tree及commit SHA均重建核对，没有复制.env或私人配置。
- 新Testcontainers PG17.11/pgvector0.8.6位于127.0.0.1:32769，库mirror_self_evaluation_qg_v1；只在该库执行Flyway V1–V5。未连接正式5432、已有测试库、业务卷或部署API。
- Java21.0.10/Maven3.9.13：5个契约单元+1个真实集成通过，0跳过。质量采集单调耗时28.879秒，Live IT32.81秒；冷构建及测试约526秒，不能作为生产吞吐测量。
- 临时Maven/PG/Ryuk已退出，按本次唯一标签确认剩余0，临时源码/缓存/结果目录已删除。没有临时Key文件，进程环境已结束；未删除模型权重或修改时钟。
- 准备阶段临时包装器语法错误在任何模型/元数据调用前修正；Mac报告解包兼容问题在推理结束后修正，两者均未重跑推理或变更批准源码。

## 两阶段实测

R@10为目标pair任一方向进入候选的比例；另有每anchor查询R@3/R@10，分母不混用。以下均为固定参考下的暂定成绩。

| 集合/模式 | 候选R@10 | 已召回分类P/R/F1 | 端到端P/R/F1 | 数值门禁 |
|---|---:|---|---|---|
| DEV KEYWORD | 25/30=83.33% | 100 / 100 / 100% | 100 / 83.33 / 90.91% | FAIL：候选召回 |
| DEV HYBRID | 30/30=100% | 100 / 83.33 / 90.91% | 100 / 83.33 / 90.91% | PASS，仅DEV条件 |
| HOLDOUT KEYWORD | 15/25=60% | 100 / 100 / 100% | 100 / 60 / 75% | FAIL：候选召回、关系R/F1 |
| HOLDOUT HYBRID | 25/25=100% | 100 / 64 / 78.05% | 100 / 64 / 78.05% | FAIL：关系R/F1 |

真实存量向量诊断R@10为55/55，HYBRID的pair及查询R@3在本小池内也均100%。这只说明本批正确目标均被召回，不能推广为大库/开放集100%召回；召回候选中仍有大量无预期关系的对，不能把高cosine当支持关系。

四模式错误SAME_AS、无关错误关系、owner/隐私/证据错误均0；无关关联率0%，机会分母分别60、339、82、364。HYBRID证据DEV55/55、HOLDOUT32/32；默认待审及冲突审核策略正确率100%。SQL断言identity_claim与memory_relation均0写入，没有自动确认人格、价值观或合并记忆。安全断言强制通过不等于召回/覆盖门禁通过，Maven使用观察模式所以可以BUILD SUCCESS。

## 逐例错误归因

DEV的QG0100–0104：KEYWORD没有找到目标，是词面召回遗漏；HYBRID与向量已找到，但有限跨语言对象归一化不能建立等价，属于规则分类弃答。不得称为BGE漏检。

HOLDOUT的QG1300–1304和QG2001–2004：HYBRID已全部召回，但完整句式/词汇不在当前规则覆盖内，9对被弃答。QG2000“patient/耐心”由现有通用别名识别，因此HYBRID比KEYWORD多保留1条关系；这不代表规则整体通过跨语言泛化。

| ID | 两条虚构记忆 | HYBRID分类弃答码 |
|---|---|---|
| QG0100 | 我不喜欢安静的图书馆。 / I dislike quiet libraries. | DIFFERENT_PROPOSITION_OR_CONTEXT |
| QG0101 | 我不喜欢慢节奏旅行。 / I dislike slow travel. | DIFFERENT_PROPOSITION_OR_CONTEXT |
| QG0102 | 我不喜欢手绘地图。 / I dislike handmade maps. | DIFFERENT_PROPOSITION_OR_CONTEXT |
| QG0103 | 我不喜欢深夜会议。 / I dislike late meetings. | DIFFERENT_PROPOSITION_OR_CONTEXT |
| QG0104 | 我不喜欢小组辩论。 / I dislike group debates. | DIFFERENT_PROPOSITION_OR_CONTEXT |
| QG1300 | 我倾向于独自阅读诗歌。 / I prefer reading poetry alone. | UNRESOLVED_COMPLETE_PROPOSITION |
| QG1301 | 我不爱参加喧闹的聚会。 / I avoid noisy parties. | UNRESOLVED_COMPLETE_PROPOSITION |
| QG2001 | 我觉得自己很好奇。 / I consider myself curious. | DIFFERENT_PROPOSITION_OR_CONTEXT |
| QG1302 | 我习惯先列提纲再写作。 / I usually outline before writing. | UNRESOLVED_COMPLETE_PROPOSITION |
| QG2002 | 我觉得自己很谨慎。 / I consider myself careful. | DIFFERENT_PROPOSITION_OR_CONTEXT |
| QG1303 | 我偏好可重复使用的容器。 / I prefer reusable containers. | UNRESOLVED_COMPLETE_PROPOSITION |
| QG2003 | 我觉得自己很健谈。 / I consider myself talkative. | DIFFERENT_PROPOSITION_OR_CONTEXT |
| QG1304 | 我讨厌没有议程的会议。 / I dislike meetings without an agenda. | UNRESOLVED_COMPLETE_PROPOSITION |
| QG2004 | 我觉得自己很灵活。 / I consider myself flexible. | DIFFERENT_PROPOSITION_OR_CONTEXT |

其余逐例命中、排名key、期望方向、实际关系、弃答及阶段标记保留在report.json和analysis.json。所有负例的保守弃答也按原因列出。

QG1301的“避免行为/不喜欢”参考歧义已在执行前标记待人审；本次仍按原SAME_AS计分，没有为了提高R而改标签。未来若人工修订，只能新版本并将重标注影响与模型影响分开。本留出现在已消费，不能用于后续调参后继续宣称独立盲测。

## Mock和历史结果的解释性对照

同一DEV上，受控全相同向量Mock与真实BGE的候选pair R@10都为100%，分类P/R/F1也相同，说明当前瓶颈在规则而非本批语义召回。Mock没有证明BGE提升，真实运行是独立条件记录。留出没有Mock成绩，不补造对照。

旧31对、v2/v3查询检索和DeepSeek抽取使用不同数据、单位和评分口径，所有原始文件与SHA保留，不能把本次100%pair召回与旧查询分数直接比较为模型提升。

## V0.3准入与下一步

`v03Ready=false`。HOLDOUT HYBRID R=64%低于80%、F1=78.05%低于85%；KEYWORD也不满足冻结召回标准，独立人工参考审核仍未完成。即使模型调用、工程回归及安全门禁通过，也不应冻结正式可信基线或自动进入九维推断。

下一步先独立人审76项旧参考与新120对的语义边界；再为通用双语命题规范/可解释NLI扩展设计新的开发与独立留出版本。不要针对本表硬编码，不降低当前阈值，不改当前参考。若改变KEYWORD对照门禁角色，必须事前建立新门禁契约并保留本次FAIL。关系图设计可继续审阅，发布/撤销/依赖测试仍需遵守独立授权与L2待审；本阶段没有实现完整Identity Model。
