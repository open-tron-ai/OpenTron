package org.opentron.backend.services;

import org.junit.jupiter.api.Test;
import org.opentron.backend.util.CloudModelService;
import org.opentron.backend.util.OllamaCliService;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModelSelectorServiceTest {

    @Test
    void selectsAnthropicCloudModelWhenAnthropicApiKeyConfigured() {
        OllamaCliService ollamaService = mock(OllamaCliService.class);
        CloudModelService cloudModelService = mock(CloudModelService.class);

        when(ollamaService.listModels()).thenReturn(Mono.just(List.of()));
        when(cloudModelService.listModels()).thenReturn(Mono.just(List.of()));
        when(cloudModelService.listModels(anyMap())).thenReturn(Mono.just(List.of()));
        when(cloudModelService.hasApiKey(eq("openrouter"), anyMap())).thenReturn(false);
        when(cloudModelService.hasApiKey(eq("google"), anyMap())).thenReturn(false);
        when(cloudModelService.hasApiKey(eq("anthropic"), anyMap())).thenReturn(true);

        ModelSelectorService service = new ModelSelectorService(ollamaService, cloudModelService);

        assertEquals("claude-haiku-4-5", service.selectBestModel("backend", java.util.Map.of("anthropic", "test-key")));
    }
}
