package org.opentron.backend.dto;

public class ScreenshotAnalyzeRequest {
    private String image_base64;
    private String prompt;
    private String context;

    public ScreenshotAnalyzeRequest() {}

    public ScreenshotAnalyzeRequest(String image_base64, String prompt, String context) {
        this.image_base64 = image_base64;
        this.prompt = prompt;
        this.context = context;
    }

    public String getImage_base64() {
        return image_base64;
    }

    public void setImage_base64(String image_base64) {
        this.image_base64 = image_base64;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
