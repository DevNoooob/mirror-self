package com.mirrorself.memory;

import com.mirrorself.config.ExtractionProperties;
import com.mirrorself.ingestion.SourceRepository.SourceView;
import java.util.*;

/** Conservative escaped-JSON character budget, with two-message overlap at batch boundaries. */
public final class ConversationBatcher {
    private ConversationBatcher() { }
    public static List<List<SourceView>> batches(List<SourceView> input, ExtractionProperties properties) {
        List<List<SourceView>> result=new ArrayList<>();
        List<SourceView> current=new ArrayList<>();
        int sliceSize=(properties.batchChars()-512)/6;
        for (var source : input) {
            if (source.text().isBlank()) continue;
            int overlap=Math.min(100,sliceSize/4);
            for (int start=0; start<source.text().length();) {
                int end=Math.min(source.text().length(),start+sliceSize);
                var slice=new SourceView(source.id(),source.sourceSystem(),source.locator(),source.title(),source.role(),
                        source.text().substring(start,end),"{}",source.occurredAt());
                if (!current.isEmpty() && !fits(current,slice,properties)) {
                    result.add(List.copyOf(current));
                    current=new ArrayList<>(current.subList(Math.max(0,current.size()-2),current.size()));
                    while (!current.isEmpty() && !fits(current,slice,properties)) current.removeFirst();
                }
                current.add(slice);
                if (end == source.text().length()) break;
                start=end-overlap;
            }
        }
        if (!current.isEmpty()) result.add(List.copyOf(current));
        return result;
    }
    private static boolean fits(List<SourceView> batch, SourceView next, ExtractionProperties p) {
        return batch.size()<p.batchMessages()
                && batch.stream().mapToInt(m->m.text().length()*6+512).sum()+next.text().length()*6+512<=p.batchChars();
    }
}
