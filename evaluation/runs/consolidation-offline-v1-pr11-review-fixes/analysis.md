# PR #11 合并前修复：独立离线回归

原31对数据、首次离线报告、V4及v2/v3文件保持逐字节不变。此目录为独立reportVersion `consolidation-offline-v1-pr11-review-fixes`；没有新Gold，没有真实模型调用或正式质量基线。

执行head `7b7dba4e7a6406a6fd8aab53dd7db64f71c613a6`，见 [CI 41036371289](https://github.com/DevNoooob/mirror-self/actions/runs/41036371289)。Java21单元131通过，隔离集成58通过、3个真实模型入口按配置跳过，Python预检15通过。新增8项回归全部通过。

## 已确认原因与修复证据

- P1：旧关键词路先按通用资格排序并截断100，再过滤USER_EXPLICIT；110条ACTIVE MODEL_INFERRED的高得分项确实能使低词面得分的合格显式项消失。专用SQL现在先过滤完整整合资格并排除自身，Top1即可返回目标；通用检索仍可返回已审核推断，未改retrieval/ask规则。
- 回归分别覆盖撤回、SENSITIVE/SECRET、跨owner、RESTRICTED来源、assistant、跨owner证据、伪造和缺失引用；KEYWORD与存量向量进入RRF前都只保留合格项，扫描请求模型数为0。
- P2：V4唯一键包含relation_type，原写入幂等仅覆盖同类型。当前保留不同假设，但详情/列表显式标记RELATION_TYPE_CONFLICT、approvalBlocked及冲突条目。相同owner/记忆对/revision的未拒绝有效异类关系阻断双方批准；双记忆锁和SQL反查保证并发API不会分别批准。
- 旧引擎SAME_AS→新SUPPORTS，以及旧SUPPORTS→新SAME_AS均通过冲突回归；先显式拒绝一方才允许另一方批准。已APPROVED历史保持不变，不自动覆盖或撤销；新冲突保持待审。API冲突返回409，来源/状态失效后不暴露旧冲突。
- 新pipeline版本纳入游标键，旧扫描可被显式有界任务重访，不修改L1修订，不自动扫描正式服务。V4/数据库约束未修改，无V5或正式迁移。

## 31对受控Mock复验

TP8 / FP0 / FN1，Precision100%、Recall88.89%，错误SAME_AS0/3，证据17/17、待审策略8/8。成绩与首次观察相同；CC16有限跨语言词汇漏检仍保留，不改参考。全部向量故意相同，不能作为BGE语义质量提升。8项新增工程回归单独计数，不混入31对指标。

## 风险与边界

不同关系一律保守视作竞争假设，可能增加人工处理负担；当前不自动判断某些关系是否兼容。对已批准历史的纠正及旧数据中此前存在的双APPROVED冲突，只显示争议，不自动撤销，需要单独人工处理。原始SQL直写不属于API锁与批准SQL的保证范围，没有新增数据库触发器或RLS。冲突元数据随当前有效状态重新计算，approvalBlocked只表示关系冲突，不代表REJECTED/APPROVED记录可重新审核。

正式部署、数据库操作、付费/本地真实模型测试均未执行。保持独立PR待审，不自动合并。
