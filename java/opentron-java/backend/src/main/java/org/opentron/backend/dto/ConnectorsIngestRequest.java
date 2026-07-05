package org.opentron.backend.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public class ConnectorsIngestRequest {
    private String source;
    @NotBlank
    private String content;
    private Map<String, Object> metadata;

    public ConnectorsIngestRequest() {}

    public ConnectorsIngestRequest(String source, String content, Map<String, Object> metadata) {
        this.source = source;
        this.content = content;
        this.metadata = metadata;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
