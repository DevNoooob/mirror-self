-- Additive: preserve all L0/L1 content and V1/V2 migration history.
ALTER TABLE memory_item ADD COLUMN embedding_revision BIGINT NOT NULL DEFAULT 0;
ALTER TABLE memory_embedding ADD COLUMN model_version VARCHAR(80) NOT NULL DEFAULT 'legacy',
    ADD COLUMN memory_revision BIGINT NOT NULL DEFAULT 0;
DROP INDEX idx_memory_embedding_unique_memory;
DROP INDEX idx_memory_embedding_unique_source;
CREATE UNIQUE INDEX idx_memory_embedding_unique_memory_version
    ON memory_embedding(owner_id,memory_id,model_id,model_version) WHERE memory_id IS NOT NULL;
CREATE UNIQUE INDEX idx_memory_embedding_unique_source_version
    ON memory_embedding(owner_id,source_id,model_id,model_version) WHERE source_id IS NOT NULL;

CREATE TABLE embedding_job (
    id UUID PRIMARY KEY, owner_id UUID NOT NULL,
    model_id VARCHAR(100) NOT NULL, model_version VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK(status IN ('RUNNING','COMPLETE','PARTIAL','FAILED','BLOCKED')),
    selected_count INTEGER NOT NULL DEFAULT 0, inserted_count INTEGER NOT NULL DEFAULT 0,
    skipped_count INTEGER NOT NULL DEFAULT 0, failed_count INTEGER NOT NULL DEFAULT 0,
    error_codes JSONB NOT NULL DEFAULT '[]'::jsonb,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(), finished_at TIMESTAMPTZ
);
CREATE INDEX idx_embedding_job_owner ON embedding_job(owner_id,started_at DESC);

-- Invalidate all versions atomically; revision CAS also rejects in-flight stale writes.
CREATE FUNCTION invalidate_memory_embeddings() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF ROW(NEW.content,NEW.status,NEW.sensitivity,NEW.assertion_origin,NEW.memory_type,
           NEW.temporal_scope,NEW.embedding_revision)
       IS DISTINCT FROM
       ROW(OLD.content,OLD.status,OLD.sensitivity,OLD.assertion_origin,OLD.memory_type,
           OLD.temporal_scope,OLD.embedding_revision) THEN
        NEW.embedding_revision := OLD.embedding_revision+1;
        DELETE FROM memory_embedding WHERE memory_id=OLD.id;
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER memory_embedding_invalidation BEFORE UPDATE ON memory_item
    FOR EACH ROW EXECUTE FUNCTION invalidate_memory_embeddings();

CREATE FUNCTION invalidate_evidence_embeddings() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP<>'INSERT' THEN
        UPDATE memory_item SET embedding_revision=embedding_revision+1 WHERE id=OLD.memory_id;
    END IF;
    IF TG_OP<>'DELETE' AND (TG_OP='INSERT' OR NEW.memory_id IS DISTINCT FROM OLD.memory_id) THEN
        UPDATE memory_item SET embedding_revision=embedding_revision+1 WHERE id=NEW.memory_id;
    END IF;
    RETURN NULL;
END $$;
CREATE TRIGGER evidence_embedding_invalidation AFTER INSERT OR UPDATE OR DELETE ON memory_evidence
    FOR EACH ROW EXECUTE FUNCTION invalidate_evidence_embeddings();

CREATE FUNCTION invalidate_source_embeddings() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF ROW(NEW.text_content,NEW.role,NEW.sensitivity,NEW.owner_id)
       IS DISTINCT FROM ROW(OLD.text_content,OLD.role,OLD.sensitivity,OLD.owner_id) THEN
        UPDATE memory_item SET embedding_revision=embedding_revision+1
          WHERE id IN (SELECT memory_id FROM memory_evidence WHERE source_id=OLD.id);
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER source_embedding_invalidation BEFORE UPDATE ON source_record
    FOR EACH ROW EXECUTE FUNCTION invalidate_source_embeddings();
