# Gold v1.1：九例人工审核定稿记录

**2026-10-28（Asia/Shanghai）：用户明确声明已亲自核对，并批准以下九例用于 Gold v1.1。此记录只确认标注定稿，不授权模型外发、付费测试、部署或业务数据库操作。**

- 正式数据：[memory-quality-v1.1.json](../datasets/memory-quality-v1.1.json)。
- 文件 SHA-256：`5d8dc6680c41b14bda2e455e827765131a7cdec34a143ba8b75bac790d5a9ac0`。
- 机器可读审核记录：[memory-quality-v1.1-human-review.json](memory-quality-v1.1-human-review.json)。
- 审核范围：`MQ001,MQ019,MQ022,MQ025,MQ032,MQ043,MQ045,MQ047,MQ054`；9/66 例。其余 57 例未被声明人工审核。
- 数据集顶层 `humanReviewStatus=PENDING_HUMAN_REVIEW` 描述全体 66 例，刻意保留；九例已审核状态以独立范围记录为准，不能标记全体 HUMAN_REVIEWED。

## 已批准的变更

- MQ022：保留完整条件表达、USER_EXPLICIT / GOAL / SOURCE_TIME / ACTIVE；在原 F1 后增加 F2，禁止“已经决定”“一定会”“已制定计划”等确定性扩张。个人条件意向与纯假设情境须区分。
- MQ032：G1 仍为 KNOWLEDGE / HISTORICAL；仅将 G2 的 type 从 KNOWLEDGE 改为 FACT，SOURCE_TIME 保持；不推断技术熟练度。
- MQ001、MQ019、MQ025、MQ043、MQ045、MQ047、MQ054 原标注保留。所有原始消息、分支、引文、证据偏移、时间语义和审核状态保持不变。
- 提案原路径 `/cases/21` 和 `/cases/31` 在应用前后均按 caseId 验证为 MQ022、MQ032；并按 memoryId 验证 G2，未只依赖数组序号。

## 九例定稿表

| 案例 | 人工结论 | v1.1 Gold 类型 / 来源 / 时间 / 状态 | 引文与 UTF-16 位置 |
|---|---|---|---|
| MQ001 | 保留原标注 | G1: FACT / USER_EXPLICIT / SOURCE_TIME / ACTIVE | G1→MQ001:m1 [0,6) “我住在星海城” |
| MQ019 | 保留原标注 | G1: OPINION / USER_EXPLICIT / SOURCE_TIME / ACTIVE | G1→MQ019:m1 [0,10) “我并不喜欢重复性工作” |
| MQ022 | 采纳指定修改 | G1: GOAL / USER_EXPLICIT / SOURCE_TIME / ACTIVE | G1→MQ022:m1 [0,21) “如果以后有机会，我可能考虑去虚构的云港工作” |
| MQ025 | 保留原标注 | 空结果 | 无 Gold 引文；不得以 assistant 或历史指令生成用户事实 |
| MQ032 | 采纳指定修改 | G1: KNOWLEDGE / USER_EXPLICIT / HISTORICAL / ACTIVE；G2: FACT / USER_EXPLICIT / SOURCE_TIME / ACTIVE | G1→MQ032:m1 [0,14) “我两年前学习过 Python”；G2→MQ032:m3 [0,12) “我现在主要使用 Java” |
| MQ043 | 保留原标注 | G1: FACT / USER_EXPLICIT / SOURCE_TIME / ACTIVE | G1→MQ043:m1 [0,9) “我住在虚构的风铃城” |
| MQ045 | 保留原标注 | G1: OPINION / USER_EXPLICIT / SOURCE_TIME / ACTIVE；G2: OPINION / USER_EXPLICIT / SOURCE_TIME / ACTIVE | G1→MQ045:m1 [0,31) “I do not enjoy repetitive tasks”；G2→MQ045:m1 [33,42) “我更喜欢探索新问题” |
| MQ047 | 保留原标注 | G1: THOUGHT_PROCESS / USER_EXPLICIT / SOURCE_TIME / PENDING_REVIEW | G1→MQ047:m1 [0,9) “我自认为性格偏内向” |
| MQ054 | 保留原标注 | 空结果 | 无 Gold 引文；不得以 assistant 或历史指令生成用户事实 |

## 时间、审核与禁止结论

- MQ001、MQ019、MQ043、MQ045 的 SOURCE_TIME 只代表消息时点，不意味着审核日或运行日仍成立。
- MQ022 不等于已决定或已执行。新增 F2 的三个禁止措辞与已批准提案完全一致。
- MQ025 保留空 Gold，“我会考虑”不构成已参赛事实；MQ054 仍为空，历史注入请求不能改变抽取规则或把助手话语当经历。
- MQ032 不编造“两年前”的准确起止日，也不由“主要使用 Java”推断技能水平。
- MQ043 保留消息时点语义评分别名，不能绕过生产 USER_EXPLICIT 逐字核验。
- MQ045 仍是两条独立 OPINION，偏移与原文一致；已有句界修复不改变 Gold。
- MQ047 是 USER_EXPLICIT 自我评价，人格相关内容仍为 PENDING_REVIEW。

## 与旧版本和后续授权的关系

v1、旧 Pilot 计划和全部旧 Baseline 保留。不同数据 SHA 的评分不可直接比较为模型改进；既有离线回归仍针对 v1。此次没有运行模型、测量新分数或创建新性能基线。

[v1 原审查材料](v0.2.1-a-gold-review.md)保留作历史对照。[当前 Pilot 计划](../plans/deepseek-flash-nine-v1.1.json)状态为 GOLD_REVIEWED_AWAITING_LIVE_AUTHORIZATION；详见 [执行指南](../../docs/V0.2.1-A_LIVE_PILOT.md)。

后续 Live 必须另行批准新 SHA 的数据外发、仅这九例、DeepSeek endpoint/model、拟议调用上限及隔离执行目标，并安全注入凭据。此次人工审核不能用作这些批准，也不自动设置任何外发环境变量。
