package com.mirrorself.api;

import com.mirrorself.config.MirrorProperties;
import com.mirrorself.identity.IdentityClaimRepository;
import com.mirrorself.memory.MemoryStatus;
import com.mirrorself.memory.ReviewDecision;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/identity/claims")
public class IdentityController {
    public record Review(@NotNull ReviewDecision decision) { }
    private final MirrorProperties props;
    private final IdentityClaimRepository claims;
    public IdentityController(MirrorProperties props, IdentityClaimRepository claims) {
        this.props = props;
        this.claims = claims;
    }

    @GetMapping
    public List<IdentityClaimRepository.Claim> list(@RequestParam(required=false) MemoryStatus status) {
        return claims.list(props.ownerId(), status);
    }

    @PostMapping("/{id}/review")
    public Map<String,Object> review(@PathVariable UUID id, @Valid @RequestBody Review request) {
        return Map.of("id", id, "status", claims.review(props.ownerId(), id, request.decision()));
    }
}
