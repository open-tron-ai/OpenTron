package org.opentron.backend.dto;

import java.util.Map;

public class ChannelBindRequest {
    private String channel_type;
    private Map<String, Object> config;
    private String routing_mode;

    public ChannelBindRequest() {}

    public ChannelBindRequest(String channel_type, Map<String, Object> config, String routing_mode) {
        this.channel_type = channel_type;
        this.config = config;
        this.routing_mode = routing_mode;
    }

    public String getChannel_type() {
        return channel_type;
    }

    public void setChannel_type(String channel_type) {
        this.channel_type = channel_type;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public String getRouting_mode() {
        return routing_mode;
    }

    public void setRouting_mode(String routing_mode) {
        this.routing_mode = routing_mode;
    }
}
