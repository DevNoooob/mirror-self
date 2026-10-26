package com.mirrorself.memory;

import com.mirrorself.config.ExtractionProperties;
import com.mirrorself.ingestion.SourceRepository;
import com.mirrorself.ingestion.SourceRepository.SourceView;
import com.mirrorself.model.ModelFailure;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Model I/O runs outside database transactions. Each accepted batch commits atomically. */
@Service
public class MemoryExtractionService {
    private static final Logger log=LoggerFactory.getLogger(MemoryExtractionService.class);
    private final SourceRepository sources;
    private final MemoryExtractor extractor;
    private final MemoryValidator validator;
    private final MemoryService memories;
    private final ExtractionJobRepository jobs;
    private final ExtractionProperties properties;
    private final com.mirrorself.model.ModelAuthorization authorization;
    public MemoryExtractionService(SourceRepository sources, RuleBasedExtractor rule, LlmMemoryExtractor llm,
            MemoryValidator validator, MemoryService memories, ExtractionJobRepository jobs, ExtractionProperties properties,
            com.mirrorself.model.ModelAuthorization authorization) {
        this.sources=sources; this.extractor=properties.mode()==ExtractionProperties.Mode.LLM?llm:rule;
        this.authorization=authorization; this.validator=validator; this.memories=memories; this.jobs=jobs; this.properties=properties;
    }
    public ExtractionJobRepository.Job extract(UUID owner,List<UUID> ids,UUID grant) {
        if (ids==null || ids.isEmpty()) throw new IllegalArgumentException("source IDs required");
        List<SourceView> messages=new ArrayList<>();
        for (UUID id:new LinkedHashSet<>(ids)) messages.add(sources.get(owner,id));
        // Never mix unrelated conversations. The repository reconstructs parent order when available.
        messages=SourceConversations.order(messages);
        UUID job=jobs.start(owner,extractor,messages.stream().map(SourceView::id).toList());
        int accepted=0,rejected=0,created=0,completed=0;
        Set<String> errors=new LinkedHashSet<>();
        boolean blocked=false;
        for (var batch:ConversationBatcher.batches(messages,properties)) {
            try {
                if(properties.mode()==ExtractionProperties.Mode.LLM) authorization.check(owner,grant,batch);
                List<ExtractedMemory> proposals=extractor.extract(owner,batch,grant);
                List<ExtractedMemory> checked=new ArrayList<>();
                for (var memory:proposals) {
                    if (validator.evidenceValid(owner,memory,batch)) checked.add(memory);
                    else { rejected++; errors.add("INVALID_EVIDENCE"); }
                }
                Set<Integer> supported=properties.mode()==ExtractionProperties.Mode.LLM
                        ?validator.supported(owner,grant,batch,checked):java.util.stream.IntStream.range(0,checked.size())
                                .boxed().collect(java.util.stream.Collectors.toSet());
                List<ExtractedMemory> valid=new ArrayList<>();
                for(int i=0;i<checked.size();i++) {
                    if(supported.contains(i)) valid.add(checked.get(i));
                    else { rejected++; errors.add("UNSUPPORTED_CLAIM"); }
                }
                created+=memories.persistValidated(owner,job,extractor.version(),extractor.model(),valid,batch);
                accepted+=valid.size(); completed++;
            } catch (ModelFailure ex) {
                errors.add(ex.code().name()); blocked=ex.code()==ModelFailure.Code.EGRESS_DENIED;
                log.warn("Extraction job {}: {}",job,ex.code());
                // Stop on authorization failures; do not send any later batch.
                if(blocked) break;
            } catch (RuntimeException ex) {
                errors.add("STORAGE_FAILED"); log.warn("Extraction job {}: STORAGE_FAILED",job); break;
            }
        }
        String status=errors.isEmpty()?"COMPLETE":accepted>0||completed>0?"PARTIAL":blocked?"BLOCKED":"FAILED";
        jobs.finish(owner,job,status,accepted,rejected,created,errors);
        return jobs.get(owner,job);
    }
}
