package com.mirrorself.evaluation;

import com.mirrorself.model.ModelFailure;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EvaluationGatewayTest {
    @Test void quotaIsEnforcedBeforeDelegateWithoutRetainingOrEchoingInput() {
        var actual=new AtomicInteger();var gateway=new EvaluationGateway((o,g,i,t,s)->{actual.incrementAndGet();return "{}";},2);
        gateway.generate(null,null,"PRIVATE_PROMPT","PRIVATE_INPUT",List.of());gateway.generate(null,null,"","",List.of());
        var failure=assertThrows(ModelFailure.class,()->gateway.generate(null,null,"PRIVATE_PROMPT","PRIVATE_INPUT",List.of()));
        assertEquals(ModelFailure.Code.EGRESS_DENIED,failure.code());assertNull(failure.getCause());
        assertEquals("EGRESS_DENIED",failure.getMessage());assertEquals(2,actual.get());assertEquals(2,gateway.calls());
        assertEquals(1,gateway.budgetDenied());assertEquals(2,gateway.callLimit());
    }
    @Test void concurrentReservationsCannotExceedApprovedLimit() throws Exception {
        var actual=new AtomicInteger();var gateway=new EvaluationGateway((o,g,i,t,s)->{actual.incrementAndGet();return "{}";},3);
        try(var pool=Executors.newFixedThreadPool(6)) {
            List<Future<Boolean>> tasks=new ArrayList<>();
            for(int i=0;i<12;i++)tasks.add(pool.submit(()->{
                try {gateway.generate(null,null,"","",List.of());return true;}
                catch(ModelFailure failure){assertEquals(ModelFailure.Code.EGRESS_DENIED,failure.code());return false;}
            }));
            int accepted=0;for(var task:tasks)if(task.get())accepted++;
            assertEquals(3,accepted);assertEquals(3,actual.get());assertEquals(9,gateway.budgetDenied());
        }
    }
}
