package com.mirrorself.consolidation;

import com.google.gson.JsonObject;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsolidationWriter {
    public static final String ENGINE_VERSION="consolidation-engine-3";
    public record Outcome(String kind,UUID candidateId,String reason) { }
    private final ConsolidationRepository repository;
    private final ConsolidationClassifier classifier;
    public ConsolidationWriter(ConsolidationRepository repository,ConsolidationClassifier classifier) {this.repository=repository;this.classifier=classifier;}
    @Transactional
    public Outcome propose(UUID owner,ConsolidationRecall.Batch batch,ConsolidationRecall.Neighbor neighbor) {
        var left=batch.anchor();var right=neighbor.target();
        var a=left.memory().id().toString().compareTo(right.memory().id().toString())<0?left:right;var b=a==left?right:left;
        repository.lockMemories(owner,a.memory().id(),b.memory().id());
        var fresh=repository.targets(owner,List.of(a.memory().id(),b.memory().id()));
        if(fresh.size()!=2) return new Outcome("STALE",null,"TARGET_NO_LONGER_ELIGIBLE");
        var byId=fresh.stream().collect(java.util.stream.Collectors.toMap(t->t.memory().id(),t->t));
        if(byId.get(a.memory().id()).revision()!=a.revision()||byId.get(b.memory().id()).revision()!=b.revision())
            return new Outcome("STALE",null,"TARGET_REVISION_CHANGED");
        if(complexValidity(byId.get(a.memory().id()))||complexValidity(byId.get(b.memory().id())))
            return new Outcome("ABSTAINED",null,"EXPLICIT_VALIDITY_RANGE_REQUIRES_SEPARATE_REVIEW");
        var result=classifier.classify(byId.get(a.memory().id()).memory(),byId.get(b.memory().id()).memory());
        if(result.proposal()==null) return new Outcome("ABSTAINED",null,result.abstention());
        var score=neighbor.score();var discovery=new JsonObject();
        discovery.addProperty("anchorId",batch.anchor().memory().id().toString());discovery.addProperty("rrfScore",score.score());
        if(score.keywordRank()!=null) discovery.addProperty("keywordRank",score.keywordRank());
        if(score.vectorRank()!=null) {discovery.addProperty("vectorRank",score.vectorRank());discovery.addProperty("cosineSimilarity",score.cosineSimilarity());}
        discovery.addProperty("modelRequests",0);discovery.addProperty("truthSource","VERIFIED_L0_PROPOSITIONS_NOT_VECTOR_SCORE");
        var stored=repository.save(owner,a,b,result.proposal(),batch.modelId(),batch.modelVersion(),discovery);
        return new Outcome(stored.inserted()?"CREATED":"EXISTING",stored.id(),null);
    }
    private boolean complexValidity(ConsolidationRepository.Target target) {
        return target.validTo()!=null||target.supersededBy()!=null
            || target.validFrom()!=null&&!target.validFrom().equals(target.memory().occurredAt());
    }
}
