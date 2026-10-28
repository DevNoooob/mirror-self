package com.mirrorself.model;

/** Only fixed, non-sensitive codes cross the model boundary. Never chain provider exceptions. */
public final class ModelFailure extends RuntimeException {
    public enum Code { EGRESS_DENIED, MODEL_UNAVAILABLE, INVALID_MODEL_OUTPUT, INVALID_EVIDENCE, STORAGE_FAILED, CLASSIFICATION_CONFLICT }
    private final Code code;
    public ModelFailure(Code code) { super(code.name()); this.code = code; }
    public Code code() { return code; }
}
