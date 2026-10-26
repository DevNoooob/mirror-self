package com.mirrorself.memory;

import com.google.gson.*;
import com.mirrorself.ingestion.SourceRepository.SourceView;
import java.util.*;

/** Parent links are authoritative. Missing links retain caller order, never invent adjacency. */
public final class SourceConversations {
    private SourceConversations() { }
    public static String conversation(SourceView s) {
        int colon=s.locator().lastIndexOf(':');
        return s.sourceSystem()+":"+(colon<0?s.locator():s.locator().substring(0,colon));
    }
    public static List<SourceView> order(List<SourceView> input) {
        if(input.stream().map(SourceConversations::conversation).distinct().count()!=1)
            throw new IllegalArgumentException("extract one conversation at a time");
        if(input.stream().allMatch(s->s.occurredAt()!=null)) {
            input=input.stream().sorted(java.util.Comparator.comparing(SourceView::occurredAt)).toList();
        }
        Map<String,SourceView> nodes=new LinkedHashMap<>();
        Map<String,String> parents=new HashMap<>();
        for(var s:input) {
            try {
                var node=JsonParser.parseString(s.rawJson()).getAsJsonObject();
                String id=node.has("id")&&!node.get("id").isJsonNull()?node.get("id").getAsString():s.locator();
                if(nodes.put(id,s)!=null) throw new IllegalArgumentException("select one version of each source message");
                if(node.has("parent")&&!node.get("parent").isJsonNull()) parents.put(id,node.get("parent").getAsString());
            } catch(RuntimeException ex) { throw new IllegalArgumentException("invalid stored source metadata"); }
        }
        Map<String,Integer> children=new HashMap<>();
        for(var entry:parents.entrySet()) {
            if(children.merge(entry.getValue(),1,Integer::sum)>1)
                throw new IllegalArgumentException("select a single conversation branch");
        }
        List<SourceView> ordered=new ArrayList<>(); Set<String> done=new HashSet<>();
        for(String id:nodes.keySet()) {
            List<String> chain=new ArrayList<>(); Set<String> visiting=new HashSet<>();
            String cursor=id;
            while(cursor!=null&&nodes.containsKey(cursor)&&!done.contains(cursor)) {
                if(!visiting.add(cursor)) throw new IllegalArgumentException("cyclic conversation metadata");
                chain.add(cursor);cursor=parents.get(cursor);
            }
            for(int i=chain.size()-1;i>=0;i--) if(done.add(chain.get(i))) ordered.add(nodes.get(chain.get(i)));
        }
        return ordered;
    }
}
