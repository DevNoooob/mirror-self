-- Additive V0.2 migration. V1 and all existing L0/L1 rows remain intact.
CREATE TABLE extraction_job (
    id UUID PRIMARY KEY, owner_id UUID NOT NULL,
    extractor_version VARCHAR(80) NOT NULL, model_id VARCHAR(100) NOT NULL,
    status VARCHAR(24) NOT NULL CHECK(status IN ('RUNNING','COMPLETE','PARTIAL','FAILED','BLOCKED')),
    source_count INTEGER NOT NULL, accepted_count INTEGER NOT NULL DEFAULT 0,
    rejected_count INTEGER NOT NULL DEFAULT 0, new_memory_count INTEGER NOT NULL DEFAULT 0,
    error_codes JSONB NOT NULL DEFAULT '[]'::jsonb,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(), finished_at TIMESTAMPTZ
);
CREATE INDEX idx_extraction_owner ON extraction_job(owner_id,started_at DESC);
CREATE TABLE extraction_job_source (
    job_id UUID NOT NULL REFERENCES extraction_job(id) ON DELETE CASCADE,
    source_id UUID NOT NULL REFERENCES source_record(id) ON DELETE CASCADE,
    PRIMARY KEY(job_id,source_id)
);
CREATE TABLE model_egress_grant (
    id UUID PRIMARY KEY, owner_id UUID NOT NULL, provider_key CHAR(64) NOT NULL,
    allow_sensitive BOOLEAN NOT NULL DEFAULT false,
    expires_at TIMESTAMPTZ NOT NULL, revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE model_egress_grant_source (
    grant_id UUID NOT NULL REFERENCES model_egress_grant(id) ON DELETE CASCADE,
    source_id UUID NOT NULL REFERENCES source_record(id) ON DELETE CASCADE,
    PRIMARY KEY(grant_id,source_id)
);
ALTER TABLE memory_item
    ADD COLUMN extraction_fingerprint CHAR(64),
    ADD COLUMN temporal_scope VARCHAR(24) NOT NULL DEFAULT 'SOURCE_TIME'
        CHECK(temporal_scope IN ('SOURCE_TIME','HISTORICAL')),
    ADD COLUMN extractor_version VARCHAR(80),
    ADD COLUMN model_id VARCHAR(100),
    ADD COLUMN extraction_job_id UUID REFERENCES extraction_job(id) ON DELETE SET NULL;
CREATE UNIQUE INDEX idx_memory_extraction_fingerprint
    ON memory_item(owner_id,extraction_fingerprint) WHERE extraction_fingerprint IS NOT NULL;
