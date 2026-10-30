package com.mirrorself.api;

import com.mirrorself.config.MirrorProperties;
import com.mirrorself.consolidation.*;
import com.mirrorself.memory.ReviewDecision;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/consolidation")
public class ConsolidationController {
    public record Review(@NotNull ReviewDecision decision,@NotNull @Min(1) Long expectedVersion) { }
    private final MirrorProperties properties;
    private final ConsolidationScanService scans;
    private final ConsolidationService candidates;
    public ConsolidationController(MirrorProperties properties,ConsolidationScanService scans,ConsolidationService candidates) {
        this.properties=properties;this.scans=scans;this.candidates=candidates;
    }
    @PostMapping("/jobs") public ConsolidationScanService.Job scan(@RequestParam(defaultValue="10") int limit,
            @RequestParam(defaultValue="10") int neighbors,@RequestParam(defaultValue="HYBRID") ConsolidationRecall.Mode mode) {
        return scans.run(properties.ownerId(),limit,neighbors,mode);
    }
    @GetMapping("/jobs/{id}") public ConsolidationScanService.Job job(@PathVariable UUID id) {return scans.get(properties.ownerId(),id);}
    @GetMapping("/candidates") public List<ConsolidationService.View> list(@RequestParam(required=false) String status,
            @RequestParam(defaultValue="20") int limit,@RequestParam(defaultValue="0") int offset) {
        return candidates.list(properties.ownerId(),status,limit,offset);
    }
    @GetMapping("/candidates/{id}") public ConsolidationService.View candidate(@PathVariable UUID id) {return candidates.get(properties.ownerId(),id);}
    @PostMapping("/candidates/{id}/review") public ConsolidationService.View review(@PathVariable UUID id,@Valid @RequestBody Review body) {
        return candidates.review(properties.ownerId(),id,body.decision(),body.expectedVersion());
    }
}
