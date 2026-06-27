import type { ModelInfo, SavingsData, ServerInfo } from '../types';

// Supabase config
const SUPABASE_URL = import.meta.env.VITE_SUPABASE_URL || 'https://mtbtgpwzrbostweaanpr.supabase.co';
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY || '[REDACTED]';

declare global {
  interface Window {
    __TAURI_INTERNALS__?: unknown;
  }
}

export const isTauri = () => typeof window !== 'undefined' && !!window.__TAURI_INTERNALS__;

let _tauriApiBase: string | null = null;

export async function initApiBase(): Promise<void> {
  if (!isTauri()) return;
  try {
    const { invoke } = await import('@tauri-apps/api/core');
    _tauriApiBase = await invoke<string>('get_api_base');
  } catch {
    // ignore
  }
}

const DESKTOP_API_FALLBACK = 'http://127.0.0.1:8000';

const getSettingsApiUrl = (): string => {
  try {
    const raw = localStorage.getItem('opentron-settings');
    if (raw) {
      const parsed = JSON.parse(raw);
      if (parsed.apiUrl) return parsed.apiUrl.replace(/\/+$/, '');
    }
  } catch {}
  return '';
};

export const getBase = (): string => {
  const settingsUrl = getSettingsApiUrl();
  if (settingsUrl) return settingsUrl;
  if (import.meta.env.VITE_API_URL) return import.meta.env.VITE_API_URL;
  if (isTauri()) return _tauriApiBase || DESKTOP_API_FALLBACK;
  return '';
};

export const getApiKey = (): string => {
  try {
    const raw = localStorage.getItem('opentron-settings');
    if (raw) {
      const parsed = JSON.parse(raw);
      if (parsed.apiKey) return String(parsed.apiKey);
    }
  } catch {}
  if (import.meta.env.VITE_OPENTRON_API_KEY) {
    return import.meta.env.VITE_OPENTRON_API_KEY as string;
  }
  return '';
};

export const authHeaders = (extra: Record<string, string> = {}): Record<string, string> => {
  const key = getApiKey();
  return key ? { ...extra, Authorization: `Bearer ${key}` } : { ...extra };
};

export const apiFetch = (path: string, init: RequestInit = {}): Promise<Response> => {
  const headers = authHeaders((init.headers as Record<string, string> | undefined) ?? {});
  return fetch(`${getBase()}${path}`, { ...init, headers });
};

async function tauriInvoke<T>(command: string, args: Record<string, unknown> = {}): Promise<T> {
  const { invoke } = await import('@tauri-apps/api/core');
  const apiUrl = getBase();
  return invoke<T>(command, { apiUrl, ...args });
}

// Setup status
export interface SetupStatus {
  phase: string;
  detail: string;
  ollama_ready: boolean;
  server_ready: boolean;
  model_ready: boolean;
  error: string | null;
  source?: 'ollama' | 'custom';
}

export async function getSetupStatus(): Promise<SetupStatus | null> {
  if (!isTauri()) return null;
  try {
    const { invoke } = await import('@tauri-apps/api/core');
    return await invoke<SetupStatus>('get_setup_status');
  } catch {
    return null;
  }
}

// Models
export async function fetchModels(): Promise<ModelInfo[]> {
  if (isTauri()) {
    try {
      const result = await tauriInvoke<{ data?: ModelInfo[] }>('fetch_models');
      return result?.data || [];
    } catch {
      // Fall through
    }
  }
  const res = await apiFetch(`/v1/models`);
  if (!res.ok) throw new Error(`Failed to fetch models: ${res.status}`);
  const data = await res.json();
  return data.data || [];
}

export async function fetchRecommendedModel(): Promise<{ model: string; reason: string }> {
  const res = await apiFetch(`/v1/recommended-model`);
  if (!res.ok) return { model: '', reason: 'Failed to fetch' };
  return res.json();
}

