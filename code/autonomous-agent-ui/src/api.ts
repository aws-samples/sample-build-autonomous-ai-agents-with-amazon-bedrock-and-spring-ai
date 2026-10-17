// Typed client for the autonomous agent backend. Types mirror the Java DTOs.

const API_BASE = import.meta.env.VITE_API_BASE ?? '';

/** Landing page that links both sample agents (code/landing-page). Set at build time. */
export const LANDING_URL: string = import.meta.env.VITE_LANDING_URL || 'http://localhost:3000/';

export interface AgentInfo {
  name: string;
  description: string;
  capabilities: string[];
  tools: string[];
  model: string;
}

export async function fetchAgentInfo(): Promise<AgentInfo> {
  const res = await fetch(`${API_BASE}/api/agent/info`);
  if (!res.ok) {
    throw new Error(`Failed to load agent info (${res.status})`);
  }
  return res.json();
}

export interface ChatParams {
  prompt: string;
  sessionId: string;
  signal?: AbortSignal;
  onToken: (chunk: string) => void;
}

/**
 * Calls the streaming /api/chat endpoint and invokes onToken for each chunk as it
 * arrives, giving a live token-by-token effect in the UI.
 */
export async function streamChat({ prompt, sessionId, signal, onToken }: ChatParams): Promise<void> {
  const res = await fetch(`${API_BASE}/api/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ prompt, sessionId }),
    signal,
  });

  if (!res.ok || !res.body) {
    const text = await res.text().catch(() => '');
    throw new Error(`Chat failed (${res.status}). ${text}`);
  }

  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  for (;;) {
    const { value, done } = await reader.read();
    if (done) break;
    onToken(decoder.decode(value, { stream: true }));
  }
}
