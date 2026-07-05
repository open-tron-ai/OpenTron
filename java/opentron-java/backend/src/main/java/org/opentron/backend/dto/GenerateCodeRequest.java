package org.opentron.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class GenerateCodeRequest {
    @NotBlank
    private String request;
    private String language;
    private String context;

    public GenerateCodeRequest() {}

    public GenerateCodeRequest(String request, String language, String context) {
        this.request = request;
        this.language = language;
        this.context = context;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
