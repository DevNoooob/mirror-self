package com.mirrorself.retrieval.evidence;

import com.mirrorself.memory.*;
import com.mirrorself.memory.MemoryRepository.MemoryView;
import java.util.*;
import java.util.regex.*;
import org.springframework.stereotype.Component;

/** Bounded bilingual rule contract, not an NLI model. Unknown paraphrases abstain.
 * Decisions use user L0 quotations, never cosine scores or reference answers. */
@Component
public class ConservativeEvidenceJudge implements EvidenceSupportJudge {
    public static final String VERSION="evidence-rules-1.1";
    private static final Pattern YEAR=Pattern.compile("\\b(19[0-9]{2}|20[0-9]{2})\\b");
    private static final Set<String> STOP=Set.of("do","did","am","are","is","there","have","has","that","the","what","myself",
            "my","me","in","at","to","of","and","or","on","was","can","been","with","not","like","work","person","evidence");
    @Override public Proposition proposition(String input) {
        if(input==null || input.isBlank() || input.length()>1000) throw new IllegalArgumentException("question must contain 1-1000 characters");
        String lower=input.toLowerCase(Locale.ROOT);
        RequestedTime time=lower.matches("(?s).*(现在|目前|当前|如今|still|currently|\\bnow\\b).*")?RequestedTime.CURRENT:
                lower.matches("(?s).*(以前|过去|两年前|曾经|previously|formerly|used to|\\b202[0-9]\\b|\\bdid\\b).*")?RequestedTime.HISTORICAL:RequestedTime.EVIDENCE_TIME;
        Integer year=null;var years=YEAR.matcher(lower);if(years.find()) year=Integer.valueOf(years.group(1));
        String text=clean(lower).replaceAll("在消息时点|在记录时点|消息时点|是否|请问|现在|目前|当前|如今|以前|过去|曾经|还|吗", "")
                .replace("自认为","认为自己").replaceAll("\\b(currently|still|now|previously|formerly)\\b", "")
                .replaceAll("\\b(?:in )?(?:19|20)[0-9]{2}\\b", "").replace("used to ","").strip();
        if(lower.matches("(?s).*(并非不|不是不|not always|not necessarily|do not dislike|don't dislike|不一定|未必|所有|总是|always|never).*"))
            return unresolved(time,year);
        var chinese=Pattern.compile("^我(不喜欢|喜欢|不喝|喝|不住在|住在|不使用|使用|没有学过|学过|学习过|正在学习|学习)(.+)$").matcher(text.replace(" ",""));
        if(chinese.matches()) {
            String verb=chinese.group(1);String predicate=switch(verb) {
                case "喜欢","不喜欢" -> "LIKE";case "喝","不喝" -> "DRINK";case "住在","不住在" -> "LIVE_IN";
                case "使用","不使用" -> "USE";default -> "STUDY";
            };
            return resolved(predicate,chinese.group(2),verb.startsWith("不")||verb.startsWith("没有"),time,year);
        }
        var self=Pattern.compile("^我(?:觉得|认为)自己(?:很)?(.+)$").matcher(text.replace(" ",""));
        if(self.matches()) return resolved("SELF_REPORT",self.group(1),false,time,year);
        text=text.replaceFirst("^(?:do|did) +","").replaceAll(" +"," ").strip();
        var english=Pattern.compile("^i (?:(do not|don't|did not|didn't|not) )?(like|dislike|drink|live in|lived in|use|study|am learning|consider myself|commute by) (.+)$").matcher(text);
        if(english.matches()) {
            String verb=english.group(2);String predicate=switch(verb) {
                case "like","dislike" -> "LIKE";case "drink" -> "DRINK";case "live in","lived in" -> "LIVE_IN";
                case "use" -> "USE";case "consider myself" -> "SELF_REPORT";case "commute by" -> "COMMUTE_BY";default -> "STUDY";
            };
            return resolved(predicate,english.group(3),english.group(1)!=null||verb.equals("dislike"),time,year);
        }
        return unresolved(time,year);
    }
    @Override public Decision assess(String question,MemoryView memory) {
        var target=proposition(question);var reasons=new LinkedHashSet<String>();var ids=new ArrayList<UUID>();
        if(memory.status()!=MemoryStatus.ACTIVE || memory.sensitivity()!=Sensitivity.NORMAL || memory.evidence().isEmpty())
            return decision(memory,target,Relation.INSUFFICIENT_EVIDENCE,Relation.INSUFFICIENT_EVIDENCE,ids,List.of("INELIGIBLE_OR_MISSING_EVIDENCE"));
        Set<Relation> direct=new HashSet<>();boolean related=false;
        for(var quote:memory.evidence()) {
            if(!"user".equals(quote.role()) || quote.excerpt()==null || quote.excerpt().isBlank())
                return decision(memory,target,Relation.INSUFFICIENT_EVIDENCE,Relation.INSUFFICIENT_EVIDENCE,List.of(),List.of("NON_USER_OR_EMPTY_EVIDENCE"));
            if(quote.excerpt().length()>1000)
                return decision(memory,target,Relation.INSUFFICIENT_EVIDENCE,Relation.INSUFFICIENT_EVIDENCE,
                    List.of(quote.sourceId()),List.of("EVIDENCE_TOO_LONG_FOR_RULE_CONTRACT"));
            ids.add(quote.sourceId());var source=proposition(quote.excerpt());
            boolean same=target.resolved() && source.resolved() && target.predicate().equals(source.predicate()) && target.object().equals(source.object());
            if(same) direct.add(target.negative()==source.negative()?Relation.SUPPORTS:Relation.REFUTES);
            else if(topic(question,quote.excerpt())) related=true;
        }
        Relation raw=direct.size()>1?Relation.INSUFFICIENT_EVIDENCE:direct.size()==1?direct.iterator().next():related?Relation.RELATED_ONLY:Relation.INSUFFICIENT_EVIDENCE;
        Relation result=raw;
        if(direct.size()>1) reasons.add("CONFLICTING_QUOTES");
        if(memory.origin()==AssertionOrigin.MODEL_INFERRED || memory.type()==MemoryType.GOAL
                || memory.evidence().stream().anyMatch(e->e.excerpt().matches("(?is).*(如果|可能|考虑|希望|想要|might|may |hope |would |if ).*"))) {
            result=raw==Relation.INSUFFICIENT_EVIDENCE?raw:Relation.RELATED_ONLY;reasons.add("INFERENCE_OR_CONDITIONAL_NOT_ESTABLISHED_FACT");
        }
        if(memory.type()==MemoryType.THOUGHT_PROCESS && !target.predicate().equals("SELF_REPORT")) {
            result=raw==Relation.INSUFFICIENT_EVIDENCE?raw:Relation.RELATED_ONLY;reasons.add("SELF_REPORT_NOT_OBJECTIVE_PERSONALITY");
        }
        if(result==Relation.SUPPORTS || result==Relation.REFUTES) {
            if(target.requestedTime()==RequestedTime.CURRENT) {result=Relation.RELATED_ONLY;reasons.add("CURRENT_ENDORSEMENT_UNCONFIRMED");}
            else if((target.requestedTime()==RequestedTime.HISTORICAL)!=memory.temporalScope().equals("HISTORICAL")) {
                result=Relation.RELATED_ONLY;reasons.add("TIME_SCOPE_MISMATCH");
            }
            if(target.explicitYear()!=null && memory.evidence().stream().noneMatch(e->target.explicitYear().equals(proposition(e.excerpt()).explicitYear()))) {
                result=Relation.RELATED_ONLY;reasons.add("YEAR_NOT_SUPPORTED");
            }
            if(question.contains("两年前")) {result=Relation.RELATED_ONLY;reasons.add("RELATIVE_TIME_UNRESOLVED");}
        }
        if(!target.resolved()) reasons.add("PROPOSITION_UNRESOLVED");
        if(!memory.conflicts().isEmpty()) reasons.add("CONTRADICTION_RECORDED_NO_AUTOMATIC_RESOLUTION");
        if(reasons.isEmpty()) reasons.add(result==Relation.SUPPORTS || result==Relation.REFUTES?"MATCHED_USER_QUOTATION_POLARITY":"NO_DIRECT_MATCH_ABSTAIN");
        return decision(memory,target,result,raw,ids,List.copyOf(reasons));
    }
    private Decision decision(MemoryView memory,Proposition target,Relation relation,Relation source,List<UUID> ids,List<String> reasons) {
        return new Decision(memory.id(),target,relation,source,memory.temporalScope().equals("HISTORICAL")?"HISTORICAL":"SOURCE_TIME_ONLY",
                "UNCONFIRMED",List.copyOf(ids),reasons);
    }
    private static Proposition unresolved(RequestedTime time,Integer year) {return new Proposition("SELF","UNRESOLVED","",false,time,year,false);}
    private static Proposition resolved(String predicate,String object,boolean negative,RequestedTime time,Integer year) {
        return new Proposition("SELF",predicate,object(object),negative,time,year,true);
    }
    private static String clean(String text) {return text.replaceAll("[。！？?!.,，：:;；]", "").strip();}
    private static String object(String value) {
        String text=clean(value.toLowerCase(Locale.ROOT)).replaceAll("\\b(the|a|an) +", "").replace(" ","");
        return switch(text) {
            case "repetitivework","重复性工作","重复性劳动" -> "重复劳动";
            case "gardening" -> "园艺";case "coffee" -> "咖啡";case "tea" -> "茶";case "patient" -> "耐心";
            default -> text;
        };
    }
    private static boolean topic(String question,String evidence) {
        String target=clean(question.toLowerCase(Locale.ROOT)).replaceAll("现在|是否|已经|以前|目前|消息时点|我|吗", "");
        String source=clean(evidence.toLowerCase(Locale.ROOT));
        for(String token:SearchTerms.from(target)) if(!STOP.contains(token) && source.contains(token)) return true;
        return false;
    }
}
