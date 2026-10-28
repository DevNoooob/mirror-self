package com.mirrorself.api;

import com.mirrorself.config.MirrorProperties;
import com.mirrorself.memory.MemoryRepository;
import com.mirrorself.retrieval.RetrievalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RetrievalController {
    public record AskRequest(@NotBlank @Size(max=1000) String question) { }

    private final MirrorProperties props;
    private final RetrievalService service;
    public RetrievalController(MirrorProperties props, RetrievalService service) {
        this.props=props;
        this.service=service;
    }

    @GetMapping("/retrieval")
    public List<MemoryRepository.MemoryView> retrieve(@RequestParam String query,
                                                       @RequestParam(defaultValue="10") int limit) {
        return service.retrieve(props.ownerId(), query, limit);
    }

    @GetMapping("/retrieval/explain")
    public RetrievalService.Result explain(@RequestParam String query,
            @RequestParam(defaultValue="10") int limit,
            @RequestParam(defaultValue="HYBRID") RetrievalService.Mode mode) {
        return service.explain(props.ownerId(),query,limit,mode);
    }

    @PostMapping("/ask")
    public RetrievalService.ExtractiveAnswer ask(@Valid @RequestBody AskRequest request) {
        return service.answer(props.ownerId(), request.question());
    }
}
