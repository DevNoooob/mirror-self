-- Additive M2-B. No L0/L1 content, type, status or existing relation is rewritten.
CREATE UNIQUE INDEX idx_memory_owner_identity ON memory_item(owner_id,id);

CREATE FUNCTION consolidation_memory_visible(p_owner UUID,p_id UUID,p_revision BIGINT)
RETURNS BOOLEAN LANGUAGE sql STABLE AS $$
    SELECT EXISTS(SELECT 1 FROM memory_item m WHERE m.owner_id=p_owner AND m.id=p_id
        AND m.embedding_revision=p_revision AND m.status='ACTIVE' AND m.sensitivity='NORMAL'
        AND m.assertion_origin='USER_EXPLICIT'
        AND EXISTS(SELECT 1 FROM memory_evidence e WHERE e.memory_id=m.id)
        AND NOT EXISTS(SELECT 1 FROM memory_evidence e LEFT JOIN source_record s ON s.id=e.source_id
            WHERE e.memory_id=m.id AND (s.id IS NULL OR s.owner_id<>m.owner_id OR s.role<>'user'
                OR s.sensitivity<>'PRIVATE' OR length(e.excerpt)=0 OR position(e.excerpt in s.text_content)=0)))
$$;

CREATE TABLE consolidation_candidate (
    id UUID PRIMARY KEY, owner_id UUID NOT NULL,
    memory_a_id UUID NOT NULL, memory_b_id UUID NOT NULL,
    a_revision BIGINT NOT NULL CHECK(a_revision>=0), b_revision BIGINT NOT NULL CHECK(b_revision>=0),
    relation_type VARCHAR(24) NOT NULL CHECK(relation_type IN ('SAME_AS','SUPPORTS','CONTRADICTS','EVOLVED_FROM')),
    direction VARCHAR(16) NOT NULL CHECK(direction IN ('SYMMETRIC','A_TO_B','B_TO_A')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW' CHECK(status IN ('PENDING_REVIEW','APPROVED','REJECTED')),
    validity VARCHAR(16) NOT NULL DEFAULT 'VALID' CHECK(validity IN ('VALID','STALE')),
    invalidation_reason VARCHAR(80), review_version BIGINT NOT NULL DEFAULT 1,
    engine_version VARCHAR(80) NOT NULL, discovery_model_id VARCHAR(100) NOT NULL,
    discovery_model_version VARCHAR(80) NOT NULL,
    reason_codes JSONB NOT NULL, uncertainties JSONB NOT NULL, discovery JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), reviewed_at TIMESTAMPTZ,
    FOREIGN KEY(owner_id,memory_a_id) REFERENCES memory_item(owner_id,id) ON DELETE CASCADE,
    FOREIGN KEY(owner_id,memory_b_id) REFERENCES memory_item(owner_id,id) ON DELETE CASCADE,
    CHECK(memory_a_id<memory_b_id),
    CHECK((relation_type IN ('SAME_AS','CONTRADICTS'))=(direction='SYMMETRIC')),
    UNIQUE(owner_id,memory_a_id,memory_b_id,relation_type,a_revision,b_revision)
);
CREATE INDEX idx_consolidation_owner_status ON consolidation_candidate(owner_id,status,created_at DESC);
CREATE INDEX idx_consolidation_a ON consolidation_candidate(memory_a_id);
CREATE INDEX idx_consolidation_b ON consolidation_candidate(memory_b_id);

CREATE TABLE consolidation_cursor (
    owner_id UUID NOT NULL,memory_id UUID NOT NULL,scan_key CHAR(64) NOT NULL,
    memory_revision BIGINT NOT NULL,vector_epoch BIGINT NOT NULL DEFAULT 0,processed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY(owner_id,memory_id,scan_key),
    FOREIGN KEY(owner_id,memory_id) REFERENCES memory_item(owner_id,id) ON DELETE CASCADE
);
CREATE TABLE consolidation_job (
    id UUID PRIMARY KEY,owner_id UUID NOT NULL,status VARCHAR(16) NOT NULL CHECK(status IN ('RUNNING','COMPLETE','PARTIAL','FAILED')),
    scan_key CHAR(64) NOT NULL,vector_epoch BIGINT NOT NULL DEFAULT 0,engine_version VARCHAR(80) NOT NULL,mode VARCHAR(16) NOT NULL,
    model_id VARCHAR(100) NOT NULL,model_version VARCHAR(80) NOT NULL,
    selected_count INTEGER NOT NULL DEFAULT 0,processed_count INTEGER NOT NULL DEFAULT 0,
    compared_pairs INTEGER NOT NULL DEFAULT 0,created_count INTEGER NOT NULL DEFAULT 0,
    existing_count INTEGER NOT NULL DEFAULT 0,abstained_count INTEGER NOT NULL DEFAULT 0,stale_count INTEGER NOT NULL DEFAULT 0,
    error_codes JSONB NOT NULL DEFAULT '[]'::jsonb,started_at TIMESTAMPTZ NOT NULL DEFAULT now(),finished_at TIMESTAMPTZ
);
CREATE INDEX idx_consolidation_job_owner ON consolidation_job(owner_id,started_at DESC);

