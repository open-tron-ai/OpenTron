package org.opentron.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * DTO for chat completion requests.
 * Represents the standard OpenAI-compatible chat request format.
 */
public class ChatCompletionRequest {
    @JsonProperty("model")
    private String model;

    @JsonProperty("messages")
    private List<Message> messages;

    @JsonProperty("stream")
    private Boolean stream;

    @JsonProperty("temperature")
    private Double temperature;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    @JsonProperty("top_p")
    private Double topP;

    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;

    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    @JsonProperty("stop")
    private List<String> stop;
    
    @JsonProperty("params")
    private java.util.Map<String, Object> params;

    // Getters and Setters
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public Boolean getStream() {
        return stream != null ? stream : false;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }

    public Double getTemperature() {
        return temperature != null ? temperature : 0.7;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Double getTopP() {
        return topP != null ? topP : 1.0;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public Double getFrequencyPenalty() {
        return frequencyPenalty != null ? frequencyPenalty : 0.0;
    }

    public void setFrequencyPenalty(Double frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
    }

    public Double getPresencePenalty() {
        return presencePenalty != null ? presencePenalty : 0.0;
    }

    public void setPresencePenalty(Double presencePenalty) {
        this.presencePenalty = presencePenalty;
    }

    public List<String> getStop() {
        return stop;
    }

    public void setStop(List<String> stop) {
        this.stop = stop;
    }

    public java.util.Map<String, Object> getParams() {
        return params;
    }

    public void setParams(java.util.Map<String, Object> params) {
        this.params = params;
    }

    /**
     * Inner class representing a message in the chat.
     */
    public static class Message {
        @JsonProperty("role")
        private String role;

        @JsonProperty("content")
        private String content;

        @JsonProperty("name")
        private String name;

        // Getters and Setters
        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
