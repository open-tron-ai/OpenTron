package org.opentron.backend.agents;

import org.junit.jupiter.api.Test;
import org.opentron.backend.util.CloudModelService;
import org.opentron.backend.util.HuggingFaceService;
import org.opentron.backend.util.OllamaCliService;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AgentLLMBridgeTest {

    @Test
    void usesCloudModelServiceForClaudeModels() {
        OllamaCliService ollamaService = mock(OllamaCliService.class);
        HuggingFaceService huggingFaceService = mock(HuggingFaceService.class);
        CloudModelService cloudModelService = mock(CloudModelService.class);

        Map<String, Object> cloudResponse = Map.of(
            "choices", List.of(Map.of("message", Map.of("content", "Claude response")))
        );

        when(cloudModelService.callCloudModel(eq("claude-haiku-4-5"), anyList(), eq(Map.of("anthropic", "test-key"))))
            .thenReturn(Mono.just(cloudResponse));

        AgentLLMBridge bridge = new AgentLLMBridge(
            ollamaService,
            huggingFaceService,
            cloudModelService,
            "claude-haiku-4-5",
            Map.of("anthropic", "test-key")
        );

        Map<String, Object> result = bridge.queryLLM("system", "hello", 256);

        assertEquals("completed", result.get("status"));
        assertEquals("Claude response", result.get("response"));
        verify(ollamaService, never()).chatCompletion(anyString(), anyList());
    }
}
