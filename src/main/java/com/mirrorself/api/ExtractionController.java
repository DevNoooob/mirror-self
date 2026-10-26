package com.mirrorself.api;

import com.mirrorself.config.MirrorProperties;
import com.mirrorself.memory.*;
import com.mirrorself.model.ModelAuthorization;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ExtractionController {
    public record Extract(@NotEmpty @Size(max=500) List<@NotNull UUID> sourceIds,UUID grantId) { }
    public record Grant(@NotEmpty @Size(max=500) List<@NotNull UUID> sourceIds,@NotNull Instant expiresAt,
            boolean allowSensitive) { }
    private final MirrorProperties properties;
    private final MemoryExtractionService extraction;
    private final ExtractionJobRepository jobs;
    private final ModelAuthorization authorization;
    public ExtractionController(MirrorProperties properties,MemoryExtractionService extraction,
            ExtractionJobRepository jobs,ModelAuthorization authorization) {
        this.properties=properties; this.extraction=extraction; this.jobs=jobs; this.authorization=authorization;
    }
    @PostMapping("/extractions")
    public ExtractionJobRepository.Job extract(@Valid @RequestBody Extract request) {
        return extraction.extract(properties.ownerId(),request.sourceIds(),request.grantId());
    }
    @GetMapping("/extractions/{id}")
    public ExtractionJobRepository.Job job(@PathVariable UUID id) { return jobs.get(properties.ownerId(),id); }
    @PostMapping("/model-egress-grants")
    public Map<String,Object> grant(@Valid @RequestBody Grant request) {
        UUID id=authorization.grant(properties.ownerId(),request.sourceIds(),request.expiresAt(),request.allowSensitive());
        return Map.of("id",id,"expiresAt",request.expiresAt(),"allowSensitive",request.allowSensitive());
    }
    @DeleteMapping("/model-egress-grants/{id}")
    public Map<String,Object> revoke(@PathVariable UUID id) {
        authorization.revoke(properties.ownerId(),id); return Map.of("id",id,"revoked",true);
    }
}
