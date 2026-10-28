package com.mirrorself.consolidation;

import com.google.gson.Gson;
import com.mirrorself.memory.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class ConsolidationService {
    public record Side(UUID id,long revision,MemoryType type,String content,AssertionOrigin origin,MemoryStatus status,
        String temporalScope,Instant occurredAt,Instant validFrom,Instant validTo,UUID supersededBy,List<MemoryRepository.Evidence> evidence,String extractorVersion,String modelId) { }
    public record Discovery(UUID anchorId,Double rrfScore,Integer keywordRank,Integer vectorRank,Double cosineSimilarity,int modelRequests,String truthSource) { }
    public record View(UUID id,ConsolidationRelation relation,String direction,String status,String validity,long reviewVersion,
        Side memoryA,Side memoryB,String engineVersion,String discoveryModelId,String discoveryModelVersion,
        List<String> reasonCodes,List<String> uncertainties,Discovery discovery,String approvedEffect) { }
    private final ConsolidationRepository repository;
    public ConsolidationService(ConsolidationRepository repository) {this.repository=repository;}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public List<View> list(UUID owner,String status,int limit,int offset) {
        return repository.list(owner,status,limit,offset).stream().map(c->view(owner,c)).toList();
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public View get(UUID owner,UUID id) {return view(owner,repository.get(owner,id));}
    @Transactional
    public View review(UUID owner,UUID id,ReviewDecision decision,long version) {
        if(decision==null||version<1) throw new IllegalArgumentException("decision and positive expectedVersion are required");
        var before=repository.get(owner,id);
        // Lock order matches generation/invalidation: memories in DB UUID order, then candidate.
        repository.lockMemories(owner,before.a(),before.b());before=repository.get(owner,id);
        repository.review(owner,before,decision,version);
        return view(owner,repository.get(owner,id));
    }
    private View view(UUID owner,ConsolidationRepository.Candidate c) {
        var targets=repository.targets(owner,List.of(c.a(),c.b()));
        if(targets.size()!=2) throw new IllegalArgumentException("candidate not found or no longer eligible");
        var byId=targets.stream().collect(java.util.stream.Collectors.toMap(t->t.memory().id(),t->t));
        if(byId.get(c.a()).revision()!=c.aRevision()||byId.get(c.b()).revision()!=c.bRevision())
            throw new IllegalArgumentException("candidate not found or no longer eligible");
        return new View(c.id(),c.relation(),c.direction(),c.status(),c.validity(),c.reviewVersion(),side(byId.get(c.a())),side(byId.get(c.b())),
            c.engineVersion(),c.modelId(),c.modelVersion(),c.reasons(),c.uncertainties(),new Gson().fromJson(c.discovery(),Discovery.class),"RELATION_REVIEW_ONLY");
    }
    private Side side(ConsolidationRepository.Target target) {
        var m=target.memory();return new Side(m.id(),target.revision(),m.type(),m.content(),m.origin(),m.status(),m.temporalScope(),m.occurredAt(),target.validFrom(),target.validTo(),target.supersededBy(),m.evidence(),m.extractorVersion(),m.modelId());
    }
}
