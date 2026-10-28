package com.mirrorself.consolidation;

import com.mirrorself.memory.*;
import com.mirrorself.memory.MemoryRepository.MemoryView;
import com.mirrorself.retrieval.evidence.ConservativeEvidenceJudge;
import com.mirrorself.retrieval.evidence.EvidenceSupportJudge.RequestedTime;
import java.util.*;
import org.springframework.stereotype.Component;

/** High precision, bounded proposition rules. Retrieval scores are never proof. */
@Component
public class ConsolidationClassifier {
    public static final String VERSION="consolidation-rules-1";
    public record Proposal(ConsolidationRelation relation,String direction,List<String> reasons,List<String> uncertainties) { }
    public record Result(Proposal proposal,String abstention) { }
    private record Claim(String predicate,String object,boolean negative,String mode,String condition,
                         RequestedTime lexicalTime,Integer year) {
        boolean sameCore(Claim other) {return predicate.equals(other.predicate)&&object.equals(other.object);}
        boolean sameQualification(Claim other) {return mode.equals(other.mode)&&condition.equals(other.condition);}
    }
    private final ConservativeEvidenceJudge parser;
    public ConsolidationClassifier(ConservativeEvidenceJudge parser) {this.parser=parser;}
    public Result classify(MemoryView a,MemoryView b) {
        if(a.id().equals(b.id())) return skip("IDENTICAL_MEMORY_ID");
        if(a.status()!=MemoryStatus.ACTIVE || b.status()!=MemoryStatus.ACTIVE || a.sensitivity()!=Sensitivity.NORMAL || b.sensitivity()!=Sensitivity.NORMAL
                || a.origin()!=AssertionOrigin.USER_EXPLICIT || b.origin()!=AssertionOrigin.USER_EXPLICIT) return skip("INELIGIBLE_STATUS_OR_ORIGIN");
        if(a.type()!=b.type()) return skip("TYPE_CORRECTION_REQUIRES_SEPARATE_REVIEW");
        Claim x=parse(a.content()),y=parse(b.content());
        if(x==null||y==null) return skip("UNRESOLVED_COMPLETE_PROPOSITION");
        if(!supported(a,x)||!supported(b,y)) return skip("INCOMPLETE_OR_CONFLICTING_L0_SUPPORT");
        if(!x.sameCore(y)) return skip("DIFFERENT_PROPOSITION_OR_CONTEXT");
        if(!x.sameQualification(y)) return skip("MODALITY_OR_CONDITION_DIFFERS");
        var reasons=new ArrayList<String>();var uncertainty=new ArrayList<String>();
        uncertainty.add("CANDIDATE_ONLY_NO_MEMORY_CHANGE");uncertainty.add("CURRENT_ENDORSEMENT_UNCONFIRMED");
        if(a.type()==MemoryType.THOUGHT_PROCESS) {
            if(!"SELF_REPORT".equals(x.predicate)) return skip("OBJECTIVE_PERSONALITY_NOT_ESTABLISHED");
            uncertainty.add("SELF_REPORT_ONLY_NOT_OBJECTIVE_PERSONALITY");
        }
        if(!"ACTUAL".equals(x.mode)) uncertainty.add("INTENTION_OR_CONDITION_NOT_COMPLETION");
        if(a.conflicts().contains(b.id())||b.conflicts().contains(a.id())) uncertainty.add("EXISTING_CONFLICT_MARKER_REQUIRES_REVIEW");
        boolean historicalA="HISTORICAL".equals(a.temporalScope()),historicalB="HISTORICAL".equals(b.temporalScope());
        boolean samePoint=a.occurredAt()!=null && a.occurredAt().equals(b.occurredAt());
        boolean sharedSource=a.evidence().stream().anyMatch(e->b.evidence().stream().anyMatch(f->e.sourceId().equals(f.sourceId())));
        boolean dated=a.occurredAt()!=null && b.occurredAt()!=null;
        if(x.negative!=y.negative) {
            if(!"ACTUAL".equals(x.mode)) return skip("OPPOSITE_INTENT_SCOPE_UNRESOLVED");
            if(historicalA!=historicalB && explicitHistorical(a,b)) {
                reasons.add("EXPLICIT_HISTORICAL_AND_SOURCE_TIME_CHANGE");uncertainty.add("POSSIBLE_EVOLUTION_NOT_CONFIRMED");
                return proposal(ConsolidationRelation.EVOLVED_FROM,historicalA?"B_TO_A":"A_TO_B",reasons,uncertainty);
            }
            if(historicalA!=historicalB) return skip("TIME_SCOPE_NOT_ALIGNED");
            if(!Objects.equals(x.year,y.year)) return skip("EVENT_YEAR_DIFFERS");
            if(samePoint || sharedSource) {
                reasons.add("SAME_POINT_OPPOSITE_EXPLICIT_POLARITY");
                return proposal(ConsolidationRelation.CONTRADICTS,"SYMMETRIC",reasons,uncertainty);
            }
            if(dated && !historicalA) {
                reasons.add("DATED_OPPOSITE_EXPLICIT_ASSERTIONS");uncertainty.add("POSSIBLE_EVOLUTION_OR_MISSING_CONTEXT");
                return proposal(ConsolidationRelation.EVOLVED_FROM,a.occurredAt().isAfter(b.occurredAt())?"A_TO_B":"B_TO_A",reasons,uncertainty);
            }
            return skip("TIME_ANCHOR_MISSING_OR_HISTORICAL_PERIOD_UNKNOWN");
        }
        if(historicalA!=historicalB || !Objects.equals(x.year,y.year) || x.lexicalTime!=y.lexicalTime)
            return skip("TIME_SCOPE_NOT_EQUIVALENT");
        Set<UUID> sa=sourceIds(a),sb=sourceIds(b);
        if(sa.size()!=sb.size() && (sa.containsAll(sb)||sb.containsAll(sa))) {
            reasons.add("ADDITIONAL_VERIFIED_SOURCE_FOR_SAME_CLAIM");uncertainty.add("NO_PROOF_OF_CONTINUOUS_TRUTH");
            return proposal(ConsolidationRelation.SUPPORTS,sa.containsAll(sb)?"A_TO_B":"B_TO_A",reasons,uncertainty);
        }
        if(samePoint || sharedSource || historicalA && x.year!=null) {
            reasons.add("COMPLETE_CLAIM_POLARITY_QUALIFIERS_AND_TIME_MATCH");
            if(!a.content().equals(b.content())) uncertainty.add("LEXICAL_EQUIVALENCE_REQUIRES_REVIEW");
            if(historicalA && !samePoint) uncertainty.add("YEAR_GRANULARITY_ONLY");
            return proposal(ConsolidationRelation.SAME_AS,"SYMMETRIC",reasons,uncertainty);
        }
        if(dated && !historicalA) {
            reasons.add("CONSISTENT_EXPLICIT_REASSERTION_AT_ANOTHER_TIME");uncertainty.add("NO_PROOF_OF_CONTINUOUS_TRUTH");
            return proposal(ConsolidationRelation.SUPPORTS,a.occurredAt().isAfter(b.occurredAt())?"A_TO_B":"B_TO_A",reasons,uncertainty);
        }
        return skip("TIME_ANCHOR_MISSING");
    }
    private Claim parse(String input) {
        if(input==null||input.isBlank()||input.length()>1000) return null;
        String raw=trim(input);
        if(raw.matches("(?is).*(并且|而且|同时|以及|但是| and | but | or |并非不|不是不|不一定|未必|not always|not necessarily|don't dislike|do not dislike).*")) return null;
        if(raw.matches("(?s).*[。！？!?;；\n].*")) return null;
        String lower=raw.toLowerCase(Locale.ROOT);
        if(lower.startsWith("如果") && raw.contains("，我可能")) {
            int at=raw.indexOf("，我可能");return new Claim("INTENT",normal(raw.substring(at+4)),false,"CONDITIONAL",normal(raw.substring(2,at)),RequestedTime.EVIDENCE_TIME,null);
        }
        for(String prefix:List.of("我可能","我希望","我想要","i might ","i hope to ","i want to ")) if(lower.startsWith(prefix))
            return new Claim("INTENT",normal(raw.substring(prefix.length())),false,prefix.contains("可能")||prefix.contains("might")?"POSSIBLE":"INTENTION","",RequestedTime.EVIDENCE_TIME,null);
        var p=parser.proposition(raw);if(!p.resolved()) return null;
        return new Claim(p.predicate(),p.object(),p.negative(),"ACTUAL","",p.requestedTime(),p.explicitYear());
    }
    private boolean supported(MemoryView m,Claim claim) {
        if(m.evidence().isEmpty()) return false;
        if(m.occurredAt()!=null && m.evidence().stream().noneMatch(e->m.occurredAt().equals(e.occurredAt()))) return false;
        if(claim.lexicalTime==RequestedTime.HISTORICAL && !"HISTORICAL".equals(m.temporalScope())) return false;
        if(claim.lexicalTime==RequestedTime.CURRENT && "HISTORICAL".equals(m.temporalScope())) return false;
        for(var evidence:m.evidence()) {
            if(!"user".equals(evidence.role()) || evidence.excerpt()==null || evidence.excerpt().length()>12000) return false;
            boolean found=false;
            for(String part:evidence.excerpt().split("[。.!?！？;；\\n]+")) {
                Claim e=parse(part);
                if(e==null) continue;
                if(e.sameCore(claim) && e.sameQualification(claim) && e.negative!=claim.negative && e.lexicalTime==claim.lexicalTime && Objects.equals(e.year,claim.year)) return false;
                if(e.sameCore(claim) && e.sameQualification(claim) && e.negative==claim.negative
                        && e.lexicalTime==claim.lexicalTime && Objects.equals(e.year,claim.year)) found=true;
            }
            if(!found) return false;
        }
        return true;
    }
    private static boolean explicitHistorical(MemoryView a,MemoryView b) {
        return List.of(a.content(),b.content()).stream().anyMatch(s->s.matches("(?is).*(以前|过去|曾经|used to|previously|formerly).*"));
    }
    private static Set<UUID> sourceIds(MemoryView m) {return m.evidence().stream().map(e->e.sourceId()).collect(java.util.stream.Collectors.toSet());}
    private static String trim(String s) {return s.strip().replaceAll("[。.!?！？]+$","").strip();}
    private static String normal(String s) {return trim(s).toLowerCase(Locale.ROOT).replaceAll("\\s+","");}
    private static Result skip(String reason) {return new Result(null,reason);}
    private static Result proposal(ConsolidationRelation r,String direction,List<String> reasons,List<String> uncertainty) {
        return new Result(new Proposal(r,direction,List.copyOf(reasons),List.copyOf(uncertainty)),null);
    }
}
