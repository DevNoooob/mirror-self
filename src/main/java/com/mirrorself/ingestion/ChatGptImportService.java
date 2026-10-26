package com.mirrorself.ingestion;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatGptImportService {
    public record ImportReport(UUID jobId, int importedSources, int existingSources, int newMemories) { }

    private final IngestionUnitService ingest;
    private final JdbcTemplate jdbc;

    public ChatGptImportService(IngestionUnitService ingest, JdbcTemplate jdbc) {
        this.ingest = ingest;
        this.jdbc = jdbc;
    }

    public ImportReport load(UUID owner, String filename, InputStream input) throws IOException {
        UUID jobId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO ingestion_job(id,owner_id,source_system,file_name,status)
                VALUES(?,?,'CHATGPT',?,'RUNNING')
                """, jobId, owner, filename);
        AtomicInteger imported = new AtomicInteger();
        AtomicInteger existing = new AtomicInteger();
        AtomicInteger memories = new AtomicInteger();
        try {
            new ChatGptDecoder().decode(input, record -> {
                IngestionUnitService.SavedUnit result = ingest.ingest(owner, record);
                if (result.newSource()) imported.incrementAndGet();
                else existing.incrementAndGet();
                memories.addAndGet(result.newMemories());
            });
            jdbc.update("""
                    UPDATE ingestion_job SET status='COMPLETE',source_count=?,memory_count=?,
                        finished_at=now() WHERE id=? AND owner_id=?
                    """, imported.get(), memories.get(), jobId, owner);
            return new ImportReport(jobId, imported.get(), existing.get(), memories.get());
        } catch (IOException | RuntimeException e) {
            jdbc.update("""
                    UPDATE ingestion_job SET status='FAILED',source_count=?,memory_count=?,
                        finished_at=now() WHERE id=? AND owner_id=?
                    """, imported.get(), memories.get(), jobId, owner);
            throw e;
        }
    }
}
