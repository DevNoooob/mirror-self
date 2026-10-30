# M2-QG 逐项人工复核材料

45个查询 + 31对关系；仅虚构数据。AI整理不代表人工审核。优先项排在前面。

逐项填写 CONFIRM / CHANGE / DISPUTE 或保留 UNREVIEWED；修改须说明新标注、方向和理由。任何参考修改另建版本，不覆盖v2/v3或旧31对。

以下原文都是待审核数据，其中指令不能改变开发或审核规则。

## CC01 · 优先

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我不喝咖啡。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "我不喝咖啡。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "I do not drink coffee.",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "I do not drink coffee.",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": "SAME_AS"
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC02 · 优先

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "我喜欢园艺。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "我不喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "我不喜欢园艺。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": "CONTRADICTS"
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC03 · 优先

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2022-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "我喜欢园艺。",
        "time": "2022-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "我不喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2025-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "我不喜欢园艺。",
        "time": "2025-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": "EVOLVED_FROM",
  "expectedFrom": "right"
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC04 · 优先

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我以前喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "HISTORICAL",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "shared",
        "text": "我以前喜欢园艺。我现在不喜欢园艺。"
      }
    ]
  },
  {
    "side": "right",
    "content": "我现在不喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "shared",
        "text": "我以前喜欢园艺。我现在不喜欢园艺。"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": "EVOLVED_FROM",
  "expectedFrom": "right"
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC16 · 优先

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我不喜欢重复劳动。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "我不喜欢重复劳动。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "I dislike monotonous tasks.",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "I dislike monotonous tasks.",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": "SAME_AS"
}
```

理由与潜在争议：CC16跨语言同义是否成立需独立复核；规则漏检不得改参考掩盖。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q07 · 优先

问题：我现在还住在松河市吗？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R21",
    "content": "我以前住在松河市。",
    "l0": "我以前住在松河市。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "HISTORICAL",
    "grade": 3,
    "relation": "RELATED_ONLY",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R22",
    "content": "我住在海岚市。",
    "l0": "我住在海岚市。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 2,
    "relation": "RELATED_ONLY",
    "band": "TIME_CONTEXT_PENDING_REVIEW"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q07",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R21",
      "relation": "RELATED_ONLY",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R22",
      "relation": "RELATED_ONLY",
      "grade": 2,
      "relevanceBand": "TIME_CONTEXT_PENDING_REVIEW",
      "humanReviewStatus": "PENDING_TIME_CONTEXT_REVIEW"
    }
  ],
  "expectedAggregate": "INSUFFICIENT_EVIDENCE",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q07",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：grade2时间背景不能替代直接证据；SOURCE_TIME不证明现在仍认可。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q11 · 优先

问题：我是否明确表示在获得奖学金的条件下，可能考虑去北岸市读研？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R23",
    "content": "如果拿到奖学金，我可能考虑去北岸市读研究生。",
    "l0": "如果拿到奖学金，我可能考虑去北岸市读研究生。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "SUPPORTS",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q11",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R23",
      "relation": "SUPPORTS",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "SUPPORTS",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q11",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q12 · 优先

问题：我以前喜欢热闹的工作环境吗？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R24",
    "content": "我以前喜欢热闹的工作环境。",
    "l0": "我以前喜欢热闹的工作环境。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2022-04-01T00:00:00Z",
    "scope": "HISTORICAL",
    "grade": 3,
    "relation": "SUPPORTS",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R25",
    "content": "我不喜欢热闹的工作环境。",
    "l0": "我不喜欢热闹的工作环境。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2025-01-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 2,
    "relation": "RELATED_ONLY",
    "band": "TIME_CONTEXT_PENDING_REVIEW"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q12",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R24",
      "relation": "SUPPORTS",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R25",
      "relation": "RELATED_ONLY",
      "grade": 2,
      "relevanceBand": "TIME_CONTEXT_PENDING_REVIEW",
      "humanReviewStatus": "PENDING_TIME_CONTEXT_REVIEW"
    }
  ],
  "expectedAggregate": "SUPPORTS",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q12",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：grade2时间背景不能替代直接证据；SOURCE_TIME不证明现在仍认可。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q13 · 优先

问题：根据 2025-01-01 的用户消息，当时我喜欢热闹的工作环境吗？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R25",
    "content": "我不喜欢热闹的工作环境。",
    "l0": "我不喜欢热闹的工作环境。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2025-01-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "REFUTES",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R24",
    "content": "我以前喜欢热闹的工作环境。",
    "l0": "我以前喜欢热闹的工作环境。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2022-04-01T00:00:00Z",
    "scope": "HISTORICAL",
    "grade": 2,
    "relation": "RELATED_ONLY",
    "band": "TIME_CONTEXT_PENDING_REVIEW"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q13",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R25",
      "relation": "REFUTES",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R24",
      "relation": "RELATED_ONLY",
      "grade": 2,
      "relevanceBand": "TIME_CONTEXT_PENDING_REVIEW",
      "humanReviewStatus": "PENDING_TIME_CONTEXT_REVIEW"
    }
  ],
  "expectedAggregate": "REFUTES",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q13",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：grade2时间背景不能替代直接证据；SOURCE_TIME不证明现在仍认可。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q14 · 优先

问题：我现在喜欢热闹的工作环境吗？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R24",
    "content": "我以前喜欢热闹的工作环境。",
    "l0": "我以前喜欢热闹的工作环境。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2022-04-01T00:00:00Z",
    "scope": "HISTORICAL",
    "grade": 2,
    "relation": "RELATED_ONLY",
    "band": "TIME_CONTEXT_PENDING_REVIEW"
  },
  {
    "memoryId": "R25",
    "content": "我不喜欢热闹的工作环境。",
    "l0": "我不喜欢热闹的工作环境。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2025-01-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "RELATED_ONLY",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q14",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R24",
      "relation": "RELATED_ONLY",
      "grade": 2,
      "relevanceBand": "TIME_CONTEXT_PENDING_REVIEW",
      "humanReviewStatus": "PENDING_TIME_CONTEXT_REVIEW"
    },
    {
      "memoryId": "R25",
      "relation": "RELATED_ONLY",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "INSUFFICIENT_EVIDENCE",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q14",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：grade2时间背景不能替代直接证据；SOURCE_TIME不证明现在仍认可。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q23 · 优先

问题：我是否表达过未来写小说的愿望？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R08",
    "content": "I hope to write a novel.",
    "l0": "I hope to write a novel.",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-05-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "SUPPORTS",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R35",
    "content": "我希望有一天写一本小说。",
    "l0": "我希望有一天写一本小说。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "SUPPORTS",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q23",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R08",
      "relation": "SUPPORTS",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R35",
      "relation": "SUPPORTS",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "SUPPORTS",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q23",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q41 · 优先

问题：Do I like noise at work?

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R24",
    "content": "我以前喜欢热闹的工作环境。",
    "l0": "我以前喜欢热闹的工作环境。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2022-04-01T00:00:00Z",
    "scope": "HISTORICAL",
    "grade": 2,
    "relation": "RELATED_ONLY",
    "band": "TIME_CONTEXT_PENDING_REVIEW"
  },
  {
    "memoryId": "R25",
    "content": "我不喜欢热闹的工作环境。",
    "l0": "我不喜欢热闹的工作环境。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2025-01-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "REFUTES",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q41",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R24",
      "relation": "RELATED_ONLY",
      "grade": 2,
      "relevanceBand": "TIME_CONTEXT_PENDING_REVIEW",
      "humanReviewStatus": "PENDING_TIME_CONTEXT_REVIEW"
    },
    {
      "memoryId": "R25",
      "relation": "REFUTES",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "REFUTES",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q41",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：grade2时间背景不能替代直接证据；SOURCE_TIME不证明现在仍认可。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q42A · 优先

问题：At the recorded historical message times, was I learning Spanish?

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R07",
    "content": "I am learning Spanish.",
    "l0": "I am learning Spanish.",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-05-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "SUPPORTS",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R29",
    "content": "我正在学习西班牙语。",
    "l0": "我正在学习西班牙语。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "SUPPORTS",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q42A",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R07",
      "relation": "SUPPORTS",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R29",
      "relation": "SUPPORTS",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "SUPPORTS",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q42",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q42B · 优先

问题：Had I expressed a wish to write a novel?

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R08",
    "content": "I hope to write a novel.",
    "l0": "I hope to write a novel.",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-05-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "SUPPORTS",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R35",
    "content": "我希望有一天写一本小说。",
    "l0": "我希望有一天写一本小说。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "SUPPORTS",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q42B",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R08",
      "relation": "SUPPORTS",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R35",
      "relation": "SUPPORTS",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "SUPPORTS",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q42",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC05

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我喜欢陶艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "shared",
        "text": "我喜欢陶艺。我不喝咖啡。"
      }
    ]
  },
  {
    "side": "right",
    "content": "我不喝咖啡。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "shared",
        "text": "我喜欢陶艺。我不喝咖啡。"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": null
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC06

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "如果获得录取，我可能去月港学习。",
    "type": "GOAL",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "如果获得录取，我可能去月港学习。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "我已经完成月港课程。",
    "type": "EVENT",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "我已经完成月港课程。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": null
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC07

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "如果获得录取，我可能去月港学习。",
    "type": "GOAL",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "如果获得录取，我可能去月港学习。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "如果获得录取，我可能去月港学习。",
    "type": "GOAL",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "如果获得录取，我可能去月港学习。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": "SAME_AS"
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC08

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "如果获得录取，我可能去月港学习。",
    "type": "GOAL",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "如果获得录取，我可能去月港学习。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "如果得到资助，我可能去月港学习。",
    "type": "GOAL",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "如果得到资助，我可能去月港学习。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": null
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC09

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我觉得自己很耐心。",
    "type": "THOUGHT_PROCESS",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "我觉得自己很耐心。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "我是稳定的耐心型人格。",
    "type": "FACT",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "我是稳定的耐心型人格。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": null
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC10

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我觉得自己很耐心。",
    "type": "THOUGHT_PROCESS",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "我觉得自己很耐心。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "I consider myself patient.",
    "type": "THOUGHT_PROCESS",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "I consider myself patient.",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": "SAME_AS"
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC11

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我不喝咖啡。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "shared",
        "text": "我不喝咖啡。"
      }
    ]
  },
  {
    "side": "right",
    "content": "I do not drink coffee.",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "shared",
        "text": "我不喝咖啡。"
      },
      {
        "key": "extra",
        "text": "I do not drink coffee."
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": "SUPPORTS",
  "expectedFrom": "right"
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC12

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我不喝咖啡。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2022-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "我不喝咖啡。",
        "time": "2022-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "I do not drink coffee.",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2025-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "I do not drink coffee.",
        "time": "2025-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": "SUPPORTS",
  "expectedFrom": "right"
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC13

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": null,
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "我喜欢园艺。",
        "time": null
      }
    ]
  },
  {
    "side": "right",
    "content": "我不喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": null,
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "我不喜欢园艺。",
        "time": null
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": null
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC14

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我使用Java。",
    "type": "FACT",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "我使用Java。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "我使用Java。",
    "type": "KNOWLEDGE",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "我使用Java。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": null
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC15

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "In 2018 I lived in Amber Town.",
    "type": "FACT",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "HISTORICAL",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "In 2018 I lived in Amber Town.",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "In 2020 I lived in Amber Town.",
    "type": "FACT",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "HISTORICAL",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "In 2020 I lived in Amber Town.",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": null
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC17

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "I like coffee at work.",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "I like coffee at work.",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "I do not like coffee at home.",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "I do not like coffee at home.",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": null
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC18

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "我喜欢园艺。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "我喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "assistant",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "我喜欢园艺。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": null
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC19

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "我喜欢园艺。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "我喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "B",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "我喜欢园艺。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": null
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC20

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "我喜欢园艺。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "我喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "SECRET",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "我喜欢园艺。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": null
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC21

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "我喜欢园艺。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "我喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "RESTRICTED",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "我喜欢园艺。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": null
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC22

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我觉得自己很耐心。",
    "type": "THOUGHT_PROCESS",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "我觉得自己很耐心。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "我觉得自己很耐心。",
    "type": "THOUGHT_PROCESS",
    "status": "PENDING_REVIEW",
    "origin": "MODEL_INFERRED",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "我觉得自己很耐心。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": null
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC23

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "我喜欢园艺。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "我喜欢园艺。",
    "type": "OPINION",
    "status": "REJECTED",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "我喜欢园艺。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": null
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC24

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "我喜欢园艺。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "我喜欢园艺。",
    "type": "OPINION",
    "status": "RETRACTED",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "我喜欢园艺。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": null
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC25

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "我喜欢园艺。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "我喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": true,
    "l0": [
      {
        "key": "right",
        "text": "我喜欢园艺。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": null
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC26

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "我喜欢园艺。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "我喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "bad",
        "text": "我不喜欢园艺。"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": null
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC27

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "我喜欢园艺。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "我并非不喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "我并非不喜欢园艺。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": null
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC28

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "我喜欢园艺。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "我喜欢园艺并且喜欢陶艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "我喜欢园艺并且喜欢陶艺。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": null
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC29

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "我喜欢园艺。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "我的朋友喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "我的朋友喜欢园艺。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": null
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC30

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我可能写小说。",
    "type": "GOAL",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "我可能写小说。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "我希望写小说。",
    "type": "GOAL",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "right",
        "text": "我希望写小说。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": null
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## CC31

L1、L0、消息时间与来源性质：

```json
[
  {
    "side": "left",
    "content": "我喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "left",
        "text": "我喜欢园艺。",
        "time": "2024-03-01T00:00:00Z"
      }
    ]
  },
  {
    "side": "right",
    "content": "我喜欢园艺。",
    "type": "OPINION",
    "status": "ACTIVE",
    "origin": "USER_EXPLICIT",
    "scope": "SOURCE_TIME",
    "time": "2024-03-01T00:00:00Z",
    "owner": "A",
    "role": "user",
    "privacy": "PRIVATE",
    "sensitivity": "NORMAL",
    "deleted": false,
    "l0": [
      {
        "key": "wrong-time",
        "text": "我喜欢园艺。",
        "time": "2020-01-01T00:00:00Z"
      }
    ]
  }
]
```

现有参考（待独立确认）：
```json
{
  "expectedRelation": null
}
```

理由与潜在争议：同义、反证、跨时演化要区分；方向取决于时间/证据集合；人格仅自述，所有关系仍待审。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q01

问题：我喜欢重复劳动吗？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R19",
    "content": "我不喜欢重复劳动。",
    "l0": "我不喜欢重复劳动。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "REFUTES",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R20",
    "content": "我喜欢需要创造力的工作。",
    "l0": "我喜欢需要创造力的工作。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 1,
    "relation": "RELATED_ONLY",
    "band": "BACKGROUND"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q01",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R19",
      "relation": "REFUTES",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R20",
      "relation": "RELATED_ONLY",
      "grade": 1,
      "relevanceBand": "BACKGROUND",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "REFUTES",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q01",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q02

问题：我不喜欢重复劳动吗？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R19",
    "content": "我不喜欢重复劳动。",
    "l0": "我不喜欢重复劳动。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "SUPPORTS",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R20",
    "content": "我喜欢需要创造力的工作。",
    "l0": "我喜欢需要创造力的工作。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 1,
    "relation": "RELATED_ONLY",
    "band": "BACKGROUND"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q02",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R19",
      "relation": "SUPPORTS",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R20",
      "relation": "RELATED_ONLY",
      "grade": 1,
      "relevanceBand": "BACKGROUND",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "SUPPORTS",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q02",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q03

问题：我喜欢机械重复的工作吗？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R19",
    "content": "我不喜欢重复劳动。",
    "l0": "我不喜欢重复劳动。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "REFUTES",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R20",
    "content": "我喜欢需要创造力的工作。",
    "l0": "我喜欢需要创造力的工作。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 1,
    "relation": "RELATED_ONLY",
    "band": "BACKGROUND"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q03",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R19",
      "relation": "REFUTES",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R20",
      "relation": "RELATED_ONLY",
      "grade": 1,
      "relevanceBand": "BACKGROUND",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "REFUTES",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q03",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q04

问题：我现在还喜欢重复劳动吗？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R19",
    "content": "我不喜欢重复劳动。",
    "l0": "我不喜欢重复劳动。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "RELATED_ONLY",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R20",
    "content": "我喜欢需要创造力的工作。",
    "l0": "我喜欢需要创造力的工作。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 1,
    "relation": "RELATED_ONLY",
    "band": "BACKGROUND"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q04",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R19",
      "relation": "RELATED_ONLY",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R20",
      "relation": "RELATED_ONLY",
      "grade": 1,
      "relevanceBand": "BACKGROUND",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "INSUFFICIENT_EVIDENCE",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q04",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q05

问题：我喜欢有创造性的工作吗？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R20",
    "content": "我喜欢需要创造力的工作。",
    "l0": "我喜欢需要创造力的工作。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "SUPPORTS",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R19",
    "content": "我不喜欢重复劳动。",
    "l0": "我不喜欢重复劳动。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 1,
    "relation": "RELATED_ONLY",
    "band": "BACKGROUND"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q05",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R20",
      "relation": "SUPPORTS",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R19",
      "relation": "RELATED_ONLY",
      "grade": 1,
      "relevanceBand": "BACKGROUND",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "SUPPORTS",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q05",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q06

问题：我以前住在松河市吗？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R21",
    "content": "我以前住在松河市。",
    "l0": "我以前住在松河市。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "HISTORICAL",
    "grade": 3,
    "relation": "SUPPORTS",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R22",
    "content": "我住在海岚市。",
    "l0": "我住在海岚市。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 1,
    "relation": "RELATED_ONLY",
    "band": "BACKGROUND"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q06",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R21",
      "relation": "SUPPORTS",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R22",
      "relation": "RELATED_ONLY",
      "grade": 1,
      "relevanceBand": "BACKGROUND",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "SUPPORTS",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q06",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q08

问题：在消息时点我住在海岚市吗？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R22",
    "content": "我住在海岚市。",
    "l0": "我住在海岚市。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "SUPPORTS",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R21",
    "content": "我以前住在松河市。",
    "l0": "我以前住在松河市。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "HISTORICAL",
    "grade": 1,
    "relation": "RELATED_ONLY",
    "band": "BACKGROUND"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q08",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R22",
      "relation": "SUPPORTS",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R21",
      "relation": "RELATED_ONLY",
      "grade": 1,
      "relevanceBand": "BACKGROUND",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "SUPPORTS",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q08",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q09

问题：我是否已经决定去北岸市读研？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R23",
    "content": "如果拿到奖学金，我可能考虑去北岸市读研究生。",
    "l0": "如果拿到奖学金，我可能考虑去北岸市读研究生。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "RELATED_ONLY",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q09",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R23",
      "relation": "RELATED_ONLY",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "INSUFFICIENT_EVIDENCE",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q09",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q10

问题：如果拿到奖学金我一定去北岸市读研吗？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R23",
    "content": "如果拿到奖学金，我可能考虑去北岸市读研究生。",
    "l0": "如果拿到奖学金，我可能考虑去北岸市读研究生。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "RELATED_ONLY",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q10",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R23",
      "relation": "RELATED_ONLY",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "INSUFFICIENT_EVIDENCE",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q10",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q15

问题：我是否自认为很耐心？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R26",
    "content": "我觉得自己很耐心。",
    "l0": "我觉得自己很耐心。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "SUPPORTS",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q15",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R26",
      "relation": "SUPPORTS",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "SUPPORTS",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q15",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q16

问题：我已经被证明具有稳定耐心人格吗？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R26",
    "content": "我觉得自己很耐心。",
    "l0": "我觉得自己很耐心。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "RELATED_ONLY",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q16",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R26",
      "relation": "RELATED_ONLY",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "INSUFFICIENT_EVIDENCE",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q16",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q17

问题：我一定是外向型人格吗？

L1、L0、消息时间与来源性质：

```json
[]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q17",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [],
  "expectedAggregate": "INSUFFICIENT_EVIDENCE",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q17",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q18