export async function pullModel(modelName: string): Promise<void> {
  if (isTauri()) {
    try {
      const { invoke } = await import('@tauri-apps/api/core');
      await invoke('pull_ollama_model', { modelName });
      return;
    } catch (e: any) {
      throw new Error(e?.message || e || 'Download failed');
    }
  }
  const res = await apiFetch(`/v1/models/pull`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ model: modelName }),
  });
  if (!res.ok) {
    const detail = await res.text().catch(() => res.statusText);
    throw new Error(`Failed to pull model: ${detail}`);
  }
}

export async function deleteModel(modelName: string): Promise<void> {
  if (isTauri()) {
    try {
      const { invoke } = await import('@tauri-apps/api/core');
      await invoke('delete_ollama_model', { modelName });
      return;
    } catch (e: any) {
      throw new Error(e?.message || e || 'Delete failed');
    }
  }
  const res = await apiFetch(`/v1/models/${encodeURIComponent(modelName)}`, {
    method: 'DELETE',
  });
  if (!res.ok) {
    const detail = await res.text().catch(() => res.statusText);
    throw new Error(`Failed to delete model: ${detail}`);
  }
}

const _CLOUD_PREFIXES = ['gpt-', 'o1-', 'o3-', 'o4-', 'claude-', 'gemini-', 'openrouter/'];

