// Typed client for the IDP backend. Types mirror the Java DTOs.

const API_BASE = import.meta.env.VITE_API_BASE ?? '';

export type Severity = 'INFO' | 'WARN' | 'ALERT' | 'BLOCK';

export interface ExtractedField {
  name: string;
  value: string;
  confidence: number;
}

export interface ProcessedDocument {
  documentType: string;
  summary: string;
  answer: string;
  fields: ExtractedField[];
  confidence: number;
  validationIssues: string[];
}

export interface RuleResult {
  ruleId: string;
  severity: Severity;
  message: string;
}

export interface DocumentResult {
  agentType: string;
  extraction: ProcessedDocument;
  alerts: RuleResult[];
  blocked: boolean;
  /** True when the document was routed to the human review queue. */
  needsReview: boolean;
  /** Id of the queued review item, null when no review is needed. */
  reviewId: string | null;
  /** Why the document was routed to review, empty when none. */
  reviewReasons: string[];
}

export type ReviewStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export type ReviewDecision = 'APPROVE' | 'REJECT';

/** A document queued for human review (mirrors the Java ReviewItem record). */
export interface ReviewItem {
  id: string;
  agentType: string;
  conversationId: string;
  extraction: ProcessedDocument;
  alerts: RuleResult[];
  reasons: string[];
  status: ReviewStatus;
  reviewer: string | null;
  reviewerNote: string | null;
  /** ISO-8601 instant when the item entered the queue. */
  createdAt: string;
  /** ISO-8601 instant when it was resolved, null while pending. */
  resolvedAt: string | null;
}

export async function fetchAgents(): Promise<string[]> {
  const res = await fetch(`${API_BASE}/api/agents`);
  if (!res.ok) {
    throw new Error(`Failed to load agents (${res.status})`);
  }
  return res.json();
}

export interface UploadParams {
  file: File;
  agentType?: string;
  question?: string;
  sessionId?: string;
}

export async function uploadDocument(params: UploadParams): Promise<DocumentResult> {
  const form = new FormData();
  form.append('file', params.file);
  if (params.agentType) form.append('agentType', params.agentType);
  if (params.question) form.append('question', params.question);
  if (params.sessionId) form.append('sessionId', params.sessionId);

  const res = await fetch(`${API_BASE}/api/documents`, { method: 'POST', body: form });
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`Processing failed (${res.status}). ${text}`);
  }
  return res.json();
}

// -------------------------------------------------------------------------
// Human-in-the-loop review API (backed by ReviewController).
// -------------------------------------------------------------------------

/** Pending review items, newest first. */
export async function fetchPendingReviews(): Promise<ReviewItem[]> {
  const res = await fetch(`${API_BASE}/api/review`);
  if (!res.ok) {
    throw new Error(`Failed to load review queue (${res.status})`);
  }
  return res.json();
}

/** Every review item regardless of status. */
export async function fetchAllReviews(): Promise<ReviewItem[]> {
  const res = await fetch(`${API_BASE}/api/review/all`);
  if (!res.ok) {
    throw new Error(`Failed to load review history (${res.status})`);
  }
  return res.json();
}

/** Record an approve or reject decision on a queued item and return the updated item. */
export async function resolveReview(
  id: string,
  decision: ReviewDecision,
  reviewer: string,
  note?: string,
): Promise<ReviewItem> {
  const res = await fetch(`${API_BASE}/api/review/${encodeURIComponent(id)}/resolve`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ decision, reviewer, note: note?.trim() ? note.trim() : undefined }),
  });
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`Could not save your decision (${res.status}). ${text}`);
  }
  return res.json();
}
