package com.mirrorself.api;

import com.mirrorself.config.MirrorProperties;
import com.mirrorself.ingestion.ChatGptImportService;
import com.mirrorself.ingestion.SourceRepository;
import com.mirrorself.memory.MemoryService;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class ImportController {
    private final MirrorProperties properties;
    private final ChatGptImportService importer;
    private final SourceRepository sources;
    private final MemoryService memories;

    public ImportController(MirrorProperties properties, ChatGptImportService importer,
                            SourceRepository sources, MemoryService memories) {
        this.properties = properties;
        this.importer = importer;
        this.sources = sources;
        this.memories = memories;
    }

    @PostMapping(path = "/import/chatgpt", consumes = "multipart/form-data")
    public ChatGptImportService.ImportReport importChatGpt(@RequestParam("file") MultipartFile file)
            throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("empty file");
        String name = file.getOriginalFilename();
        if (name == null || !name.endsWith(".json")) {
            throw new IllegalArgumentException("upload conversations.json, not the ZIP archive");
        }
        return importer.load(properties.ownerId(), name, file.getInputStream());
    }

    @GetMapping("/sources/{id}")
    public SourceRepository.SourceView getSource(@PathVariable UUID id) {
        return sources.get(properties.ownerId(), id);
    }

    @DeleteMapping("/sources/{id}")
    public Map<String, Object> deleteSource(@PathVariable UUID id) {
        memories.deleteSource(properties.ownerId(), id);
        return Map.of("deleted", true, "id", id);
    }
}
