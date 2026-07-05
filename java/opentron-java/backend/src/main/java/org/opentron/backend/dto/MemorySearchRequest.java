package org.opentron.backend.dto;

public class MemorySearchRequest {
    private String query;
    private String agent_name;
    private Integer top_k;

    public MemorySearchRequest() {}

    public MemorySearchRequest(String query, String agent_name, Integer top_k) {
        this.query = query;
        this.agent_name = agent_name;
        this.top_k = top_k;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getAgent_name() {
        return agent_name;
    }

    public void setAgent_name(String agent_name) {
        this.agent_name = agent_name;
    }

    public Integer getTop_k() {
        return top_k;
    }

    public void setTop_k(Integer top_k) {
        this.top_k = top_k;
    }
}
