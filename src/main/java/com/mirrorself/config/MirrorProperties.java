package com.mirrorself.config;

import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mirror")
public record MirrorProperties(UUID ownerId, String ownerToken, boolean modelEgressEnabled) {
    public MirrorProperties {
        if (ownerId == null) throw new IllegalArgumentException("mirror.owner-id is required");
        if (ownerToken == null || ownerToken.length() < 32
                || ownerToken.startsWith("REPLACE_")) {
            throw new IllegalArgumentException("Set MIRROR_OWNER_TOKEN (32+ unique characters) before startup");
        }
        if (modelEgressEnabled) {
            throw new IllegalArgumentException("V0.1 disables all model egress; mirror.model-egress-enabled must be false");
        }
    }
}