问题：我学过哪些语言？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R07",
    "content": "I am learning Spanish.",
    "l0": "I am learning Spanish.",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-05-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "RELATED_ONLY",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R29",
    "content": "我正在学习西班牙语。",
    "l0": "我正在学习西班牙语。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "RELATED_ONLY",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q18",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R07",
      "relation": "RELATED_ONLY",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R29",
      "relation": "RELATED_ONLY",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "INSUFFICIENT_EVIDENCE",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q18",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q19

问题：我正在学习西班牙语吗？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R29",
    "content": "我正在学习西班牙语。",
    "l0": "我正在学习西班牙语。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "SUPPORTS",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R07",
    "content": "I am learning Spanish.",
    "l0": "I am learning Spanish.",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-05-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "SUPPORTS",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q19",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R29",
      "relation": "SUPPORTS",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R07",
      "relation": "SUPPORTS",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "SUPPORTS",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q19",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q20

问题：我精通西班牙语吗？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R07",
    "content": "I am learning Spanish.",
    "l0": "I am learning Spanish.",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-05-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "RELATED_ONLY",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R29",
    "content": "我正在学习西班牙语。",
    "l0": "我正在学习西班牙语。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "RELATED_ONLY",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q20",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R07",
      "relation": "RELATED_ONLY",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R29",
      "relation": "RELATED_ONLY",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "INSUFFICIENT_EVIDENCE",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q20",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q21

