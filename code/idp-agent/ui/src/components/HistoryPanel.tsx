import type { HistoryItem } from '../lib/history';
import { personaFor } from '../personas';

interface Props {
  items: HistoryItem[];
  activeId: string | null;
  onOpen: (item: HistoryItem) => void;
  onOpenDoc: (item: HistoryItem) => void;
  onShare: (item: HistoryItem) => void;
  onDelete: (id: string) => void;
  onClear: () => void;
}

function timeAgo(ts: number): string {
  const s = Math.floor((Date.now() - ts) / 1000);
  if (s < 60) return 'just now';
  if (s < 3600) return `${Math.floor(s / 60)}m ago`;
  if (s < 86400) return `${Math.floor(s / 3600)}h ago`;
  return new Date(ts).toLocaleDateString();
}

function docKind(item: HistoryItem): string {
  const mime = item.doc?.mime ?? '';
  if (mime.includes('pdf')) return 'PDF';
  if (mime.startsWith('image/')) return 'IMAGE';
  if (mime.startsWith('text/')) return 'TEXT';
  const ext = item.filename.split('.').pop();
  return ext ? ext.toUpperCase() : 'DOC';
}

export function HistoryPanel({ items, activeId, onOpen, onOpenDoc, onShare, onDelete, onClear }: Props) {
  return (
    <aside className="card history" aria-label="Document history">
      <div className="history-head">
        <h2>History</h2>
        {items.length > 0 && (
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span className="history-count">
              {items.length} {items.length === 1 ? 'doc' : 'docs'}
            </span>
            <button type="button" className="link-btn danger" onClick={onClear}>
              Clear all
            </button>
          </div>
        )}
      </div>

      {items.length === 0 ? (
        <div className="empty">
          <div className="empty-badge" aria-hidden="true">{'\uD83D\uDDC2\uFE0F'}</div>
          <h3>No reviews yet</h3>
          <p>Documents you review will appear here so you can re-open them any time.</p>
        </div>
      ) : (
        <ul className="history-list">
          {items.map((item) => {
            const persona = personaFor(item.agentType);
            return (
              <li
                key={item.id}
                className={`history-item ${item.id === activeId ? 'history-item-active' : ''}`}
              >
                <button
                  type="button"
                  className="history-main"
                  onClick={() => onOpen(item)}
                  aria-current={item.id === activeId ? 'true' : undefined}
                >
                  <div className="history-row">
                    <span className="history-persona" aria-hidden="true" title={persona.name}>
                      {persona.icon}
                    </span>
                    <span className="badge badge-type">{item.documentType}</span>
                    <span className="badge badge-file">{docKind(item)}</span>
                  </div>
                  <div className="history-file">{item.filename}</div>
                  <div className="history-meta">
                    <span>{timeAgo(item.ts)}</span>
                    {item.alertCount > 0 && (
                      <span className="history-alerts">
                        {item.alertCount} alert{item.alertCount > 1 ? 's' : ''}
                      </span>
                    )}
                    {item.blocked && <span className="history-blocked">BLOCKED</span>}
                  </div>
                </button>
                <div className="history-actions">
                  <button type="button" className="link-btn" onClick={() => onOpen(item)}>
                    Open
                  </button>
                  {item.doc?.dataUri && (
                    <button type="button" className="link-btn" onClick={() => onOpenDoc(item)}>
                      Document
                    </button>
                  )}
                  <button type="button" className="link-btn" onClick={() => onShare(item)}>
                    Share
                  </button>
                  <button
                    type="button"
                    className="link-btn danger"
                    onClick={() => onDelete(item.id)}
                  >
                    Delete
                  </button>
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </aside>
  );
}
