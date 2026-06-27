package org.opentron.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/v1/channels")
public class ChannelsController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> listChannels() {
        List<Map<String, Object>> channels = new ArrayList<>();
        
        Map<String, Object> slack = new HashMap<>();
        slack.put("id", "slack-1");
        slack.put("channel_type", "slack");
        slack.put("display_name", "Slack Bot");
        slack.put("connected", true);
        slack.put("config", Map.of(
            "bot_token", "xoxb-***",
            "app_token", "xapp-***"
        ));
        channels.add(slack);
        
        Map<String, Object> whatsapp = new HashMap<>();
        whatsapp.put("id", "whatsapp-1");
        whatsapp.put("channel_type", "whatsapp");
        whatsapp.put("display_name", "WhatsApp Business");
        whatsapp.put("connected", false);
        whatsapp.put("config", Map.of(
            "phone_number", "+1234567890"
        ));
        channels.add(whatsapp);
        
        Map<String, Object> sendblue = new HashMap<>();
        sendblue.put("id", "sendblue-1");
        sendblue.put("channel_type", "sendblue");
        sendblue.put("display_name", "SendBlue SMS");
        sendblue.put("connected", false);
        sendblue.put("config", Map.of(
            "api_key_id", "***"
        ));
        channels.add(sendblue);
        
        return ResponseEntity.ok(Map.of("channels", channels));
    }

    @PostMapping("/{channelId}/test")
    public ResponseEntity<Map<String, Object>> testChannel(@PathVariable String channelId) {
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Test message sent"
        ));
    }

    @PostMapping("/{channelId}/disconnect")
    public ResponseEntity<Map<String, Object>> disconnectChannel(@PathVariable String channelId) {
        return ResponseEntity.ok(Map.of(
            "status", "disconnected"
        ));
    }
}
