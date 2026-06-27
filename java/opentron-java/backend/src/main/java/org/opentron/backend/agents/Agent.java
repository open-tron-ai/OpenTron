package org.opentron.backend.agents;

import java.util.ArrayList;
import java.util.List;

public class Agent {
    private String id;
    private String name;
    private String status;
    private List<String> messages = new ArrayList<>();

    public Agent() {}

    public Agent(String id, String name) {
        this.id = id;
        this.name = name;
        this.status = "running";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void addMessage(String msg) {
        this.messages.add(msg);
    }
}
