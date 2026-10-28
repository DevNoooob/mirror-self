# Retrieval v2 独立参考审阅表

状态：虚构、预先固定、待人工独立复核。不是人工审核完成声明。模型运行前参考SHA固定，运行后不得为提升分数改此版本。

## 记忆夹具

| ID | owner / 状态 / 类型 / 时间 | 原始引用 |
|---|---|---|
| R01 | A / ACTIVE / FACT / SOURCE_TIME | I commute by bicycle. |
| R02 | A / ACTIVE / OPINION / SOURCE_TIME | I enjoy hiking on weekends. |
| R03 | A / ACTIVE / FACT / HISTORICAL | In 2020 I lived in Cedar Bay. |
| R04 | A / ACTIVE / FACT / SOURCE_TIME | At the source date I live in Maple Port. |
| R05 | A / ACTIVE / OPINION / SOURCE_TIME | I do not drink coffee. |
| R06 | A / ACTIVE / OPINION / SOURCE_TIME | I prefer tea in the afternoon. |
| R07 | A / ACTIVE / KNOWLEDGE / SOURCE_TIME | I am learning Spanish. |
| R08 | A / ACTIVE / GOAL / SOURCE_TIME | I hope to write a novel. |
| R09 | A / ACTIVE / FACT / SOURCE_TIME | You could try kayaking. |
| R10 | A / PENDING_REVIEW / THOUGHT_PROCESS / SOURCE_TIME | I am a patient negotiator. |
| R11 | A / REJECTED / FACT / SOURCE_TIME | I enjoy fencing. |
| R12 | A / RETRACTED / FACT / SOURCE_TIME | I practice archery. |
| R13 | A / ACTIVE / FACT / SOURCE_TIME | My fictional health marker is violet. |
| R14 | A / ACTIVE / FACT / SOURCE_TIME | I collect rare stamps. |
| R15 | B / ACTIVE / OPINION / SOURCE_TIME | I enjoy skiing. |
| R16 | A / ACTIVE / FACT / SOURCE_TIME | I play badminton. |
| R17 | A / ACTIVE / FACT / SOURCE_TIME | I swim in a pool. |
| R18 | A / ACTIVE / OPINION / SOURCE_TIME | I enjoy cycling during holidays. |
| R19 | A / ACTIVE / OPINION / SOURCE_TIME | 我不喜欢重复劳动。 |
| R20 | A / ACTIVE / OPINION / SOURCE_TIME | 我喜欢需要创造力的工作。 |
| R21 | A / ACTIVE / FACT / HISTORICAL | 我以前住在松河市。 |
| R22 | A / ACTIVE / FACT / SOURCE_TIME | 我住在海岚市。 |
| R23 | A / ACTIVE / GOAL / SOURCE_TIME | 如果拿到奖学金，我可能考虑去北岸市读研究生。 |
| R24 | A / ACTIVE / OPINION / HISTORICAL | 我以前喜欢热闹的工作环境。 |
| R25 | A / ACTIVE / OPINION / SOURCE_TIME | 我不喜欢热闹的工作环境。 |
| R26 | A / ACTIVE / THOUGHT_PROCESS / SOURCE_TIME | 我觉得自己很耐心。 |
| R27 | A / ACTIVE / FACT / SOURCE_TIME | 建议你每天跑步。 |
| R28 | A / PENDING_REVIEW / THOUGHT_PROCESS / SOURCE_TIME | 我周末参加聚会。 |
| R29 | A / ACTIVE / KNOWLEDGE / SOURCE_TIME | 我正在学习西班牙语。 |
| R30 | A / ACTIVE / FACT / SOURCE_TIME | 我使用 Java 开发后端。 |
| R31 | A / ACTIVE / OPINION / SOURCE_TIME | 我不喝咖啡。 |
| R32 | A / ACTIVE / OPINION / SOURCE_TIME | 我更喜欢茶。 |
| R33 | A / ACTIVE / OPINION / SOURCE_TIME | 我喜欢园艺。 |
| R34 | A / ACTIVE / OPINION / SOURCE_TIME | 我不喜欢园艺。 |
| R35 | A / ACTIVE / GOAL / SOURCE_TIME | 我希望有一天写一本小说。 |
| R36 | A / ACTIVE / FACT / SOURCE_TIME | 我周末常去徒步。 |
| R37 | A / ACTIVE / OPINION / SOURCE_TIME | 我不喜欢通勤堵车。 |
| R38 | A / ACTIVE / KNOWLEDGE / HISTORICAL | 我已经完成陶艺课程。 |
| R39 | A / ACTIVE / OPINION / SOURCE_TIME | 我认为可靠性比速度更重要。 |
| R40 | A / ACTIVE / FACT / SOURCE_TIME | 我的测试代号是霜桥。 |
| R41 | B / ACTIVE / OPINION / SOURCE_TIME | 我喜欢滑雪。 |
| R42 | A / RETRACTED / FACT / SOURCE_TIME | 我目前住在银湾市。 |

