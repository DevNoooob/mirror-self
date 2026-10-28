# 首次真实 Pilot：原始脱敏观测归档

状态：ARCHIVED_OBSERVATION_NOT_APPROVED_BASELINE。此目录不是 evaluation/baselines，不代表已冻结。

原始 report.json SHA-256：`7a235c7e76eb7db8e9956e0e8330d3cbe2d619dd6e1e871b14fa29907f448bb6`。report.json、report.md 与既有分析、执行、清理文件按字节保留；旧 5/7 重放指标与时间戳未被改写。

九例 Gold v1.1、代码 48bd913、模型 deepseek-flash、31 次真实调用及已知限制详见 analysis.md。模型服务未返回可固定的快照版本，不能把模型别名当成不可变权重版本。

后续离线修复不能补出历史重放日志，不能重新计算一个更漂亮的旧分数。新重放诊断使用独立 replay-1.0 合约；评分原契约保留。正式冻结方案见 [V0.2.1-C 冻结准备](../../../docs/V0.2.1-C_DIAGNOSTICS.md)。
