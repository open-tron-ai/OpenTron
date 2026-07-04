import { useEffect, useState, useCallback } from 'react';
import {
  getStorageStats,
  getStorageTraces,
  getMemoryStatsDetailed,
  storeAgentMemory,
  searchAgentMemory,
  getAgentMemory,
  getAgentTraces,
  getAgentStatuses,
} from '../lib/api';

export interface StorageStats {
  total_memories: number;
  total_traces: number;
  backend: string;
  timestamp: number;
  error?: string;
}

export interface TraceLog {
  id: number;
  agent: string;
  duration_ms: number;
  timestamp: string;
  is_compressed: boolean;
}

export interface Memory {
  id: number;
  agent: string;
  summary: string;
  timestamp: string;
  is_archived: boolean;
}

export interface MemorySearchResult {
  content: string;
  score: number;
  metadata: Record<string, any>;
}

export function useStorageStats() {
  const [stats, setStats] = useState<StorageStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getStorageStats();
      setStats(data);
      setError(null);
    } catch (err: any) {
      setError(err.message || 'Failed to fetch storage stats');
      console.error('[useStorageStats] Error:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
    const interval = setInterval(refresh, 30000); // Refresh every 30 seconds
    return () => clearInterval(interval);
  }, [refresh]);

  return { stats, loading, error, refresh };
}

export function useAgentTraces(agentId: string, limit: number = 50) {
  const [traces, setTraces] = useState<TraceLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetch = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getStorageTraces(agentId, limit);
      setTraces(data.traces || []);
      setError(null);
    } catch (err: any) {
      setError(err.message || 'Failed to fetch traces');
      console.error('[useAgentTraces] Error:', err);
    } finally {
      setLoading(false);
    }
  }, [agentId, limit]);

  useEffect(() => {
    fetch();
  }, [fetch]);

  return { traces, loading, error, refetch: fetch };
}

export function useAgentMemory(agentId: string, limit: number = 50) {
  const [memories, setMemories] = useState<Memory[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetch = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getAgentMemory(agentId, limit);
      setMemories(data.memories || []);
      setError(null);
    } catch (err: any) {
      setError(err.message || 'Failed to fetch memories');
      console.error('[useAgentMemory] Error:', err);
    } finally {
      setLoading(false);
    }
  }, [agentId, limit]);

  useEffect(() => {
    fetch();
  }, [fetch]);

  return { memories, loading, error, refetch: fetch };
}

export function useMemorySearch(query: string, agentName?: string, topK: number = 5) {
  const [results, setResults] = useState<MemorySearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const search = useCallback(async () => {
    if (!query.trim()) {
      setResults([]);
      return;
    }

    setLoading(true);
    try {
      const data = await searchAgentMemory(query, agentName, topK);
      setResults(data.results || []);
      setError(null);
    } catch (err: any) {
      setError(err.message || 'Failed to search memory');
      console.error('[useMemorySearch] Error:', err);
    } finally {
      setLoading(false);
    }
  }, [query, agentName, topK]);

  useEffect(() => {
    const debounce = setTimeout(() => {
      search();
    }, 500);

    return () => clearTimeout(debounce);
  }, [search]);

  return { results, loading, error, search };
}

export function useStoreMemory() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const store = useCallback(async (agentName: string, content: string, summary?: string) => {
    setLoading(true);
    try {
      const result = await storeAgentMemory(agentName, content, summary);
      setError(null);
      return result;
    } catch (err: any) {
      const errorMsg = err.message || 'Failed to store memory';
      setError(errorMsg);
      console.error('[useStoreMemory] Error:', err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  return { store, loading, error };
}

// Generic useStorage hook for making requests
export function useStorage() {
  const makeRequest = useCallback(async (path: string) => {
    try {
      const response = await fetch(`http://localhost:8000${path}`);
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      return response.json();
    } catch (error) {
      console.error('[useStorage] Request failed:', error);
      throw error;
    }
  }, []);

  return { makeRequest };
}
