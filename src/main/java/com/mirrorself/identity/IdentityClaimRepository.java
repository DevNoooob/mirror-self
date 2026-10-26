package com.mirrorself.identity;

import com.mirrorself.memory.AssertionOrigin;
import com.mirrorself.memory.MemoryStatus;
import com.mirrorself.memory.ReviewDecision;
import com.mirrorself.memory.Sensitivity;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class IdentityClaimRepository {
    public record Claim(UUID id, IdentityDimension dimension, String claimKey, String description,
                        AssertionOrigin origin, MemoryStatus status, Sensitivity sensitivity) { }
    private final JdbcTemplate jdbc;
    public IdentityClaimRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public Claim propose(UUID owner, IdentityDimension dimension, String claimKey,
                         String description, AssertionOrigin origin,
                         Sensitivity sensitivity, List<UUID> evidenceIds) {
        if (claimKey == null || claimKey.isBlank() || claimKey.length() > 120
                || description == null || description.isBlank() || description.length() > 3000) {
            throw new IllegalArgumentException("invalid identity claim");
        }
        if (origin != AssertionOrigin.USER_EXPLICIT && (evidenceIds == null || evidenceIds.isEmpty())) {
            throw new IllegalArgumentException("derived identity claims require evidence");
        }
        MemoryStatus status = origin == AssertionOrigin.USER_EXPLICIT && sensitivity == Sensitivity.NORMAL
                ? MemoryStatus.ACTIVE : MemoryStatus.PENDING_REVIEW;
        UUID id = UUID.randomUUID();
        // Never accept fabricated or cross-owner evidence identifiers.
        if (evidenceIds != null) {
            for (UUID memoryId : evidenceIds) {
                Integer count = jdbc.queryForObject("""
                    SELECT count(*) FROM memory_item WHERE id=? AND owner_id=? AND status='ACTIVE'
                    """, Integer.class, memoryId, owner);
                if (count == null || count != 1) throw new IllegalArgumentException("unavailable identity evidence");
            }
        }
        jdbc.update("""
                INSERT INTO identity_claim (id,owner_id,dimension,claim_key,description,
                   assertion_origin,status,sensitivity) VALUES (?,?,?,?,?,?,?,?)
                """, id, owner, dimension.name(), claimKey, description,
                origin.name(), status.name(), sensitivity.name());
        if (evidenceIds != null) {
            for (UUID memoryId : evidenceIds) {
                jdbc.update("""
                    INSERT INTO identity_evidence(identity_claim_id,memory_id) VALUES (?,?)
                    ON CONFLICT DO NOTHING
                    """, id, memoryId);
            }
        }
        jdbc.update("""
                INSERT INTO audit_event(id,owner_id,action,entity_type,entity_id)
                VALUES (?,?,?,?,?)
                """, UUID.randomUUID(), owner, "IDENTITY_PROPOSED", "identity_claim", id);
        return new Claim(id, dimension, claimKey, description, origin, status, sensitivity);
    }

    public List<Claim> list(UUID owner, MemoryStatus filter) {
        String where = filter == null ? "" : " AND status=?";
        Object[] args = filter == null ? new Object[]{owner} : new Object[]{owner, filter.name()};
        return jdbc.query("""
                SELECT id,dimension,claim_key,description,assertion_origin,status,sensitivity
                FROM identity_claim WHERE owner_id=?
                """ + where + " ORDER BY created_at DESC LIMIT 100", (rs, row) -> new Claim(
                rs.getObject("id", UUID.class),
                IdentityDimension.valueOf(rs.getString("dimension")),
                rs.getString("claim_key"), rs.getString("description"),
                AssertionOrigin.valueOf(rs.getString("assertion_origin")),
                MemoryStatus.valueOf(rs.getString("status")),
                Sensitivity.valueOf(rs.getString("sensitivity"))), args);
    }

    @Transactional
    public MemoryStatus review(UUID owner, UUID claimId, ReviewDecision decision) {
        MemoryStatus target = decision == ReviewDecision.APPROVE
                ? MemoryStatus.ACTIVE : MemoryStatus.REJECTED;
        int count = jdbc.update("""
                UPDATE identity_claim SET status=?,updated_at=now()
                WHERE id=? AND owner_id=? AND status='PENDING_REVIEW'
                """, target.name(), claimId, owner);
        if (count != 1) throw new IllegalStateException("identity claim missing or not pending");
        jdbc.update("""
                INSERT INTO audit_event(id,owner_id,action,entity_type,entity_id)
                VALUES (?,?,?,?,?)
                """, UUID.randomUUID(), owner, "IDENTITY_" + decision.name(), "identity_claim", claimId);
        return target;
    }
}
