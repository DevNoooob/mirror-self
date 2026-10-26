package com.mirrorself.api;

import com.mirrorself.config.MirrorProperties;
import com.mirrorself.memory.MemoryRepository;
import com.mirrorself.memory.MemoryService;
import com.mirrorself.memory.MemoryStatus;
import com.mirrorself.memory.MemoryType;
import com.mirrorself.memory.ReviewDecision;
import com.mirrorself.memory.Sensitivity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/memories")
public class MemoryController {
    public record CreateMemory(@NotNull MemoryType type,
                               @NotBlank @Size(max=3000) String text,
                               @NotNull Sensitivity sensitivity,
                               Instant occurredAt) { }
    public record Review(@NotNull ReviewDecision decision) { }

    private final MirrorProperties props;
    private final MemoryService service;
    private final MemoryRepository repository;
    public MemoryController(MirrorProperties props, MemoryService service, MemoryRepository repository) {
        this.props = props;
        this.service = service;
        this.repository = repository;
    }

    @PostMapping
    public MemoryRepository.Stored create(@Valid @RequestBody CreateMemory request) {
        return service.addManual(props.ownerId(), request.type(), request.text(),
                request.sensitivity(), request.occurredAt());
    }

    @GetMapping
    public List<MemoryRepository.MemoryView> list(
            @RequestParam(required=false) MemoryStatus status,
            @RequestParam(defaultValue="50") int limit) {
        return repository.list(props.ownerId(), status, limit);
    }

    @PostMapping("/{id}/review")
    public Map<String, Object> review(@PathVariable UUID id, @Valid @RequestBody Review request) {
        MemoryStatus status = service.review(props.ownerId(), id, request.decision());
        return Map.of("id", id, "status", status);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable UUID id) {
        service.deleteMemory(props.ownerId(), id);
        return Map.of("deleted", true, "id", id);
    }
}
