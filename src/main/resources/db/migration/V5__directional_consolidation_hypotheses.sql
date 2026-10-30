-- Preserve every existing hypothesis and review. Only expand idempotency to include direction.
DO $$
DECLARE prior_unique TEXT;
BEGIN
    SELECT conname INTO STRICT prior_unique FROM pg_constraint
    WHERE conrelid='consolidation_candidate'::regclass AND contype='u'
      AND pg_get_constraintdef(oid)='UNIQUE (owner_id, memory_a_id, memory_b_id, relation_type, a_revision, b_revision)';
    EXECUTE format('ALTER TABLE consolidation_candidate DROP CONSTRAINT %I',prior_unique);
END $$;
ALTER TABLE consolidation_candidate ADD CONSTRAINT consolidation_hypothesis_identity
    UNIQUE(owner_id,memory_a_id,memory_b_id,relation_type,direction,a_revision,b_revision);
