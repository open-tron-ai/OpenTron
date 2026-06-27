# OpenTron Java Backend Deployment

## Quick Start with Docker Compose

### Prerequisites
- Docker and Docker Compose installed
- Environment variables configured (see below)

### Setup

1. Copy the environment template:
```bash
cp .env.example .env
```

2. Update `.env` with your settings:
```bash
# Generate a secure API key
openssl rand -hex 32 > api_key.txt
cat api_key.txt

# Update .env
ENGINE_APIKEY=$(cat api_key.txt)
# Or manually set it in .env
```

3. Start the services:
```bash
docker compose up -d
```

The backend will be available at `http://localhost:8000`.

### Stopping Services
```bash
docker compose down
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `ENGINE_HOST` | `http://ollama:11434` | Engine backend URL (Ollama or compatible) |
| `ENGINE_APIKEY` | (required) | API key for client authentication |

### Services

- **opentron-backend**: Java Spring Boot backend (port 8000)
  - API endpoints: `/v1/models`, `/v1/chat/completions`, `/v1/chat/stream`
  - Proxies requests to Ollama engine with streaming support
  - WebSocket support for `/v1/chat/stream`

- **ollama**: Local Ollama inference engine (port 11434)
  - Used as the AI model provider
  - Models stored in named volume `ollama-models`

## API Usage

### List Available Models
```bash
curl -H "Authorization: Bearer ${API_KEY}" \
  http://localhost:8000/v1/models
```

### Stream Chat Completions (HTTP)
```bash
curl -H "Authorization: Bearer ${API_KEY}" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama2",
    "messages": [{"role": "user", "content": "Hello"}],
    "stream": true
  }' \
  http://localhost:8000/v1/chat/completions
```

### Stream Chat via WebSocket
```javascript
const ws = new WebSocket('ws://localhost:8000/v1/chat/stream');
ws.send(JSON.stringify({
  model: 'llama2',
  messages: [{ role: 'user', content: 'Hello' }],
  stream: true
}));
ws.onmessage = (event) => {
  console.log('Chunk:', event.data);
};
```

## Troubleshooting

### Backend fails to start: "API key is required"
- Ensure `ENGINE_APIKEY` is set in `.env`
- Run: `docker compose logs opentron-backend`

### Cannot connect to Ollama
- Check Ollama is running: `docker compose logs ollama`
- Verify models are pulled: `curl http://localhost:11434/api/tags`
- Common issue: Ollama container not fully started; check health status

### High memory usage
- Ollama requires significant memory for model inference
- Ensure Docker has sufficient allocated resources
- Consider using smaller models (e.g., `orca-mini` instead of `llama2`)

## Building Custom Docker Image

To build locally:
```bash
cd ../..  # Go to OpenTron root
docker build -f deploy/docker/Dockerfile -t opentron-backend:latest .
```

### Build Stages
1. **frontend**: Node.js build of Vue SPA
2. **java-builder**: Maven build of Spring Boot backend
3. **runtime**: Alpine JRE with compiled JAR

## GPU Support

For GPU-accelerated inference with Ollama:

### NVIDIA GPUs
```bash
docker compose -f docker-compose.gpu.nvidia.yml up -d
```

### AMD ROCm GPUs
```bash
docker compose -f docker-compose.gpu.rocm.yml up -d
```

Note: GPU support requires appropriate Docker runtime and drivers installed.

## Health Checks

The stack includes health checks:
- Ollama endpoint: `/api/tags` endpoint every 5s
- Backend: Port 8000 must be open and responsive

Check status:
```bash
docker compose ps
```

## Deployment Notes

- API key is **required** when binding to `0.0.0.0` (all interfaces)
- Clients must authenticate with: `Authorization: Bearer {api_key}`
- For production: Consider using reverse proxy (nginx) with TLS
- Backend supports end-to-end streaming without buffering for SSE and WebSocket responses
- Connection pool: 200 max connections, 60s timeout
- Backend forwards all headers except Content-Length (recalculated by framework)
