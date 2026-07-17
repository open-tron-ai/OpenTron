import { apiFetch, getBase } from './api';
import type { ConnectorInfo, SyncStatus, ConnectRequest, ConnectResponse } from '../types/connectors';

// ---------------------------------------------------------------------------
// Connectors API
// ---------------------------------------------------------------------------

export async function listConnectors(): Promise<ConnectorInfo[]> {
  const res = await apiFetch(`/v1/connectors`);
  if (!res.ok) throw new Error(`Failed to list connectors: ${res.status}`);
  const data = await res.json();
  return data.connectors || [];
}

export async function getConnector(id: string): Promise<ConnectorInfo> {
  const res = await apiFetch(`/v1/connectors/${encodeURIComponent(id)}`);
  if (!res.ok) throw new Error(`Failed to get connector ${id}: ${res.status}`);
  return res.json();
}

export async function connectSource(id: string, req: ConnectRequest): Promise<ConnectResponse> {
  // Build the body the Java controller expects
  const body: Record<string, string> = {};
  if (req.clientId)                         { body.clientId = req.clientId; if (req.clientSecret) body.clientSecret = req.clientSecret; }
  else if (req.path)                         body.path     = req.path;
  else if (req.email && req.password)       { body.email    = req.email; body.password = req.password; }
  else if (req.token)                        body.token    = req.token;
  else if (req.code)                         body.code     = req.code;

  const res = await apiFetch(`/v1/connectors/${encodeURIComponent(id)}/connect`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ detail: res.statusText }));
    throw new Error(err.detail || `Failed to connect ${id}: ${res.status}`);
  }
  return res.json() as Promise<ConnectResponse>;
}

/** Open the server-side OAuth consent flow in a popup and resolve once the
 *  connector reports connected (or reject on timeout). */
export function startServerOAuth(id: string, oauthStartPath?: string): Promise<void> {
  // oauthStartPath may be a full URL (https://accounts.google.com/...) or a
  // server-relative path (/v1/connectors/.../oauth/start). Only prepend base
  // for relative paths.
  const isFullUrl = oauthStartPath && (oauthStartPath.startsWith('http://') || oauthStartPath.startsWith('https://'));
  const url = isFullUrl
    ? oauthStartPath!
    : `${getBase()}${oauthStartPath || `/v1/connectors/${encodeURIComponent(id)}/oauth/start`}`;
  window.open(url, '_blank', 'width=600,height=700');
  return new Promise((resolve, reject) => {
    const interval = setInterval(async () => {
      try {
        const info = await getConnector(id);
        if (info.connected) {
          clearInterval(interval);
          clearTimeout(timer);
          resolve();
        }
      } catch {
        // ignore transient polling errors
      }
    }, 2000);
    const timer = setTimeout(() => {
      clearInterval(interval);
      reject(new Error('Authorization timed out — please try again.'));
    }, 180000);
  });
}

export async function disconnectSource(id: string): Promise<void> {
  const res = await apiFetch(`/v1/connectors/${encodeURIComponent(id)}/disconnect`, {
    method: 'POST',
  });
  if (!res.ok) throw new Error(`Failed to disconnect ${id}: ${res.status}`);
}

export async function getSyncStatus(id: string): Promise<SyncStatus> {
  const res = await apiFetch(`/v1/connectors/${encodeURIComponent(id)}/sync`);
  if (!res.ok) throw new Error(`Failed to get sync status for ${id}: ${res.status}`);
  return res.json();
}

export async function triggerSync(id: string): Promise<{ connector_id: string; chunks_indexed: number; status: string }> {
  const res = await apiFetch(`/v1/connectors/${encodeURIComponent(id)}/sync`, {
    method: 'POST',
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ detail: res.statusText }));
    throw new Error(err.detail || `Sync failed: ${res.status}`);
  }
  return res.json();
}
