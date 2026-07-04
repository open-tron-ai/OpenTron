export interface CoordinatorResponseParsed {
  tronResponse: string;
  agentsUsed: string[];
  elapsedMs: number;
}

export function parseCoordinatorResponse(response: any): CoordinatorResponseParsed {
  // Collect every object reachable by unwrapping .result repeatedly
  const candidates: any[] = [];
  let cur = response;
  for (let i = 0; i < 6; i++) {
    if (!cur || typeof cur !== 'object') break;
    candidates.push(cur);
    cur = cur.result;
  }

  let tronResponse = '';
  let agentsUsed: string[] = [];
  let elapsedMs = 0;

  for (const c of candidates) {
    if (!c || typeof c !== 'object') continue;
    const tr = c['tron_response'] ?? c['tronResponse'] ?? c['message'];
    if (!tronResponse && tr && typeof tr === 'string' && tr !== 'Processing complete.') {
      tronResponse = tr;
    }
    if (!agentsUsed.length && Array.isArray(c['agents_used']) && c['agents_used'].length > 0) {
      agentsUsed = c['agents_used'];
    }
    if (!elapsedMs && c['elapsed_ms']) {
      elapsedMs = Number(c['elapsed_ms']);
    }
  }

  return {
    tronResponse: tronResponse || 'Processing complete.',
    agentsUsed,
    elapsedMs,
  };
}
