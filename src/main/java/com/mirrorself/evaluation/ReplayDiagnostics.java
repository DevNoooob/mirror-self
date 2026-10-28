package com.mirrorself.evaluation;

import com.mirrorself.memory.*;
import com.mirrorself.ingestion.SourceRepository.SourceView;
import java.util.*;
import static com.mirrorself.evaluation.EvaluationReport.*;

/** Text is used only in-process. Reports retain counts, opaque row IDs and source offsets. */
public final class ReplayDiagnostics {
    private ReplayDiagnostics() { }
    private record Output(MemoryType type,MemoryIdentity.Key identity,Sensitivity sensitivity) { }
    private static Map<Output,Long> signature(List<ExtractedMemory> values) {
        Map<Output,Long> result=new HashMap<>();
        values.forEach(v->result.merge(new Output(v.type(),MemoryIdentity.of(v),v.sensitivity()),1L,Long::sum));
        return result;
    }
    public static List<String> ids(List<MemoryRepository.MemoryView> values) {
        return values.stream().map(m->m.id().toString()).sorted().toList();
    }
    public static Round round(int n,List<ExtractionJobRepository.Job> jobs,List<ExtractedMemory> raw,
            Map<String,Integer> rejected,List<MemoryRepository.MemoryView> rows) {
        return new Round(n,raw.size(),jobs.stream().mapToInt(ExtractionJobRepository.Job::acceptedCount).sum(),
                jobs.stream().mapToInt(ExtractionJobRepository.Job::newMemories).sum(),
                jobs.stream().mapToInt(ExtractionJobRepository.Job::rejectedCount).sum(),
                jobs.stream().flatMap(j->j.errorCodes().stream()).distinct().sorted().toList(),
                jobs.stream().map(ExtractionJobRepository.Job::status).toList(),Map.copyOf(rejected),ids(rows),rows.size());
    }
    public static ReplayAttempt assess(Round first,Round replay,List<String> before,
            List<ExtractedMemory> initial,List<ExtractedMemory> raw,boolean extractionComparable,
            List<MemoryRepository.MemoryView> rows,Map<UUID,SourceView> sources,Map<UUID,String> messageIds) {
        Boolean consistent=extractionComparable?signature(initial).equals(signature(raw)):null;
        List<String> added=replay.memoryIds().stream().filter(id->!before.contains(id)).toList();
        List<String> removed=before.stream().filter(id->!replay.memoryIds().contains(id)).toList();
        boolean initialEqual=replay.memoryIds().equals(first.memoryIds()),previousEqual=replay.memoryIds().equals(before);
        boolean stable=replay.inserted()==0&&initialEqual;
        List<TypeDrift> drift=new ArrayList<>();
        Map<List<MemoryIdentity.Anchor>,List<ExtractedMemory>> grouped=new LinkedHashMap<>();
        java.util.stream.Stream.concat(initial.stream(),raw.stream()).forEach(v->grouped.computeIfAbsent(MemoryIdentity.of(v).anchors(),k->new ArrayList<>()).add(v));
        for(var group:grouped.entrySet()) {
            var types=group.getValue().stream().map(ExtractedMemory::type).distinct().sorted().toList();
            if(types.size()<2)continue;
            var pointers=group.getKey().stream().map(a->{
                var source=sources.get(a.sourceId());int start=source==null?-1:source.text().indexOf(a.quote());
                return new EvidencePointer(messageIds.getOrDefault(a.sourceId(),"unknown"),source==null?"unknown":source.role(),
                        start,start<0?-1:start+a.quote().length(),source!=null);
            }).toList();
            drift.add(new TypeDrift(pointers,types,group.getValue().stream().map(MemoryIdentity::of).distinct().count()==1));
        }
        Map<MemoryIdentity.Key,List<MemoryRepository.MemoryView>> duplicates=new LinkedHashMap<>();
        rows.forEach(v->duplicates.computeIfAbsent(MemoryIdentity.of(v),k->new ArrayList<>()).add(v));
        var exact=duplicates.values().stream().filter(v->v.size()>1).map(v->new DuplicateGroup(ids(v),
                v.stream().map(MemoryRepository.MemoryView::type).distinct().sorted().toList())).toList();
        List<String> reasons=new ArrayList<>();
        if(consistent==null)reasons.add("EXTRACTION_OR_AUTHORIZATION_FAILED");
        else if(!consistent)reasons.add("MODEL_OUTPUT_CHANGED");
        if(replay.extracted()==0)reasons.add("EMPTY_REPLAY_OUTPUT");
        if(replay.accepted()==0&&!first.memoryIds().isEmpty())reasons.add("LEGACY_NO_ACCEPTED_CANDIDATES");
        replay.rejectedByStage().forEach((stage,count)->{if(count>0)reasons.add(stage+"_REJECTED");});
        for(String error:replay.errorCodes())reasons.add("ERROR_"+error);
        if(!added.isEmpty())reasons.add("NEW_ROWS");if(!removed.isEmpty())reasons.add("REMOVED_ROWS");
        if(!initialEqual)reasons.add("ROW_SET_DIFFERS_FROM_FIRST");
        if(!drift.isEmpty())reasons.add("SAME_EVIDENCE_TYPE_DRIFT");
        if(!exact.isEmpty())reasons.add("EXACT_DUPLICATE_ROWS_OBSERVED");
        boolean legacyApplicable=first.memoryCount()>0;
        return new ReplayAttempt(replay,List.copyOf(before),added,removed,initialEqual,previousEqual,consistent,stable,
                legacyApplicable,legacyApplicable&&replay.accepted()>0&&stable, List.copyOf(drift),exact,List.copyOf(reasons));
    }
    public static ReplaySummary aggregate(List<ReplayCase> cases) {
        var attempts=cases.stream().flatMap(c->c.replays().stream()).toList();
        long comparable=attempts.stream().filter(a->a.modelOutputConsistent()!=null).count();
        long same=attempts.stream().filter(a->Boolean.TRUE.equals(a.modelOutputConsistent())).count();
        return new ReplaySummary("replay-1.0",Metric.of(same,comparable),
                Metric.of(attempts.stream().filter(ReplayAttempt::databaseRowSetIdempotent).count(),attempts.size()),
                attempts.size(),attempts.stream().mapToInt(a->a.exactDuplicateRows().size()).sum(),
                (int)attempts.stream().filter(a->!a.typeDrifts().isEmpty()).count());
    }
}
