package org.opentron.backend.compose;

public class ComposeRunRequest {
    private String name;
    private String query;

    public ComposeRunRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
