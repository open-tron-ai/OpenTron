package org.opentron.backend.dto;

public class JarvisSpeakRequest {
    private String text;

    public JarvisSpeakRequest() {}

    public JarvisSpeakRequest(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
