package org.opentron.backend.memory;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MemoryServiceTest {

    @Test
    public void storeSearchAndPersistence() throws Exception {
        Path tmp = Files.createTempFile("memory-test", ".jsonl");
        tmp.toFile().deleteOnExit();

        MemoryService svc = new MemoryService(tmp.toString());
        MemoryStoreRequest req = new MemoryStoreRequest();
        req.setText("Hello world");

        var e = svc.store(req);
        assertNotNull(e.getId());
        assertEquals("Hello world", e.getText());

        List<MemoryEntry> res = svc.search(new MemorySearchRequest());
        assertTrue(res.size() >= 1);

        // Reload from file
        MemoryService svc2 = new MemoryService(tmp.toString());
        List<MemoryEntry> res2 = svc2.search(new MemorySearchRequest());
        boolean found = res2.stream().anyMatch(x -> x.getId().equals(e.getId()));
        assertTrue(found);
    }
}