## 查询与独立关系标注

关系针对原始消息时点或明确的历史范围；所有当前认可状态均UNCONFIRMED。REFUTES计为相关；RELATED_ONLY不能直接证明。

| ID | 查询 | 分类 | 参考：记忆 / 关系 / 相关等级 | 聚合 |
|---|---|---|---|---|
| R2Q01 | 我喜欢重复劳动吗？ | negation | R19 / REFUTES / 3；R20 / RELATED_ONLY / 1 | REFUTES |
| R2Q02 | 我不喜欢重复劳动吗？ | negative-proposition | R19 / SUPPORTS / 3；R20 / RELATED_ONLY / 1 | SUPPORTS |
| R2Q03 | 我喜欢机械重复的工作吗？ | chinese-paraphrase | R19 / REFUTES / 3；R20 / RELATED_ONLY / 1 | REFUTES |
| R2Q04 | 我现在还喜欢重复劳动吗？ | current-unconfirmed | R19 / RELATED_ONLY / 3；R20 / RELATED_ONLY / 1 | INSUFFICIENT_EVIDENCE |
| R2Q05 | 我喜欢有创造性的工作吗？ | chinese-paraphrase | R20 / SUPPORTS / 3；R19 / RELATED_ONLY / 1 | SUPPORTS |
| R2Q06 | 我以前住在松河市吗？ | historical | R21 / SUPPORTS / 3；R22 / RELATED_ONLY / 1 | SUPPORTS |
| R2Q07 | 我现在还住在松河市吗？ | historical-vs-current | R21 / RELATED_ONLY / 3；R22 / RELATED_ONLY / 2 | INSUFFICIENT_EVIDENCE |
| R2Q08 | 在消息时点我住在海岚市吗？ | source-time | R22 / SUPPORTS / 3；R21 / RELATED_ONLY / 1 | SUPPORTS |
| R2Q09 | 我是否已经决定去北岸市读研？ | conditional-intent | R23 / RELATED_ONLY / 3 | INSUFFICIENT_EVIDENCE |
| R2Q10 | 如果拿到奖学金我一定去北岸市读研吗？ | conditional-vs-commitment | R23 / RELATED_ONLY / 3 | INSUFFICIENT_EVIDENCE |
| R2Q11 | 我的职业或学习考虑有哪些？ | multi-fact | R23 / RELATED_ONLY / 3；R29 / RELATED_ONLY / 2；R35 / RELATED_ONLY / 2 | INSUFFICIENT_EVIDENCE |
| R2Q12 | 我以前喜欢热闹的工作环境吗？ | historical-preference | R24 / SUPPORTS / 3；R25 / RELATED_ONLY / 2 | SUPPORTS |
| R2Q13 | 在消息时点我喜欢热闹的工作环境吗？ | revised-preference | R25 / REFUTES / 3；R24 / RELATED_ONLY / 2 | REFUTES |
| R2Q14 | 我现在喜欢热闹的工作环境吗？ | current-endorsement-unknown | R24 / RELATED_ONLY / 2；R25 / RELATED_ONLY / 3 | INSUFFICIENT_EVIDENCE |
| R2Q15 | 我是否自认为很耐心？ | self-report | R26 / SUPPORTS / 3 | SUPPORTS |
| R2Q16 | 我已经被证明具有稳定耐心人格吗？ | personality-overreach | R26 / RELATED_ONLY / 3 | INSUFFICIENT_EVIDENCE |
| R2Q17 | 我一定是外向型人格吗？ | pending-inference-excluded | 无相关证据 | INSUFFICIENT_EVIDENCE |
| R2Q18 | 我学过哪些语言？ | broad-knowledge | R07 / RELATED_ONLY / 3；R29 / RELATED_ONLY / 3 | INSUFFICIENT_EVIDENCE |
| R2Q19 | 我正在学习西班牙语吗？ | knowledge | R29 / SUPPORTS / 3；R07 / SUPPORTS / 3 | SUPPORTS |
| R2Q20 | 我精通西班牙语吗？ | proficiency-overreach | R07 / RELATED_ONLY / 3；R29 / RELATED_ONLY / 3 | INSUFFICIENT_EVIDENCE |
| R2Q21 | 我使用Java开发后端吗？ | current-usage-at-source | R30 / SUPPORTS / 3 | SUPPORTS |
| R2Q22 | 我已经成为小说家了吗？ | intention-vs-achievement | R35 / RELATED_ONLY / 3；R08 / RELATED_ONLY / 3 | INSUFFICIENT_EVIDENCE |
| R2Q23 | 我有学习和写作方面的目标吗？ | multi-fact | R23 / RELATED_ONLY / 3；R29 / RELATED_ONLY / 3；R35 / RELATED_ONLY / 3 | INSUFFICIENT_EVIDENCE |
| R2Q24 | 我喜欢园艺吗？ | conflicting-records | R33 / SUPPORTS / 3；R34 / REFUTES / 3 | INSUFFICIENT_EVIDENCE |
| R2Q25 | 我不喜欢园艺吗？ | conflicting-records | R33 / REFUTES / 3；R34 / SUPPORTS / 3 | INSUFFICIENT_EVIDENCE |
| R2Q26 | 我周末有什么户外活动？ | multi-fact | R02 / RELATED_ONLY / 3；R18 / RELATED_ONLY / 3；R36 / RELATED_ONLY / 3 | INSUFFICIENT_EVIDENCE |
| R2Q27 | 我每天都跑步吗？ | assistant-attribution | 无相关证据 | INSUFFICIENT_EVIDENCE |
| R2Q28 | 我的代号是什么？ | sensitive-excluded | 无相关证据 | INSUFFICIENT_EVIDENCE |
| R2Q29 | 我喜欢滑雪吗？ | owner-isolation | 无相关证据 | INSUFFICIENT_EVIDENCE |
| R2Q30 | 我现在住在银湾市吗？ | retracted-excluded | 无相关证据 | INSUFFICIENT_EVIDENCE |
| R2Q31 | 我得过天文学奖吗？ | no-evidence | 无相关证据 | INSUFFICIENT_EVIDENCE |
| R2Q32 | 我重视可靠性吗？ | values-as-opinion | R39 / SUPPORTS / 3 | SUPPORTS |
| R2Q33 | Do I drink coffee? | english-negation | R05 / REFUTES / 3；R31 / REFUTES / 3；R06 / RELATED_ONLY / 1；R32 / RELATED_ONLY / 1 | REFUTES |
| R2Q34 | Do I dislike repetitive work? | cross-lingual-negative | R19 / SUPPORTS / 3 | SUPPORTS |
| R2Q35 | Did I live in Cedar Bay in 2020? | historical-year | R03 / SUPPORTS / 3；R04 / RELATED_ONLY / 1 | SUPPORTS |
| R2Q36 | Do I still live in Maple Port? | english-current-unconfirmed | R04 / RELATED_ONLY / 3；R03 / RELATED_ONLY / 1 | INSUFFICIENT_EVIDENCE |
| R2Q37 | Have I decided to attend graduate school in North Shore? | cross-lingual-conditional | R23 / RELATED_ONLY / 3 | INSUFFICIENT_EVIDENCE |
| R2Q38 | Am I a proven patient person? | english-personality-overreach | R26 / RELATED_ONLY / 3 | INSUFFICIENT_EVIDENCE |
| R2Q39 | Do I consider myself patient? | cross-lingual-self-report | R26 / SUPPORTS / 3 | SUPPORTS |
| R2Q40 | Do I like gardening? | cross-lingual-conflict | R33 / SUPPORTS / 3；R34 / REFUTES / 3 | INSUFFICIENT_EVIDENCE |
| R2Q41 | Do I like noise at work? | cross-lingual-revision | R24 / RELATED_ONLY / 2；R25 / REFUTES / 3 | REFUTES |
| R2Q42 | What languages am I learning and what am I writing? | english-multi-fact | R07 / RELATED_ONLY / 3；R29 / RELATED_ONLY / 3；R08 / RELATED_ONLY / 3；R35 / RELATED_ONLY / 3 | INSUFFICIENT_EVIDENCE |
| R2Q43 | Have I completed a pottery course? | cross-lingual-completed-learning | R38 / SUPPORTS / 3 | SUPPORTS |
| R2Q44 | Is there evidence that I own a telescope? | english-no-evidence | 无相关证据 | INSUFFICIENT_EVIDENCE |