问题：我使用Java开发后端吗？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R30",
    "content": "我使用 Java 开发后端。",
    "l0": "我使用 Java 开发后端。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "SUPPORTS",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q21",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R30",
      "relation": "SUPPORTS",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "SUPPORTS",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q21",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q22

问题：我已经成为小说家了吗？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R35",
    "content": "我希望有一天写一本小说。",
    "l0": "我希望有一天写一本小说。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "RELATED_ONLY",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R08",
    "content": "I hope to write a novel.",
    "l0": "I hope to write a novel.",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-05-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "RELATED_ONLY",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q22",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R35",
      "relation": "RELATED_ONLY",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R08",
      "relation": "RELATED_ONLY",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "INSUFFICIENT_EVIDENCE",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q22",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q24

问题：我喜欢园艺吗？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R33",
    "content": "我喜欢园艺。",
    "l0": "我喜欢园艺。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "SUPPORTS",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R34",
    "content": "我不喜欢园艺。",
    "l0": "我不喜欢园艺。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "REFUTES",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q24",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R33",
      "relation": "SUPPORTS",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R34",
      "relation": "REFUTES",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "INSUFFICIENT_EVIDENCE",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q24",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q25

问题：我不喜欢园艺吗？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R33",
    "content": "我喜欢园艺。",
    "l0": "我喜欢园艺。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "REFUTES",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R34",
    "content": "我不喜欢园艺。",
    "l0": "我不喜欢园艺。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "SUPPORTS",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q25",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R33",
      "relation": "REFUTES",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R34",
      "relation": "SUPPORTS",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "INSUFFICIENT_EVIDENCE",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q25",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q26

