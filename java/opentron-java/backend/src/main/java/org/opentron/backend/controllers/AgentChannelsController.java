package org.opentron.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/v1/managed-agents")
public class AgentChannelsController {

    @GetMapping("/{agentId}/channels")
    public ResponseEntity<Map<String, Object>> getAgentChannels(@PathVariable String agentId) {
        List<Map<String, Object>> bindings = new ArrayList<>();
        
        // Sample Slack binding
        Map<String, Object> slack = new HashMap<>();
        slack.put("id", "ch-slack-1");
        slack.put("agent_id", agentId);
        slack.put("channel_type", "slack");
        Map<String, Object> slackConfig = new HashMap<>();
        slackConfig.put("bot_token", "xoxb-***");
        slackConfig.put("app_token", "xapp-***");
        slack.put("config", slackConfig);
        slack.put("session_id", "session-1");
        slack.put("routing_mode", "dedicated");
        bindings.add(slack);
        
        return ResponseEntity.ok(Map.of("bindings", bindings));
    }

    @PostMapping("/{agentId}/channels")
    public ResponseEntity<Map<String, Object>> bindChannel(
            @PathVariable String agentId,
            @RequestBody org.opentron.backend.dto.ChannelBindRequest payload) {
        String channelType = payload.getChannel_type();
        
        Map<String, Object> binding = new HashMap<>();
        binding.put("id", "ch-" + channelType + "-" + System.currentTimeMillis());
        binding.put("agent_id", agentId);
        binding.put("channel_type", channelType);
        binding.put("config", payload.getConfig() == null ? new HashMap<>() : payload.getConfig());
        binding.put("session_id", "session-" + UUID.randomUUID());
        binding.put("routing_mode", payload.getRouting_mode() == null ? "dedicated" : payload.getRouting_mode());
        
        return ResponseEntity.ok(binding);
    }

    @PostMapping("/{agentId}/channels/bind")
    public ResponseEntity<Map<String, Object>> bindChannelAlias(
            @PathVariable String agentId,
            @RequestBody org.opentron.backend.dto.ChannelBindRequest payload) {
        return bindChannel(agentId, payload);
    }

    @DeleteMapping("/{agentId}/channels/{bindingId}")
    public ResponseEntity<Map<String, String>> unbindChannel(
            @PathVariable String agentId,
            @PathVariable String bindingId) {
        return ResponseEntity.ok(Map.of(
            "status", "deleted",
            "binding_id", bindingId
        ));
    }
}
