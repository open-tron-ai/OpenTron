Critical

SSE proxy: Forward raw response buffers end-to-end (no String buffering) so POST /v1/chat/completions?stream=true preserves SSE framing and chunking — files: ForwardingController.java, ChatController.java.
WS bidirectional relay: Implement raw frame-level proxy for /v1/chat/stream (relay engine ↔ client frames, propagate closes/backpressure) — files: ReactiveChatWebSocketHandler.java, WebSocketConfig.java.
Headers & status: Preserve required request/response headers (Host, Authorization, Connection, Transfer-Encoding) and status codes when proxying.
Streaming response bodies: Use Flux<DataBuffer>/bodyToFlux(DataBuffer.class) everywhere for streaming paths.
Important

Backpressure / cancellation: Ensure cancellations travel both ways so connections don't hang.
WebClient config: Verify Reactor Netty connector, timeouts, and pooling are suitable for streaming; surface engine errors to callers.
OpenAI parity: Confirm /v1/models, /health, and any extended endpoints match shapes expected by frontend/clients.
CORS & auth: Confirm allowed origins and how API keys are accepted/passed.
Testing & infra

Fix integration tests: Get EngineStubIntegrationTest passing (SSE + WS + models).
Add e2e smoke tests: Validate frontend flows (dev and desktop/tauri).
Deployment / docs: Docker/service parity, README, env vars (engine.host, engine.apiKey).
Nice-to-have

Logging & metrics: Wiretap/diagnostic logs (you added some) and metrics for connection counts/errors.
Retries / graceful failures: Sensible transient retry policy for non-streaming endpoints.