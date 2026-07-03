import type { ResearchEvent, SSEEvent } from '../types';
import { getBase, authHeaders } from './api';

export interface ChatRequest {
  model: string;
  messages: Array<{ role: string; content: string }>;
  stream?: boolean;
  temperature?: number;
  max_tokens?: number;
}

/**
 * Collect cloud provider API keys from localStorage
 */
function getCloudApiKeys(): Record<string, string> {
  const keys: Record<string, string> = {};
  
  try {
    const openaiKey = localStorage.getItem('OpenTron-openai-key');
    if (openaiKey) keys.openai = openaiKey;
    
    const anthropicKey = localStorage.getItem('OpenTron-anthropic-key');
    if (anthropicKey) keys.anthropic = anthropicKey;
    
    const geminiKey = localStorage.getItem('OpenTron-gemini-key');
    if (geminiKey) keys.google = geminiKey;
    
    const openrouterKey = localStorage.getItem('OpenTron-openrouter-key');
    if (openrouterKey) keys.openrouter = openrouterKey;
  } catch (e) {
    console.error('[getCloudApiKeys] Error reading API keys from localStorage:', e);
  }
  
  return keys;
}

/**
 * Simple non-streaming chat completion.
 * Returns the full response at once (no SSE parsing).
 */
export async function chatCompletionSimple(
  request: ChatRequest,
  signal?: AbortSignal,
): Promise<any> {
  const base = getBase();
  console.log('[chatCompletionSimple] Sending request to:', `${base}/v1/chat/completions`);
  console.log('[chatCompletionSimple] Request:', request);

  const apiKeys = getCloudApiKeys();
  const headers = authHeaders({ 'Content-Type': 'application/json' });
  
  if (Object.keys(apiKeys).length > 0) {
    headers['X-API-Keys'] = JSON.stringify(apiKeys);
    console.log('[chatCompletionSimple] Sending API keys for:', Object.keys(apiKeys).join(', '));
  }

  const response = await fetch(`${base}/v1/chat/completions`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ ...request, stream: false }),
    signal,
  });

  console.log('[chatCompletionSimple] Response status:', response.status);

  if (!response.ok) {
    const errText = await response.text();
    console.error('[chatCompletionSimple] Error response:', errText);
    throw new Error(`Chat request failed: ${response.status} - ${errText}`);
  }

  const data = await response.json();
  console.log('[chatCompletionSimple] Got response:', data);
  return data;
}

export async function* streamChat(
  request: ChatRequest,
  signal?: AbortSignal,
): AsyncGenerator<SSEEvent> {
  const base = getBase();
  console.log('[streamChat] Connecting to:', `${base}/v1/chat/completions`);
  console.log('[streamChat] Request:', request);
  
  const apiKeys = getCloudApiKeys();
  const headers = authHeaders({ 'Content-Type': 'application/json' });
  
  if (Object.keys(apiKeys).length > 0) {
    headers['X-API-Keys'] = JSON.stringify(apiKeys);
    console.log('[streamChat] Sending API keys for:', Object.keys(apiKeys).join(', '));
  }
  
  const response = await fetch(`${base}/v1/chat/completions`, {
    method: 'POST',
    headers,
    body: JSON.stringify(request),
    signal,
  });

  console.log('[streamChat] Response status:', response.status);
  console.log('[streamChat] Response content-type:', response.headers.get('content-type'));
  
  if (!response.ok) {
    const errText = await response.text();
    console.error('[streamChat] Error response:', errText);
    throw new Error(`Chat request failed: ${response.status} - ${errText}`);
  }

  if (!response.body) {
    throw new Error('Response has no body');
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) {
        console.log('[streamChat] Stream ended');
        break;
      }

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';

      let currentEvent: string | undefined;

      for (const line of lines) {
        console.log('[streamChat] Line:', line.substring(0, 100));
        
        if (line.startsWith('event: ')) {
          currentEvent = line.slice(7).trim();
          console.log('[streamChat] Event:', currentEvent);
        } else if (line.startsWith('data: ')) {
          const data = line.slice(6);
          console.log('[streamChat] Data:', data.substring(0, 100));
          
          if (data === '[DONE]') {
            console.log('[streamChat] Got [DONE]');
            return;
          }
          
          yield { event: currentEvent, data };
          currentEvent = undefined;
        } else if (line.trim() === '') {
          currentEvent = undefined;
        }
      }
    }
  } finally {
    reader.releaseLock();
  }
}

export async function* streamResearch(
  query: string,
  signal?: AbortSignal,
): AsyncGenerator<ResearchEvent> {
  // /api/research is mounted at the server root — strip any trailing /v1
  // from the base so configurations like "http://host:8000/v1" still resolve.
  const base = getBase().replace(/\/v1\/?$/, '');
  const response = await fetch(`${base}/api/research`, {
    method: 'POST',
    headers: authHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify({ query }),
    signal,
  });

  if (!response.ok) {
    throw new Error(`Research request failed: ${response.status}`);
  }

  const reader = response.body!.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';

      for (const line of lines) {
        if (!line.startsWith('data: ')) continue;
        const data = line.slice(6);
        if (data === '[DONE]') return;
        try {
          const parsed = JSON.parse(data) as ResearchEvent;
          yield parsed;
          if (parsed.type === 'done') return;
        } catch {
          // skip malformed chunks
        }
      }
    }
  } finally {
    reader.releaseLock();
  }
}
