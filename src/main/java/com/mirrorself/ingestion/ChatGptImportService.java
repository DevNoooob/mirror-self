package com.mirrorself.ingestion;

import com.mirrorself.memory.MemoryExtractionService;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatGptImportService {
    public record ImportReport(UUID jobId,int importedSources,int existingSources,int newMemories,
            List<UUID> extractionJobIds) { }
    private final SourceRepository sources;
    private final MemoryExtractionService extraction;
    private final JdbcTemplate jdbc;
    public ChatGptImportService(SourceRepository sources,MemoryExtractionService extraction,JdbcTemplate jdbc) {
        this.sources=sources; this.extraction=extraction; this.jdbc=jdbc;
    }
    public ImportReport load(UUID owner,String filename,InputStream input) throws IOException {
        UUID jobId=UUID.randomUUID();
        jdbc.update("INSERT INTO ingestion_job(id,owner_id,source_system,file_name,status) VALUES(?,?,'CHATGPT',?,'RUNNING')",
                jobId,owner,filename);
        AtomicInteger imported=new AtomicInteger(), existing=new AtomicInteger(), memories=new AtomicInteger();
        List<UUID> jobs=new ArrayList<>();
        try {
            new ChatGptDecoder().decodeConversations(input,conversation->{
                Map<String,UUID> saved=new LinkedHashMap<>(); Set<String> changed=new HashSet<>();
                // L0 commits before any remote I/O; model failures never destroy imported data.
                for(var record:conversation.sources()) {
                    var source=sources.save(owner,record); saved.put(record.locator(),source.id());
                    if(source.inserted()) { imported.incrementAndGet(); changed.add(record.locator()); }
                    else existing.incrementAndGet();
                }
                for(var path:conversation.paths()) {
                    if(path.isEmpty()||path.stream().noneMatch(changed::contains)) continue;
                    var report=extraction.extract(owner,path.stream().map(saved::get).filter(Objects::nonNull).toList(),null);
                    memories.addAndGet(report.newMemories()); jobs.add(report.id());
                }
            });
            finish(owner,jobId,"COMPLETE",imported.get(),memories.get());
            return new ImportReport(jobId,imported.get(),existing.get(),memories.get(),List.copyOf(jobs));
        } catch(IOException|RuntimeException ex) {
            finish(owner,jobId,"FAILED",imported.get(),memories.get()); throw ex;
        }
    }
    private void finish(UUID owner,UUID id,String status,int sources,int memories) {
        jdbc.update("UPDATE ingestion_job SET status=?,source_count=?,memory_count=?,finished_at=now() WHERE id=? AND owner_id=?",
                status,sources,memories,id,owner);
    }
}
