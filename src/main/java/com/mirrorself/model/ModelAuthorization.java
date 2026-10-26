package com.mirrorself.model;

import com.mirrorself.common.Hashing;
import com.mirrorself.config.ExtractionProperties;
import com.mirrorself.config.MirrorProperties;
import com.mirrorself.ingestion.SourceRepository;
import com.mirrorself.ingestion.SourceRepository.SourceView;
import com.mirrorself.memory.SensitiveDetector;
import com.mirrorself.memory.Sensitivity;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModelAuthorization {
    private final JdbcTemplate jdbc;
    private final MirrorProperties mirror;
    private final ExtractionProperties properties;
    private final SourceRepository sources;
    public ModelAuthorization(JdbcTemplate jdbc, MirrorProperties mirror,
            ExtractionProperties properties, SourceRepository sources) {
        this.jdbc=jdbc; this.mirror=mirror; this.properties=properties; this.sources=sources;
    }
    private String providerKey() { return Hashing.sha256(properties.baseUrl() + "\n" + properties.model()); }
    public void check(UUID owner, UUID grant, List<SourceView> batch) {
        if (properties.mode() != ExtractionProperties.Mode.LLM || batch.isEmpty()) deny();
        if (properties.local()) return;
        if (!mirror.modelEgressEnabled() || grant == null) deny();
        for (var sent : batch) {
            SourceView original = sources.get(owner, sent.id());
            // Authorization covers the full source, even if only a slice is sent.
            Sensitivity sensitivity = SensitiveDetector.classify(original.text());
            List<Boolean> allowed = jdbc.query("""
                SELECT g.allow_sensitive FROM model_egress_grant g
                JOIN model_egress_grant_source gs ON gs.grant_id=g.id
                JOIN source_record s ON s.id=gs.source_id AND s.owner_id=g.owner_id
                WHERE g.id=? AND g.owner_id=? AND g.provider_key=? AND gs.source_id=?
                  AND g.revoked_at IS NULL AND g.expires_at>now() AND s.sensitivity='PRIVATE'
                """, (rs,row)->rs.getBoolean(1),grant,owner,providerKey(),sent.id());
            if (allowed.isEmpty() || sensitivity == Sensitivity.SECRET
                    || (sensitivity != Sensitivity.NORMAL && !allowed.getFirst())) deny();
        }
    }
    @Transactional
    public UUID grant(UUID owner, List<UUID> ids, Instant expiresAt, boolean allowSensitive) {
        if (!mirror.modelEgressEnabled() || properties.local() || properties.mode() != ExtractionProperties.Mode.LLM) deny();
        if (expiresAt == null || !expiresAt.isAfter(Instant.now()) || expiresAt.isAfter(Instant.now().plusSeconds(86400)))
            throw new IllegalArgumentException("grant expiry must be within 24 hours");
        if (ids == null || ids.isEmpty() || ids.size()>500) throw new IllegalArgumentException("provide 1-500 source IDs");
        for (UUID id : ids) sources.get(owner,id);
        UUID grant=UUID.randomUUID();
        jdbc.update("INSERT INTO model_egress_grant(id,owner_id,provider_key,allow_sensitive,expires_at) VALUES(?,?,?,?,?)",
                grant,owner,providerKey(),allowSensitive,OffsetDateTime.ofInstant(expiresAt,ZoneOffset.UTC));
        for (UUID id : new LinkedHashSet<>(ids)) jdbc.update(
                "INSERT INTO model_egress_grant_source(grant_id,source_id) VALUES(?,?)",grant,id);
        jdbc.update("INSERT INTO audit_event(id,owner_id,action,entity_type,entity_id) VALUES(?,?,'EGRESS_GRANTED','model_egress_grant',?)",
                UUID.randomUUID(),owner,grant);
        return grant;
    }
    @Transactional public void revoke(UUID owner, UUID grant) {
        if (jdbc.update("UPDATE model_egress_grant SET revoked_at=now() WHERE id=? AND owner_id=?",grant,owner)!=1)
            throw new IllegalArgumentException("grant not found");
        jdbc.update("INSERT INTO audit_event(id,owner_id,action,entity_type,entity_id) VALUES(?,?,'EGRESS_REVOKED','model_egress_grant',?)",
                UUID.randomUUID(),owner,grant);
    }
    private static void deny() { throw new ModelFailure(ModelFailure.Code.EGRESS_DENIED); }
}