问题：我周末有什么户外活动？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R02",
    "content": "I enjoy hiking on weekends.",
    "l0": "I enjoy hiking on weekends.",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-05-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "RELATED_ONLY",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R18",
    "content": "I enjoy cycling during holidays.",
    "l0": "I enjoy cycling during holidays.",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-05-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "RELATED_ONLY",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R36",
    "content": "我周末常去徒步。",
    "l0": "我周末常去徒步。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "RELATED_ONLY",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q26",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R02",
      "relation": "RELATED_ONLY",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R18",
      "relation": "RELATED_ONLY",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R36",
      "relation": "RELATED_ONLY",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "INSUFFICIENT_EVIDENCE",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q26",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q27

问题：我每天都跑步吗？

L1、L0、消息时间与来源性质：

```json
[]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q27",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [],
  "expectedAggregate": "INSUFFICIENT_EVIDENCE",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q27",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q28

问题：我的代号是什么？

L1、L0、消息时间与来源性质：

```json
[]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q28",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [],
  "expectedAggregate": "INSUFFICIENT_EVIDENCE",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q28",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q29

问题：我喜欢滑雪吗？

L1、L0、消息时间与来源性质：

```json
[]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q29",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [],
  "expectedAggregate": "INSUFFICIENT_EVIDENCE",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q29",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q30

问题：我现在住在银湾市吗？

L1、L0、消息时间与来源性质：

```json
[]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q30",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [],
  "expectedAggregate": "INSUFFICIENT_EVIDENCE",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q30",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q31

