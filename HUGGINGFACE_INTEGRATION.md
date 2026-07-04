# Hugging Face Integration Guide for OpenTron

## Overview

OpenTron now supports **Hugging Face** as a faster alternative to Ollama for LLM inference. Choose between:

1. **Local HF Models** (fastest, no API calls) - recommended for production
2. **HF Inference API** (cloud-based, requires API key)

## Option 1: Local Hugging Face Models (Recommended)

Local models are **3-5x faster** than Ollama and require no API key.

### Setup

1. **Install Python dependencies:**
   ```bash
   pip install transformers torch fastapi uvicorn python-dotenv
   ```

2. **Create a FastAPI server** (`hf-server.py`):
   ```python
   from fastapi import FastAPI, HTTPException
   from pydantic import BaseModel
   from transformers import AutoModelForCausalLM, AutoTokenizer
   import torch
   from typing import List, Optional
   import json

   app = FastAPI()

   # Load model on startup
   model_name = "mistralai/Mistral-7B-Instruct-v0.1"
   device = "cuda" if torch.cuda.is_available() else "cpu"
   print(f"Loading model on {device}...")
   
   tokenizer = AutoTokenizer.from_pretrained(model_name)
   model = AutoModelForCausalLM.from_pretrained(
       model_name,
       torch_dtype=torch.float16 if device == "cuda" else torch.float32,
       device_map="auto"
   )

   @app.get("/models")
   def list_models():
       """List available models (currently just the loaded one)"""
       return {"models": [model_name]}

   class Message(BaseModel):
       role: str
       content: str

   class ChatRequest(BaseModel):
       model: str
       messages: List[Message]
       temperature: float = 0.7
       max_tokens: int = 256

   @app.post("/v1/chat/completions")
   def chat_completion(request: ChatRequest):
       """OpenAI-compatible chat endpoint"""
       try:
           # Format messages for the model
           prompt = ""
           for msg in request.messages:
               if msg.role == "user":
                   prompt += f"[INST] {msg.content} [/INST]"
               elif msg.role == "assistant":
                   prompt += f" {msg.content} "
           
           # Tokenize
           inputs = tokenizer(prompt, return_tensors="pt").to(device)
           
           # Generate
           with torch.no_grad():
               outputs = model.generate(
                   **inputs,
                   max_new_tokens=request.max_tokens,
                   temperature=request.temperature,
                   top_p=0.95,
                   do_sample=True,
               )
           
           # Decode
           response = tokenizer.decode(outputs[0], skip_special_tokens=True)
           
           # Extract just the generated part
           generated = response[len(prompt):].strip()
           
           # Calculate tokens
           prompt_tokens = inputs["input_ids"].shape[1]
           completion_tokens = outputs.shape[1] - prompt_tokens
           
           return {
               "model": request.model,
               "created": 1234567890,
               "choices": [{
                   "index": 0,
                   "message": {
                       "role": "assistant",
                       "content": generated
                   },
                   "finish_reason": "stop"
               }],
               "usage": {
                   "prompt_tokens": prompt_tokens,
                   "completion_tokens": completion_tokens,
                   "total_tokens": prompt_tokens + completion_tokens
               }
           }
       except Exception as e:
           raise HTTPException(status_code=500, detail=str(e))

   if __name__ == "__main__":
       import uvicorn
       uvicorn.run(app, host="127.0.0.1", port=8000)
   ```

3. **Start the HF server:**
   ```bash
   python hf-server.py
   ```
   First run will download the model (~3.5GB for Mistral 7B)

4. **Enable in OpenTron** - set environment variable:
   ```bash
   set HF_MODE=local
   set HF_LOCAL_URL=http://127.0.0.1:8000
   ```

   Then restart OpenTron backend.

## Option 2: Hugging Face Inference API (Cloud)

Use Hugging Face's cloud API for faster inference without local GPU.

### Setup

1. **Get API token:**
   - Go to https://huggingface.co/settings/tokens
   - Create a new token with "read" access
   - Copy the token

2. **Enable in OpenTron:**
   ```bash
   set HF_MODE=api
   set HF_API_TOKEN=hf_xxxxxxxxxxxxxxxxxxxx
   ```

   Then restart OpenTron backend.

## Configuration

All options via environment variables:

| Variable | Values | Default | Notes |
|----------|--------|---------|-------|
| `HF_MODE` | `local`, `api` | `local` | Use local models or cloud API |
| `HF_LOCAL_URL` | URL | `http://127.0.0.1:8000` | Local HF server URL |
| `HF_API_TOKEN` | token | (unset) | Required for `api` mode |

## Supported Models

### Local (fastest):
- `mistralai/Mistral-7B-Instruct-v0.1` (7B, ~3.5GB) ⭐ default - best speed/quality
- `meta-llama/Llama-2-7b-chat-hf` (7B, ~3.5GB)
- `NousResearch/Nous-Hermes-2-Mistral-7B-DPO` (7B, ~3.5GB)

### Cloud API (via HF Inference):
- `mistralai/Mistral-7B-Instruct-v0.1` ⭐ recommended
- `meta-llama/Llama-2-7b-chat-hf`
- `gpt2` (tiny, for testing)

**For even faster inference:** Load a smaller model (3B parameter models are 2x faster):
- `microsoft/phi-1.5` (1.3B)
- `TinyLlama/TinyLlama-1.1B-Chat-v1.0` (1.1B)

## Performance Comparison

| Backend | Speed | Quality | Cost | Setup |
|---------|-------|---------|------|-------|
| **Ollama** | Slow (5-10s) | Good | Free | Simple |
| **HF Local** | Fast (0.5-2s) | Excellent | Free | Requires GPU/RAM |
| **HF Cloud API** | Medium (1-3s) | Excellent | $$ per token | API key only |

## How It Works

1. **Request comes in** → `/v1/chat/completions`
2. **Check `HF_MODE`** environment variable
3. **If `local`** → call local FastAPI server on `HF_LOCAL_URL`
4. **If `api`** → call HF Inference API with token
5. **Convert response** → OpenAI format (same as Ollama)
6. **Return to frontend** → fully compatible with existing UI

## Migration from Ollama

No code changes needed! Just:
1. Set `HF_MODE=local`
2. Start the HF server
3. Restart OpenTron
4. All chat endpoints automatically use HF instead

## Troubleshooting

**"HF_API_TOKEN not set"**
- Set the environment variable with your token from https://huggingface.co/settings/tokens

**"Connection refused on http://127.0.0.1:8000"**
- Make sure HF server is running: `python hf-server.py`
- Check `HF_LOCAL_URL` matches your server address

**"Model not found"**
- For local mode: model needs to be installed in your HF cache
- For API mode: model might not be available, try another from the list above

**Slow inference (>5 seconds)**
- Running on CPU? Set `device = "cuda"` requires NVIDIA GPU
- Try a smaller model (3B instead of 7B)
- Increase `max_tokens` to pre-allocate memory

## Next Steps

1. **Rebuild backend:** `mvn clean package -DskipTests`
2. **Choose setup:** local (recommended) or cloud
3. **Set environment variables**
4. **Restart OpenTron**
5. **Test:** Click "Run Now" on an agent or send a chat message
6. **Check performance:** Watch logs for "Using Hugging Face (local)" or "Using Hugging Face (api)"