CREATE FUNCTION guard_consolidation_insert() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.status<>'PENDING_REVIEW' OR NEW.validity<>'VALID' OR NEW.review_version<>1
       OR NOT consolidation_memory_visible(NEW.owner_id,NEW.memory_a_id,NEW.a_revision)
       OR NOT consolidation_memory_visible(NEW.owner_id,NEW.memory_b_id,NEW.b_revision) THEN
        RAISE EXCEPTION 'invalid consolidation candidate state';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER consolidation_initial_guard BEFORE INSERT ON consolidation_candidate
    FOR EACH ROW EXECUTE FUNCTION guard_consolidation_insert();

-- Extend revision coverage through a NEW migration, preserving V3 bytes.
CREATE OR REPLACE FUNCTION invalidate_memory_embeddings() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF ROW(NEW.content,NEW.status,NEW.sensitivity,NEW.assertion_origin,NEW.memory_type,NEW.temporal_scope,
           NEW.embedding_revision,NEW.occurred_at,NEW.valid_from,NEW.valid_to,NEW.superseded_by,NEW.owner_id)
       IS DISTINCT FROM ROW(OLD.content,OLD.status,OLD.sensitivity,OLD.assertion_origin,OLD.memory_type,OLD.temporal_scope,
           OLD.embedding_revision,OLD.occurred_at,OLD.valid_from,OLD.valid_to,OLD.superseded_by,OLD.owner_id) THEN
        NEW.embedding_revision := OLD.embedding_revision+1;
        DELETE FROM memory_embedding WHERE memory_id=OLD.id;
    END IF;
    RETURN NEW;
END $$;
CREATE OR REPLACE FUNCTION invalidate_source_embeddings() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF ROW(NEW.text_content,NEW.role,NEW.sensitivity,NEW.owner_id,NEW.occurred_at,NEW.source_locator,NEW.source_system,NEW.raw_message)
       IS DISTINCT FROM ROW(OLD.text_content,OLD.role,OLD.sensitivity,OLD.owner_id,OLD.occurred_at,OLD.source_locator,OLD.source_system,OLD.raw_message) THEN
        UPDATE memory_item SET embedding_revision=embedding_revision+1
          WHERE id IN (SELECT memory_id FROM memory_evidence WHERE source_id=OLD.id);
    END IF;
    RETURN NEW;
END $$;
CREATE FUNCTION invalidate_consolidation_candidates() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.embedding_revision<>OLD.embedding_revision THEN
        UPDATE consolidation_candidate SET validity='STALE',invalidation_reason='MEMORY_OR_EVIDENCE_CHANGED',updated_at=now()
          WHERE (memory_a_id=OLD.id OR memory_b_id=OLD.id) AND validity='VALID';
    END IF;
    RETURN NULL;
END $$;
CREATE TRIGGER consolidation_invalidation AFTER UPDATE ON memory_item
    FOR EACH ROW EXECUTE FUNCTION invalidate_consolidation_candidates();
CREATE FUNCTION audit_consolidation_removal() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO audit_event(id,owner_id,actor,action,entity_type,entity_id)
      VALUES(gen_random_uuid(),OLD.owner_id,'SYSTEM','CONSOLIDATION_REMOVED','consolidation_candidate',OLD.id);
    RETURN NULL;
END $$;
CREATE TRIGGER consolidation_removal_audit AFTER DELETE ON consolidation_candidate
    FOR EACH ROW EXECUTE FUNCTION audit_consolidation_removal();

-- New vectors must revisit anchors that were previously scanned with keyword fallback.
CREATE TABLE consolidation_vector_epoch (
    owner_id UUID NOT NULL,model_id VARCHAR(100) NOT NULL,model_version VARCHAR(80) NOT NULL,
    epoch BIGINT NOT NULL DEFAULT 0,PRIMARY KEY(owner_id,model_id,model_version)
);
CREATE FUNCTION advance_consolidation_vector_epoch() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP<>'INSERT' AND OLD.memory_id IS NOT NULL THEN
        INSERT INTO consolidation_vector_epoch(owner_id,model_id,model_version,epoch)
          VALUES(OLD.owner_id,OLD.model_id,OLD.model_version,1)
          ON CONFLICT(owner_id,model_id,model_version) DO UPDATE SET epoch=consolidation_vector_epoch.epoch+1;
    END IF;
    IF TG_OP<>'DELETE' AND NEW.memory_id IS NOT NULL
       AND (TG_OP='INSERT' OR ROW(NEW.owner_id,NEW.model_id,NEW.model_version) IS DISTINCT FROM ROW(OLD.owner_id,OLD.model_id,OLD.model_version)) THEN
        INSERT INTO consolidation_vector_epoch(owner_id,model_id,model_version,epoch)
          VALUES(NEW.owner_id,NEW.model_id,NEW.model_version,1)
          ON CONFLICT(owner_id,model_id,model_version) DO UPDATE SET epoch=consolidation_vector_epoch.epoch+1;
    END IF;
    RETURN NULL;
END $$;
CREATE TRIGGER consolidation_vector_change AFTER INSERT OR UPDATE OR DELETE ON memory_embedding
    FOR EACH ROW EXECUTE FUNCTION advance_consolidation_vector_epoch();
