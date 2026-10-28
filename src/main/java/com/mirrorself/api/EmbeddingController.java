package com.mirrorself.api;

import com.mirrorself.config.MirrorProperties;
import com.mirrorself.embedding.EmbeddingService;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/embeddings/jobs")
public class EmbeddingController {
    private final MirrorProperties properties;
    private final EmbeddingService service;
    public EmbeddingController(MirrorProperties properties,EmbeddingService service) { this.properties=properties;this.service=service; }
    @PostMapping public EmbeddingService.Job create(@RequestParam(defaultValue="20") int limit) {
        return service.run(properties.ownerId(),limit);
    }
    @GetMapping("/{id}") public EmbeddingService.Job get(@PathVariable UUID id) {
        return service.get(properties.ownerId(),id);
    }
}
