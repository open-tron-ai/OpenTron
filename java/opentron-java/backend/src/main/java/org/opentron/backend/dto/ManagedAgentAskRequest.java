package org.opentron.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class ManagedAgentAskRequest {
    @NotBlank
    private String question;

    public ManagedAgentAskRequest() {}

    public ManagedAgentAskRequest(String question) {
        this.question = question;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
