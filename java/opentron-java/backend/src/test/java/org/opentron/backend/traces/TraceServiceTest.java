package org.opentron.backend.traces;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TraceServiceTest {

    @Test
    public void addListGetAndPersistence() throws Exception {
        Path tmp = Files.createTempFile("traces-test", ".jsonl");
        tmp.toFile().deleteOnExit();

        TraceService svc = new TraceService(tmp.toString());
        Map<String, Object> payload = new HashMap<>();
        payload.put("x", 1);

        TraceEntry e = svc.addTrace("event", payload);
        assertNotNull(e.getId());
        assertEquals("event", e.getKind());

        List<TraceEntry> list = svc.listTraces(10);
        assertEquals(1, list.size());

        TraceEntry fetched = svc.getTrace(e.getId());
        assertNotNull(fetched);

        // create a new service instance pointing to same file — should load existing trace
        TraceService svc2 = new TraceService(tmp.toString());
        List<TraceEntry> list2 = svc2.listTraces(10);
        assertTrue(list2.size() >= 1);
        boolean found = list2.stream().anyMatch(t -> t.getId().equals(e.getId()));
        assertTrue(found);
    }
}
