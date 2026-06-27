package org.opentron.backend.agents;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AgentServiceTest {

    @Test
    public void createListGetSendKill() {
        AgentService svc = new AgentService();

        AgentCreateRequest req = new AgentCreateRequest();
        req.setName("test-agent");

        Agent a = svc.createAgent(req);
        assertNotNull(a.getId());
        assertEquals("test-agent", a.getName());
        assertEquals("running", a.getStatus());

        assertEquals(1, svc.listAgents().size());

        Agent fetched = svc.getAgent(a.getId());
        assertNotNull(fetched);

        MessageRequest msg = new MessageRequest();
        msg.setContent("hello");
        Agent after = svc.sendMessage(a.getId(), msg);
        assertNotNull(after);
        assertTrue(after.getMessages().contains("hello"));

        boolean killed = svc.killAgent(a.getId());
        assertTrue(killed);
        assertNull(svc.getAgent(a.getId()));
    }
}
