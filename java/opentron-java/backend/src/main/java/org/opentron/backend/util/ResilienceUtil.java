package org.opentron.backend.util;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Resilience utilities for HTTP operations.
 * Provides retry and timeout configuration for engine requests.
 */
public class ResilienceUtil {

    /**
     * Apply retry logic with exponential backoff to a Mono operation.
     * Used for non-streaming endpoints (GET /v1/models, etc.)
     * 
     * Max retries: 3
     * Initial delay: 100ms
     * Max delay: 1000ms
     * Multiplier: 2
     */
    public static <T> Mono<T> withRetry(Mono<T> mono) {
        return mono.retryWhen(
            Retry.backoff(3, Duration.ofMillis(100))
                .maxBackoff(Duration.ofSeconds(1))
                .jitter(0.1)
        );
    }

    /**
     * Apply timeout to a Mono operation.
     * Used for streaming and non-streaming endpoints.
     * 
     * Timeout: 60 seconds (increased for Ollama CLI inference which takes ~15s)
     */
    public static <T> Mono<T> withTimeout(Mono<T> mono) {
        return mono.timeout(Duration.ofSeconds(60));
    }

    /**
     * Apply both retry and timeout to a Mono operation.
     * Use for non-streaming operations where retries make sense.
     */
    public static <T> Mono<T> withResilienceNonStreaming(Mono<T> mono) {
        return withTimeout(withRetry(mono));
    }

    /**
     * Apply only timeout to a Mono operation.
     * Use for streaming operations where retries don't make sense.
     */
    public static <T> Mono<T> withResilienceStreaming(Mono<T> mono) {
        return withTimeout(mono);
    }
}
