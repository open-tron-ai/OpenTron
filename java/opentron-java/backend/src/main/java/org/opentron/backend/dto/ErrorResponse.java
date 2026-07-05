package org.opentron.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Standard error response DTO for all error scenarios.
 */
public class ErrorResponse {
    @JsonProperty("status")
    private int status;

    @JsonProperty("error")
    private String error;

    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("path")
    private String path;

    public ErrorResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public ErrorResponse(int status, String error, String message) {
        this();
        this.status = status;
        this.error = error;
        this.message = message;
    }

    public ErrorResponse(int status, String error, String message, String path) {
        this(status, error, message);
        this.path = path;
    }

    // Getters and Setters
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
