package com.mirrorself.memory;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonObject;
import com.mirrorself.ingestion.SourceRepository.SourceView;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class SourceConversationsTest {
    SourceView node(String conversation,String id,String parent) {
        JsonObject raw=new JsonObject();raw.addProperty("id",id);if(parent!=null) raw.addProperty("parent",parent);
        return new SourceView(UUID.randomUUID(),"CHATGPT",conversation+":"+id,"test","user","fictional",raw.toString(),null);
    }
    @Test void reconstructsParentsAndRejectsCrossConversationOrBranchedRequests() {
        var root=node("c","r",null);var a=node("c","a","r");var b=node("c","b","r");
        assertEquals(List.of(root,a),SourceConversations.order(List.of(a,root)));
        assertThrows(IllegalArgumentException.class,()->SourceConversations.order(List.of(root,a,b)));
        assertThrows(IllegalArgumentException.class,()->SourceConversations.order(List.of(a,node("other","x",null))));
        assertThrows(IllegalArgumentException.class,()->SourceConversations.order(List.of(node("c","x","y"),node("c","y","x"))));
    }
    @Test void handlesLongParentChainsWithoutRecursiveStackOverflow() {
        List<SourceView> nodes=new ArrayList<>();
        for(int i=0;i<10000;i++) nodes.add(node("long","n"+i,i==0?null:"n"+(i-1)));
        Collections.reverse(nodes);
        assertEquals("long:n0",SourceConversations.order(nodes).getFirst().locator());
    }
}
