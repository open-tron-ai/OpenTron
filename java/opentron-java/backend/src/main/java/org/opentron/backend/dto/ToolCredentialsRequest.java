package org.opentron.backend.dto;

import java.util.Map;

public class ToolCredentialsRequest {
    private Map<String, Object> credentials;

    public ToolCredentialsRequest() {}

    public ToolCredentialsRequest(Map<String, Object> credentials) {
        this.credentials = credentials;
    }

    public Map<String, Object> getCredentials() {
        return credentials;
    }

    public void setCredentials(Map<String, Object> credentials) {
        this.credentials = credentials;
    }
}
