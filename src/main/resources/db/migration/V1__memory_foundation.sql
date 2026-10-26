-- Memory System V0.1 — single-owner deployment, designed for future tenant isolation.
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE source_record (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    source_system VARCHAR(40) NOT NULL CHECK (source_system IN ('CHATGPT','MANUAL')),
    source_locator TEXT NOT NULL,
    conversation_title TEXT,
    role VARCHAR(24) NOT NULL CHECK (role IN ('user','assistant','system','tool','unknown')),
    raw_message JSONB NOT NULL,
    text_content TEXT NOT NULL DEFAULT '',
    content_hash CHAR(64) NOT NULL,
    occurred_at TIMESTAMPTZ,
    imported_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    sensitivity VARCHAR(16) NOT NULL DEFAULT 'PRIVATE'
        CHECK (sensitivity IN ('PRIVATE','RESTRICTED')),
    UNIQUE(owner_id, source_system, source_locator, content_hash)
);
CREATE INDEX idx_source_owner_time ON source_record(owner_id, occurred_at DESC);
CREATE INDEX idx_source_text_trgm ON source_record USING gin (text_content gin_trgm_ops);

CREATE TABLE memory_item (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    memory_type VARCHAR(24) NOT NULL CHECK
        (memory_type IN ('FACT','EVENT','KNOWLEDGE','OPINION','GOAL','THOUGHT_PROCESS')),
    content TEXT NOT NULL CHECK (length(content) BETWEEN 1 AND 3000),
    assertion_origin VARCHAR(24) NOT NULL CHECK
        (assertion_origin IN ('USER_EXPLICIT','OBSERVED','MODEL_INFERRED')),
    status VARCHAR(24) NOT NULL CHECK
        (status IN ('CANDIDATE','PENDING_REVIEW','ACTIVE','REJECTED','SUPERSEDED','RETRACTED')),
    sensitivity VARCHAR(16) NOT NULL CHECK
        (sensitivity IN ('NORMAL','SENSITIVE','SECRET')),
    dedupe_key CHAR(64) NOT NULL,
    occurred_at TIMESTAMPTZ,
    valid_from TIMESTAMPTZ,
    valid_to TIMESTAMPTZ,
    superseded_by UUID REFERENCES memory_item(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_memory_valid_range CHECK (valid_to IS NULL OR valid_from IS NULL OR valid_to >= valid_from),
    UNIQUE(owner_id, dedupe_key)
);
CREATE INDEX idx_memory_owner_status ON memory_item(owner_id, status, occurred_at DESC);
CREATE INDEX idx_memory_text_trgm ON memory_item USING gin (content gin_trgm_ops);

CREATE TABLE memory_evidence (
    memory_id UUID NOT NULL REFERENCES memory_item(id) ON DELETE CASCADE,
    source_id UUID NOT NULL REFERENCES source_record(id) ON DELETE CASCADE,
    excerpt TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY(memory_id, source_id)
);
CREATE INDEX idx_memory_evidence_source ON memory_evidence(source_id);

CREATE TABLE memory_relation (
    owner_id UUID NOT NULL,
    from_memory_id UUID NOT NULL REFERENCES memory_item(id) ON DELETE CASCADE,
    to_memory_id UUID NOT NULL REFERENCES memory_item(id) ON DELETE CASCADE,
    relation_type VARCHAR(32) NOT NULL CHECK
        (relation_type IN ('RELATED_TO','SUPPORTS','CONTRADICTS','EVOLVED_FROM')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY(from_memory_id, to_memory_id, relation_type),
    CHECK(from_memory_id <> to_memory_id)
);

-- 1024 dimensions is an explicit V0.1 index contract, not a live embedding pipeline.
-- Model migrations require reindexing with a separate dimension/version table.
CREATE TABLE memory_embedding (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    source_id UUID REFERENCES source_record(id) ON DELETE CASCADE,
    memory_id UUID REFERENCES memory_item(id) ON DELETE CASCADE,
    model_id VARCHAR(100) NOT NULL,
    vector_value vector(1024) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_embedding_target CHECK
        ((source_id IS NOT NULL AND memory_id IS NULL)
         OR (source_id IS NULL AND memory_id IS NOT NULL))
);
CREATE UNIQUE INDEX idx_memory_embedding_unique_source
    ON memory_embedding(owner_id, source_id, model_id) WHERE source_id IS NOT NULL;
CREATE UNIQUE INDEX idx_memory_embedding_unique_memory
    ON memory_embedding(owner_id, memory_id, model_id) WHERE memory_id IS NOT NULL;
CREATE INDEX idx_memory_embedding_hnsw ON memory_embedding
    USING hnsw (vector_value vector_cosine_ops);

CREATE TABLE identity_claim (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    dimension VARCHAR(32) NOT NULL CHECK
        (dimension IN ('EXPERIENCE','KNOWLEDGE','COGNITION','PERSONALITY','VALUES',
                       'EMOTION','RELATIONSHIPS','MOTIVATION','SELF_PERCEPTION')),
    claim_key VARCHAR(120) NOT NULL,
    description TEXT NOT NULL,
    assertion_origin VARCHAR(24) NOT NULL CHECK
        (assertion_origin IN ('USER_EXPLICIT','OBSERVED','MODEL_INFERRED')),
    status VARCHAR(24) NOT NULL CHECK
        (status IN ('PENDING_REVIEW','ACTIVE','REJECTED','SUPERSEDED','RETRACTED')),
    sensitivity VARCHAR(16) NOT NULL CHECK
        (sensitivity IN ('NORMAL','SENSITIVE','SECRET')),
    valid_from TIMESTAMPTZ,
    valid_to TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_identity_owner_dim ON identity_claim(owner_id, dimension, status);
CREATE TABLE identity_evidence (
    identity_claim_id UUID NOT NULL REFERENCES identity_claim(id) ON DELETE CASCADE,
    memory_id UUID NOT NULL REFERENCES memory_item(id) ON DELETE CASCADE,
    PRIMARY KEY(identity_claim_id, memory_id)
);

-- Future sharing / delegation. V0.1 exposes NO endpoints granting third-party access.
CREATE TABLE share_profile (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    approved_description TEXT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT','APPROVED','REVOKED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE share_grant (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    profile_id UUID NOT NULL REFERENCES share_profile(id) ON DELETE CASCADE,
    principal_ref TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_share_grant_owner ON share_grant(owner_id, expires_at);

CREATE TABLE ingestion_job (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    source_system VARCHAR(40) NOT NULL,
    file_name TEXT NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('RUNNING','COMPLETE','FAILED')),
    source_count INTEGER NOT NULL DEFAULT 0,
    memory_count INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at TIMESTAMPTZ
);

CREATE TABLE audit_event (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    actor VARCHAR(24) NOT NULL DEFAULT 'OWNER',
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(60) NOT NULL,
    entity_id UUID NOT NULL,
    -- IDs only; do not store raw secrets/personal data in audit logs.
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_owner_time ON audit_event(owner_id, created_at DESC);
