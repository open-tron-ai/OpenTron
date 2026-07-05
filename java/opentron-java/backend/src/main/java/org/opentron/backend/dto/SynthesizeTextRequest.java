package org.opentron.backend.dto;

public class SynthesizeTextRequest {
    private String text;
    private String voice;

    public SynthesizeTextRequest() {}

    public SynthesizeTextRequest(String text, String voice) {
        this.text = text;
        this.voice = voice;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }
}
