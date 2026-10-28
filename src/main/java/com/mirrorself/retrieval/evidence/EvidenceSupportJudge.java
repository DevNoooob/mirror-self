package com.mirrorself.retrieval.evidence;

import com.mirrorself.memory.MemoryRepository.MemoryView;
import java.util.List;
import java.util.UUID;

/** Evidence entailment is separate from relevance/ranking. No database/model writes. */
public interface EvidenceSupportJudge {
    enum Relation { SUPPORTS, REFUTES, RELATED_ONLY, INSUFFICIENT_EVIDENCE }
    enum RequestedTime { EVIDENCE_TIME, HISTORICAL, CURRENT }
    record Proposition(String subject,String predicate,String object,boolean negative,
                       RequestedTime requestedTime,Integer explicitYear,boolean resolved) { }
    record Decision(UUID memoryId,Proposition proposition,Relation relation,Relation sourceTimeRelation,
                    String evidenceTimeScope,String currentRecognition,List<UUID> sourceIds,List<String> reasons) { }
    record Conclusion(Relation relation,String currentRecognition,List<String> reasons) { }
    Proposition proposition(String question);
    Decision assess(String question,MemoryView memory);
    default Conclusion conclude(List<Decision> evidence) {
        boolean supports=evidence.stream().anyMatch(d->d.relation()==Relation.SUPPORTS);
        boolean refutes=evidence.stream().anyMatch(d->d.relation()==Relation.REFUTES);
        if(supports && refutes) return new Conclusion(Relation.INSUFFICIENT_EVIDENCE,"UNCONFIRMED",List.of("CONFLICTING_DIRECT_EVIDENCE"));
        if((supports || refutes) && evidence.stream().anyMatch(d->d.proposition().requestedTime()!=RequestedTime.HISTORICAL
                && d.reasons().contains("CONTRADICTION_RECORDED_NO_AUTOMATIC_RESOLUTION")))
            return new Conclusion(Relation.INSUFFICIENT_EVIDENCE,"UNCONFIRMED",List.of("RECORDED_CONFLICT_REQUIRES_REVIEW"));
        if(supports || refutes) return new Conclusion(supports?Relation.SUPPORTS:Relation.REFUTES,"UNCONFIRMED",List.of("AT_EVIDENCE_TIME_ONLY"));
        return new Conclusion(Relation.INSUFFICIENT_EVIDENCE,"UNCONFIRMED",List.of("NO_DIRECT_EVIDENCE"));
    }
}