问题：我得过天文学奖吗？

L1、L0、消息时间与来源性质：

```json
[]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q31",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [],
  "expectedAggregate": "INSUFFICIENT_EVIDENCE",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q31",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q32

问题：我重视可靠性吗？

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R39",
    "content": "我认为可靠性比速度更重要。",
    "l0": "我认为可靠性比速度更重要。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "SUPPORTS",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q32",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R39",
      "relation": "SUPPORTS",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "SUPPORTS",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q32",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q33

问题：Do I drink coffee?

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R05",
    "content": "I do not drink coffee.",
    "l0": "I do not drink coffee.",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-05-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "REFUTES",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R31",
    "content": "我不喝咖啡。",
    "l0": "我不喝咖啡。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "REFUTES",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R06",
    "content": "I prefer tea in the afternoon.",
    "l0": "I prefer tea in the afternoon.",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-05-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 1,
    "relation": "RELATED_ONLY",
    "band": "BACKGROUND"
  },
  {
    "memoryId": "R32",
    "content": "我更喜欢茶。",
    "l0": "我更喜欢茶。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 1,
    "relation": "RELATED_ONLY",
    "band": "BACKGROUND"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q33",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R05",
      "relation": "REFUTES",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R31",
      "relation": "REFUTES",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R06",
      "relation": "RELATED_ONLY",
      "grade": 1,
      "relevanceBand": "BACKGROUND",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R32",
      "relation": "RELATED_ONLY",
      "grade": 1,
      "relevanceBand": "BACKGROUND",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "REFUTES",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q33",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q34

问题：Do I dislike repetitive work?

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R19",
    "content": "我不喜欢重复劳动。",
    "l0": "我不喜欢重复劳动。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "SUPPORTS",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q34",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R19",
      "relation": "SUPPORTS",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "SUPPORTS",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q34",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q35

问题：Did I live in Cedar Bay in 2020?

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R03",
    "content": "In 2020 I lived in Cedar Bay.",
    "l0": "In 2020 I lived in Cedar Bay.",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-05-01T00:00:00Z",
    "scope": "HISTORICAL",
    "grade": 3,
    "relation": "SUPPORTS",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R04",
    "content": "At the source date I live in Maple Port.",
    "l0": "At the source date I live in Maple Port.",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-05-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 1,
    "relation": "RELATED_ONLY",
    "band": "BACKGROUND"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q35",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R03",
      "relation": "SUPPORTS",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R04",
      "relation": "RELATED_ONLY",
      "grade": 1,
      "relevanceBand": "BACKGROUND",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "SUPPORTS",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q35",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q36

问题：Do I still live in Maple Port?

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R04",
    "content": "At the source date I live in Maple Port.",
    "l0": "At the source date I live in Maple Port.",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-05-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "RELATED_ONLY",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R03",
    "content": "In 2020 I lived in Cedar Bay.",
    "l0": "In 2020 I lived in Cedar Bay.",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-05-01T00:00:00Z",
    "scope": "HISTORICAL",
    "grade": 1,
    "relation": "RELATED_ONLY",
    "band": "BACKGROUND"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q36",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R04",
      "relation": "RELATED_ONLY",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R03",
      "relation": "RELATED_ONLY",
      "grade": 1,
      "relevanceBand": "BACKGROUND",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "INSUFFICIENT_EVIDENCE",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q36",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q37

问题：Have I decided to attend graduate school in North Shore?

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R23",
    "content": "如果拿到奖学金，我可能考虑去北岸市读研究生。",
    "l0": "如果拿到奖学金，我可能考虑去北岸市读研究生。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "RELATED_ONLY",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q37",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R23",
      "relation": "RELATED_ONLY",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "INSUFFICIENT_EVIDENCE",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q37",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q38

问题：Am I a proven patient person?

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R26",
    "content": "我觉得自己很耐心。",
    "l0": "我觉得自己很耐心。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "RELATED_ONLY",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q38",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R26",
      "relation": "RELATED_ONLY",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "INSUFFICIENT_EVIDENCE",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q38",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q39

问题：Do I consider myself patient?

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R26",
    "content": "我觉得自己很耐心。",
    "l0": "我觉得自己很耐心。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "SUPPORTS",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q39",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R26",
      "relation": "SUPPORTS",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "SUPPORTS",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q39",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q40

问题：Do I like gardening?

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R33",
    "content": "我喜欢园艺。",
    "l0": "我喜欢园艺。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "SUPPORTS",
    "band": "PRIMARY"
  },
  {
    "memoryId": "R34",
    "content": "我不喜欢园艺。",
    "l0": "我不喜欢园艺。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "SOURCE_TIME",
    "grade": 3,
    "relation": "REFUTES",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q40",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R33",
      "relation": "SUPPORTS",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    },
    {
      "memoryId": "R34",
      "relation": "REFUTES",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "INSUFFICIENT_EVIDENCE",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q40",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q43

问题：Have I completed a pottery course?

L1、L0、消息时间与来源性质：

```json
[
  {
    "memoryId": "R38",
    "content": "我已经完成陶艺课程。",
    "l0": "我已经完成陶艺课程。",
    "role": "user",
    "owner": "A",
    "privacy": "PRIVATE",
    "time": "2024-09-01T00:00:00Z",
    "scope": "HISTORICAL",
    "grade": 3,
    "relation": "SUPPORTS",
    "band": "PRIMARY"
  }
]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q43",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [
    {
      "memoryId": "R38",
      "relation": "SUPPORTS",
      "grade": 3,
      "relevanceBand": "PRIMARY",
      "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
    }
  ],
  "expectedAggregate": "SUPPORTS",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q43",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

## R2Q44

问题：Is there evidence that I own a telescope?

L1、L0、消息时间与来源性质：

```json
[]
```

现有参考（待独立确认）：
```json
{
  "queryId": "R2Q44",
  "propositionNote": "Test the affirmative/negative proposition as phrased; relation is scoped to evidence time unless current is explicit.",
  "relevant": [],
  "expectedAggregate": "INSUFFICIENT_EVIDENCE",
  "currentRecognition": "UNCONFIRMED",
  "previousQueryId": "R2Q44",
  "humanReviewStatus": "PENDING_INDEPENDENT_HUMAN_REVIEW"
}
```

理由与潜在争议：核对完整命题、条件/极性、消息时间、背景相关性及直接支持；未列参考可能不完备。

人工决定：UNREVIEWED；建议修订：____；理由：____；审核人：____

