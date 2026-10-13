import { useEffect, useState } from 'react';
import { fetchPendingReviews, resolveReview, type ReviewDecision, type ReviewItem } from '../api';
import { personaFor } from '../personas';
import { AlertsList } from './AlertsList';
import { ConfidenceBar } from './ConfidenceBar';

interface Props {
  /** Name of the signed-in reviewer, recorded with each decision. */
  reviewer: string;
  /** Notify the parent when the pending count changes (for the tab badge). */
  onCountChange: (count: number) => void;
  /** Emit a short confirmation message to the shared toast. */
  onNotify: (message: string) => void;
}

function timeAgo(iso: string): string {
  const ts = Date.parse(iso);
  if (Number.isNaN(ts)) return '';
  const s = Math.floor((Date.now() - ts) / 1000);
  if (s < 60) return 'just now';
  if (s < 3600) return `${Math.floor(s / 60)}m ago`;
  if (s < 86400) return `${Math.floor(s / 3600)}h ago`;
  return new Date(ts).toLocaleDateString();
}

/**
 * The human-in-the-loop review queue. Lists pending documents as cards and lets a
 * reviewer approve or reject each one with an optional note. Refreshes on open and after
 * every resolve, and keeps the parent's pending badge in sync.
 */
export function ReviewQueue({ reviewer, onCountChange, onNotify }: Props) {
  const [items, setItems] = useState<ReviewItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notes, setNotes] = useState<Record<string, string>>({});
  const [pending, setPending] = useState<Record<string, ReviewDecision>>({});

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchPendingReviews();
      setItems(data);
      onCountChange(data.length);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load the review queue');
    } finally {
      setLoading(false);
    }
  }

  // Refresh whenever the queue is opened.
  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function decide(item: ReviewItem, decision: ReviewDecision) {
    setPending((p) => ({ ...p, [item.id]: decision }));
    setError(null);
    try {
      await resolveReview(item.id, decision, reviewer, notes[item.id]);
      const remaining = items.filter((i) => i.id !== item.id);
      setItems(remaining);
      onCountChange(remaining.length);
      onNotify(decision === 'APPROVE' ? 'Document approved' : 'Document rejected');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not save your decision');
    } finally {
      setPending((p) => {
        const next = { ...p };
        delete next[item.id];
        return next;
      });
    }
  }

  return (
    <section className="review-queue" aria-label="Human review queue">
      <div className="review-head card">
        <div>
          <h2>Review queue</h2>
          <p className="card-sub" style={{ margin: 0 }}>
            Documents routed to a person for approval. Approve or reject each one, with an
            optional note.
          </p>
        </div>
        <button type="button" className="ghost" onClick={load} disabled={loading}>
          {loading ? 'Refreshing...' : 'Refresh'}
        </button>
      </div>

      {error && (
        <div className="banner banner-error" role="alert">
          <span aria-hidden="true">{'\u26A0\uFE0F'}</span>
          {error}
        </div>
      )}

      <div aria-live="polite">
        {loading && items.length === 0 && (
          <div className="card processing" role="status" aria-live="polite">
            <div className="processing-head">
              <span className="spinner" aria-hidden="true" />
              <strong>Loading documents awaiting review</strong>
            </div>
            <div aria-hidden="true">
              <div className="skeleton skeleton-line medium" />
              <div className="skeleton skeleton-line" />
              <div className="skeleton skeleton-line short" />
            </div>
          </div>
        )}

        {!loading && !error && items.length === 0 && (
          <div className="card empty">
            <div className="empty-badge" aria-hidden="true">{'\u2705'}</div>
            <h3>No documents awaiting review</h3>
            <p>
              When a document is blocked, alerted, or comes back with low confidence, it
              lands here for a person to approve or reject.
            </p>
          </div>
        )}

        {items.length > 0 && (
          <ul className="review-list">
            {items.map((item) => {
              const persona = personaFor(item.agentType);
              const busy = Boolean(pending[item.id]);
              return (
                <li className="card review-card" key={item.id}>
                  <div className="review-card-head">
                    <span className="persona-icon" aria-hidden="true">{persona.icon}</span>
                    <div className="review-card-titles">
                      <span className="review-card-agent">{persona.name}</span>
                      <span className="badge badge-type">
                        {item.extraction?.documentType || 'UNKNOWN'}
                      </span>
                    </div>
                    <span className="muted review-card-time">{timeAgo(item.createdAt)}</span>
                  </div>

                  {item.extraction?.summary && (
                    <p className="prose review-card-summary">{item.extraction.summary}</p>
                  )}

                  <div className="review-card-confidence">
                    <span className="section-label" style={{ margin: 0 }}>Confidence</span>
                    <ConfidenceBar value={item.extraction?.confidence ?? 0} />
                  </div>

                  {item.reasons?.length > 0 && (
                    <div className="review-reasons">
                      <span className="section-label">Why it needs review</span>
                      <ul className="banner-review-reasons">
                        {item.reasons.map((reason, i) => (
                          <li key={i}>{reason}</li>
                        ))}
                      </ul>
                    </div>
                  )}

                  {item.alerts?.length > 0 && (
                    <div className="review-alerts">
                      <span className="section-label">Workflow alerts</span>
                      <AlertsList alerts={item.alerts} />
                    </div>
                  )}

                  <label className="review-note" htmlFor={`note-${item.id}`}>
                    Note (optional)
                    <input
                      id={`note-${item.id}`}
                      type="text"
                      value={notes[item.id] ?? ''}
                      placeholder="Add context for this decision"
                      disabled={busy}
                      onChange={(e) => setNotes((n) => ({ ...n, [item.id]: e.target.value }))}
                    />
                  </label>

                  <div className="review-actions">
                    <button
                      type="button"
                      className="btn review-approve"
                      disabled={busy}
                      onClick={() => decide(item, 'APPROVE')}
                    >
                      {pending[item.id] === 'APPROVE' ? 'Approving...' : 'Approve'}
                    </button>
                    <button
                      type="button"
                      className="ghost review-reject"
                      disabled={busy}
                      onClick={() => decide(item, 'REJECT')}
                    >
                      {pending[item.id] === 'REJECT' ? 'Rejecting...' : 'Reject'}
                    </button>
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </section>
  );
}
