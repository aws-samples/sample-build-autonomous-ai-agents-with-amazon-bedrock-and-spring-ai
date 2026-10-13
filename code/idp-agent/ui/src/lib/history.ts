// Per-user document history, persisted in localStorage so a user sees their previous
// and current reviews across sessions. In production this would live in a database
// behind the API.

import type { DocumentResult } from '../api';

/** The original uploaded document, kept so the user can re-open it from history. */
export interface UploadedDoc {
  name: string;
  mime: string;
  /** base64 data URI; omitted for very large files to stay within localStorage limits. */
  dataUri?: string;
}

export interface HistoryItem {
  id: string;
  ts: number;
  agentType: string;
  documentType: string;
  filename: string;
  doc?: UploadedDoc;
  question: string;
  blocked: boolean;
  alertCount: number;
  result: DocumentResult;
}

const MAX_ITEMS = 50;

function key(user: string): string {
  return `idp.history.${user}`;
}

export function loadHistory(user: string): HistoryItem[] {
  try {
    const s = localStorage.getItem(key(user));
    return s ? (JSON.parse(s) as HistoryItem[]) : [];
  } catch {
    return [];
  }
}

function save(user: string, items: HistoryItem[]): void {
  try {
    localStorage.setItem(key(user), JSON.stringify(items.slice(0, MAX_ITEMS)));
  } catch {
    // Storage full (large documents). Drop the stored document bytes and retry metadata only.
    const slim = items.slice(0, MAX_ITEMS).map((i) => ({ ...i, doc: i.doc ? { name: i.doc.name, mime: i.doc.mime } : undefined }));
    try {
      localStorage.setItem(key(user), JSON.stringify(slim));
    } catch {
      /* give up silently */
    }
  }
}

export function addHistory(
  user: string,
  doc: UploadedDoc,
  question: string,
  result: DocumentResult,
): HistoryItem {
  const item: HistoryItem = {
    id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    ts: Date.now(),
    agentType: result.agentType,
    documentType: result.extraction?.documentType ?? 'UNKNOWN',
    filename: doc.name,
    doc,
    question,
    blocked: result.blocked,
    alertCount: result.alerts?.length ?? 0,
    result,
  };
  save(user, [item, ...loadHistory(user)]);
  return item;
}

export function removeHistory(user: string, id: string): HistoryItem[] {
  const items = loadHistory(user).filter((i) => i.id !== id);
  save(user, items);
  return items;
}

export function clearHistory(user: string): void {
  save(user, []);
}

/** A readable plain-text summary suitable for sharing (copied to the clipboard). */
export function shareText(item: HistoryItem): string {
  const e = item.result.extraction;
  const alerts = item.result.alerts?.length
    ? item.result.alerts.map((a) => `${a.severity}: ${a.message}`).join('\n  ')
    : 'none';
  const lines = [
    `IDP Review, ${item.documentType} (reviewed by ${item.agentType})`,
    `File: ${item.filename}`,
    `When: ${new Date(item.ts).toLocaleString()}`,
    item.question ? `Question: ${item.question}` : '',
    '',
    `Summary: ${e?.summary ?? ''}`,
    e?.answer ? `Answer: ${e.answer}` : '',
    '',
    `Alerts:\n  ${alerts}`,
  ];
  return lines.filter((l) => l !== '').join('\n');
}

/** Read a File as a base64 data URI (used to keep the uploaded document in history). */
export function readAsDataUri(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = () => reject(reader.error);
    reader.readAsDataURL(file);
  });
}

/** Open a stored uploaded document (PDF, image, or text) in a new browser tab. */
export function openUploadedDoc(doc?: UploadedDoc): boolean {
  if (!doc?.dataUri) return false;
  try {
    const [meta, b64] = doc.dataUri.split(',');
    const mime = meta.match(/:(.*?);/)?.[1] ?? doc.mime ?? 'application/octet-stream';
    const bin = atob(b64);
    const bytes = new Uint8Array(bin.length);
    for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
    const url = URL.createObjectURL(new Blob([bytes], { type: mime }));
    window.open(url, '_blank', 'noopener');
    return true;
  } catch {
    return false;
  }
}
