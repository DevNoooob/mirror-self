package com.mirrorself.memory;

/** One versioned taxonomy shared by extraction and independent semantic validation. */
public final class MemoryTypeGuidance {
    public static final String VERSION="types1";
    private MemoryTypeGuidance() { }
    public static final String INSTRUCTIONS="""
        Memory type contract (types1): classify the focus of a supported user statement, not its grammar.
        FACT: personal circumstances, current role/residence, current tools or habitual usage; no skill inference.
        EVENT: a bounded occurrence, attendance or completed action with an episode as its focus.
        KNOWLEDGE: a topic/skill the user explicitly learned, studied or understands, or an explicit knowledge claim.
          Past study is KNOWLEDGE even though it is also a true biographical fact; it does not prove mastery.
          Using a tool now is FACT, not evidence of learning or proficiency. Attending a named workshop is EVENT
          when attendance is the focus; learning a subject there is KNOWLEDGE when learning is the focus.
        OPINION: an explicitly expressed preference, evaluation or belief about something.
        GOAL: a personal desired future outcome or intention, including conditional, low-commitment consideration.
          Preserve the entire condition and uncertainty. A purely hypothetical scenario, question or someone
          else's intention is not the user's GOAL. Never turn a possible intention into a decision or plan.
        THOUGHT_PROCESS: personality, values, self-concept or a pattern of reasoning/decision making.
          An explicit self-description remains USER_EXPLICIT; tentative inference is MODEL_INFERRED and
          permitted only here. Both are review candidates, never automatically confirmed personality.
        Priority for overlapping descriptions: cognitive/self-trait -> THOUGHT_PROCESS; future desire -> GOAL;
          preference/evaluation -> OPINION; explicit learning/knowledge -> KNOWLEDGE; bounded episode -> EVENT;
          otherwise supported personal circumstance/usage -> FACT. Split independent supported statements;
          do not duplicate one proposition under multiple types. No fallback FACT when evidence is insufficient.
        Type, origin, sensitivity and time are independent axes. HISTORICAL for explicit prior learning,
          events or former views; SOURCE_TIME for assertions as of the message, not verified current today.
        Fictional examples (guidance only, never extract these examples as user memories):
          'I studied fictional orbital botany last winter.' -> KNOWLEDGE / USER_EXPLICIT / HISTORICAL.
          'I currently use a fictional PinePad notebook.' -> FACT / USER_EXPLICIT / SOURCE_TIME.
          'Last week I attended the fictional Lantern fair.' -> EVENT / USER_EXPLICIT / HISTORICAL.
          'I prefer quiet workspaces.' -> OPINION / USER_EXPLICIT / SOURCE_TIME.
          'If I find time, I may consider building a small kite.' -> GOAL / USER_EXPLICIT / SOURCE_TIME.
          'I describe myself as cautious when deciding.' -> THOUGHT_PROCESS / USER_EXPLICIT / SOURCE_TIME.
        """;
}
