package org.opentron.backend.dto;

public class TelemetryTrackRequest {
    private Long tokens;
    private String event;

    public TelemetryTrackRequest() {}

    public TelemetryTrackRequest(Long tokens, String event) {
        this.tokens = tokens;
        this.event = event;
    }

    public Long getTokens() {
        return tokens;
    }

    public void setTokens(Long tokens) {
        this.tokens = tokens;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }
}
