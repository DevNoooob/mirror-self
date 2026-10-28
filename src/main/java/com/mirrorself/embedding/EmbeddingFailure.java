package com.mirrorself.embedding;

/** Fixed public codes. Never retain provider messages, response bodies or causes. */
public final class EmbeddingFailure extends RuntimeException {
    public enum Code { DISABLED, EGRESS_DENIED, UNAVAILABLE, DIMENSION_MISMATCH, INVALID_VECTOR, STALE_MEMORY }
    private final Code code;
    public EmbeddingFailure(Code code) { super(code.name()); this.code=code; }
    public Code code() { return code; }
}
