package org.opentron.backend.memory;

public class MemorySearchRequest {
    private String query;
    private int limit = 10;

    public MemorySearchRequest() {}

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