export async function preloadModel(modelName: string): Promise<void> {
  if (_CLOUD_PREFIXES.some(p => modelName.startsWith(p))) {
    return;
  }
  const ollamaUrl = 'http://127.0.0.1:11434';
  try {
    const res = await fetch(`${ollamaUrl}/api/generate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ model: modelName, prompt: '', keep_alive: '5m' }),
      signal: AbortSignal.timeout(120_000),
    });
    if (!res.ok) throw new Error(`Preload failed: ${res.status}`);
  } catch (e: any) {
    if (e.name === 'TimeoutError') throw new Error('Model load timed out (120s)');
    throw e;
  }
}

// Server info
export async function fetchSavings(): Promise<SavingsData> {
  const res = await apiFetch(`/v1/savings`);
  if (!res.ok) throw new Error(`Failed to fetch savings: ${res.status}`);
  return res.json();
}

export async function fetchServerInfo(): Promise<ServerInfo> {
  const res = await apiFetch(`/v1/info`);
  if (!res.ok) throw new Error(`Failed to fetch server info: ${res.status}`);
  return res.json();
}

export async function checkHealth(): Promise<boolean> {
  if (isTauri()) {
    try {
      await tauriInvoke('check_health', { apiUrl: getBase() });
      return true;
    } catch {
      return false;
    }
  }
  const probe = async (url: string): Promise<boolean> => {
    try {
      const res = await fetch(url, { cache: 'no-store' });
      return res.ok;
    } catch {
      return false;
    }
  };
  if (await probe('/health')) return true;
  return probe('/v1/connectors');
}

export async function fetchEnergy(): Promise<unknown> {
  if (isTauri()) {
    try {
      return await tauriInvoke('fetch_energy', { apiUrl: getBase() });
    } catch {}
  }
  const res = await apiFetch(`/v1/telemetry/energy`);
  if (!res.ok) throw new Error(`Failed: ${res.status}`);
  return res.json();
}

export async function fetchTelemetry(): Promise<unknown> {
  if (isTauri()) {
    try {
      return await tauriInvoke('fetch_telemetry', { apiUrl: getBase() });
    } catch {}
  }
  const res = await apiFetch(`/v1/telemetry/stats`);
  if (!res.ok) throw new Error(`Failed: ${res.status}`);
  return res.json();
}

export async function fetchTraces(limit: number = 50): Promise<unknown> {
  if (isTauri()) {
    try {
      return await tauriInvoke('fetch_traces', { apiUrl: getBase(), limit });
    } catch {}
  }
  const res = await apiFetch(`/v1/traces?limit=${limit}`);
  if (!res.ok) throw new Error(`Failed: ${res.status}`);
  return res.json();
}

// Speech interfaces
export interface TranscriptionResult {
  text: string;
  language: string | null;
  confidence: number | null;
  duration_seconds: number;
}

export interface SpeechHealth {
  available?: boolean;
  backend?: string;
  voice?: string;
  reason?: string;
  status?: string;
}

export async function transcribeAudio(audioBlob: Blob, filename = 'recording.webm'): Promise<TranscriptionResult> {
  if (isTauri()) {
    try {
      const buffer = await audioBlob.arrayBuffer();
      return await tauriInvoke<TranscriptionResult>('transcribe_audio', {
        audioData: Array.from(new Uint8Array(buffer)),
        filename,
      });
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      throw new Error(msg || 'Transcription failed');
    }
  }
  const formData = new FormData();
  formData.append('file', audioBlob, filename);
  const res = await apiFetch(`/v1/speech/transcribe`, {
    method: 'POST',
    body: formData,
  });
  if (!res.ok) {
    let detail = "";
    try {
      const body = await res.json();
      detail = typeof body.detail === 'string' ? body.detail : "";
    } catch {
      //
    }
    throw new Error(detail || `Transcription failed: ${res.status}`);
  }
  return res.json();
}

export async function fetchSpeechHealth(): Promise<SpeechHealth> {
  if (isTauri()) {
    try {
      return await tauriInvoke<SpeechHealth>('speech_health');
    } catch {
      return { available: false };
    }
  }
  const res = await apiFetch(`/v1/jarvis/health`);
  if (!res.ok) return { available: false };
  return res.json();
}

export interface JarvisSynthesisResult {
  status: string;
  audio_base64?: string;
  voice_id?: string;
  audio_format?: string;
  text_length?: number;
  duration_estimate_ms?: number;
  error_message?: string;
}

export async function synthesizeWithJarvis(text: string): Promise<JarvisSynthesisResult> {
  if (!text || !text.trim()) {
    throw new Error('Text cannot be empty');
  }
  
  const res = await apiFetch(`/v1/jarvis/speak`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text }),
  });
  
  if (!res.ok) {
    let detail = '';
    try {
      const body = await res.json();
      detail = typeof body.error_message === 'string' ? body.error_message : '';
    } catch {}
    throw new Error(detail || `Jarvis TTS failed: ${res.status}`);
  }
  
  return res.json();
}

// Agent/Memory types and functions (minimal stubs to prevent import errors)
export interface ManagedAgent {
  id: string;
  name: string;
  [key: string]: any;
}

export interface AgentTask {
  id: string;
  [key: string]: any;
}

export interface ChannelBinding {
  id: string;
  [key: string]: any;
}

export interface AgentTemplate {
  id: string;
  [key: string]: any;
}

export interface LearningLogEntry {
  id: string;
  [key: string]: any;
}

export interface AgentTrace {
  id: string;
  [key: string]: any;
}

export interface AgentTraceDetail {
  id: string;
  [key: string]: any;
}

export interface ToolInfo {
  name: string;
  [key: string]: any;
}

export interface PendingApproval {
  id: string;
  [key: string]: any;
}

export interface AgentMessage {
  id: string;
  [key: string]: any;
}

export interface MemoryStats {
  entries: number;
  backend: string;
  [key: string]: any;
}

export interface MemorySearchResult {
  content: string;
  [key: string]: any;
}

export interface InferenceSource {
  kind: 'ollama' | 'custom';
  [key: string]: any;
}

// Placeholder functions for agents/memory to prevent import errors
export async function fetchManagedAgents(): Promise<ManagedAgent[]> { return []; }
export async function fetchManagedAgent(id: string): Promise<ManagedAgent> { throw new Error('Not implemented'); }
export async function createManagedAgent(body: any): Promise<ManagedAgent> { throw new Error('Not implemented'); }
export async function updateManagedAgent(id: string, body: any): Promise<ManagedAgent> { throw new Error('Not implemented'); }
export async function deleteManagedAgent(id: string): Promise<void> { }
export async function pauseManagedAgent(id: string): Promise<void> { }
export async function resumeManagedAgent(id: string): Promise<void> { }
export async function runManagedAgent(id: string): Promise<void> { }
export async function recoverManagedAgent(id: string): Promise<any> { throw new Error('Not implemented'); }
export async function fetchAgentState(id: string): Promise<any> { throw new Error('Not implemented'); }

export async function fetchAgentTasks(id: string): Promise<AgentTask[]> { return []; }
export async function createAgentTask(id: string, desc: string): Promise<AgentTask> { throw new Error('Not implemented'); }

export async function fetchAgentChannels(id: string): Promise<ChannelBinding[]> { return []; }
export async function bindAgentChannel(id: string, type: string, config?: any): Promise<ChannelBinding> { throw new Error('Not implemented'); }
export async function unbindAgentChannel(id: string, binding: string): Promise<void> { }

export async function fetchTemplates(): Promise<AgentTemplate[]> { return []; }
export async function askAgent(id: string, content: string): Promise<AgentMessage> { throw new Error('Not implemented'); }
export async function fetchAgentMessages(id: string): Promise<AgentMessage[]> { return []; }
export async function sendAgentMessage(id: string, content: string, mode?: any, callbacks?: any): Promise<AgentMessage> { throw new Error('Not implemented'); }

export async function fetchErrorAgents(): Promise<ManagedAgent[]> { return []; }

export async function fetchLearningLog(id: string): Promise<LearningLogEntry[]> { return []; }
export async function triggerLearning(id: string): Promise<void> { }
export async function fetchAgentTraces(id: string, limit?: number): Promise<AgentTrace[]> { return []; }
export async function fetchAgentTrace(id: string, traceId: string): Promise<AgentTraceDetail> { throw new Error('Not implemented'); }

export async function fetchAvailableTools(): Promise<ToolInfo[]> { return []; }
export async function saveToolCredentials(tool: string, creds: any): Promise<void> { }

export async function fetchPendingApprovals(): Promise<PendingApproval[]> { return []; }
export async function approveAction(id: string): Promise<void> { }
export async function denyAction(id: string): Promise<void> { }

export async function getMemoryStats(): Promise<MemoryStats> { throw new Error('Not implemented'); }
export async function searchMemory(query: string, topK?: number): Promise<MemorySearchResult[]> { return []; }
export async function storeMemory(content: string, metadata?: any): Promise<void> { }
export async function indexMemoryPath(path: string): Promise<any> { throw new Error('Not implemented'); }
export async function getMemoryConfig(): Promise<any> { throw new Error('Not implemented'); }

export async function getInferenceSource(): Promise<InferenceSource> { throw new Error('Not implemented'); }
export async function setInferenceSource(src: any): Promise<void> { }

export async function submitSavings(data: any): Promise<boolean> {
  if (!SUPABASE_URL || !SUPABASE_ANON_KEY) return false;
  try {
    const res = await fetch(
      `${SUPABASE_URL}/rest/v1/savings_entries?on_conflict=anon_id`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          apikey: SUPABASE_ANON_KEY,
          Authorization: `Bearer ${SUPABASE_ANON_KEY}`,
          Prefer: 'resolution=merge-duplicates',
        },
        body: JSON.stringify(data),
      },
    );
    return res.ok || res.status === 201 || res.status === 200;
  } catch {
    return false;
  }
}

// Channel setup functions
export async function sendblueVerify(id: string, secret: string): Promise<any> { throw new Error('Not implemented'); }
export async function sendblueRegisterWebhook(id: string, secret: string, url: string): Promise<any> { throw new Error('Not implemented'); }
export async function sendblueTest(id: string, secret: string, from: string, to: string): Promise<any> { throw new Error('Not implemented'); }
export async function sendblueHealth(): Promise<any> { throw new Error('Not implemented'); }
