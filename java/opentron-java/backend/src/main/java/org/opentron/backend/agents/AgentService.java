package org.opentron.backend.agents;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgentService {

    private final Map<String, Agent> agents = new ConcurrentHashMap<>();

    public List<Agent> listAgents() {
        return new ArrayList<>(agents.values());
    }

    public Agent createAgent(AgentCreateRequest req) {
        String id = UUID.randomUUID().toString();
        Agent a = new Agent(id, req.getName() == null ? id : req.getName());
        // store config if needed (ignored for now)
        agents.put(id, a);
        return a;
    }

    public boolean killAgent(String id) {
        Agent a = agents.remove(id);
        return a != null;
    }

    public Agent sendMessage(String id, MessageRequest msg) {
        Agent a = agents.get(id);
        if (a == null) return null;
        a.addMessage(msg.getContent());
        return a;
    }

    public Agent getAgent(String id) {
        return agents.get(id);
    }
}
