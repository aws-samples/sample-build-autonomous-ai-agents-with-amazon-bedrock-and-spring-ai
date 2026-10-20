import { useEffect, useState } from 'react';
import { LANDING_URL, fetchAgents, uploadDocument, type UploadParams } from '../api';
import type { User } from '../lib/auth';
import type { Theme } from '../lib/theme';
import {
  addHistory,
  clearHistory,
  loadHistory,
  openUploadedDoc,
  readAsDataUri,
  removeHistory,
  shareText,
  type HistoryItem,
} from '../lib/history';
import { fetchPendingReviews } from '../api';
import { UploadForm } from './UploadForm';
import { ResultView } from './ResultView';
import { HistoryPanel } from './HistoryPanel';
import { ProcessingCard } from './ProcessingCard';
import { ThemeToggle } from './ThemeToggle';
import { ReviewQueue } from './ReviewQueue';

interface Props {
  user: User;
  theme: Theme;
  onToggleTheme: () => void;
  onLogout: () => void;
}

export function Dashboard({ user, theme, onToggleTheme, onLogout }: Props) {
  const [agents, setAgents] = useState<string[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [history, setHistory] = useState<HistoryItem[]>([]);
  const [selected, setSelected] = useState<HistoryItem | null>(null);
  const [toast, setToast] = useState<string | null>(null);
  const [view, setView] = useState<'process' | 'review'>('process');
  const [pendingReviews, setPendingReviews] = useState(0);

  useEffect(() => {
    fetchAgents().then(setAgents).catch((e) => setError(e.message));
    setHistory(loadHistory(user.name));
    refreshReviewCount();
  }, [user.name]);

  function flash(msg: string) {
    setToast(msg);
    window.setTimeout(() => setToast(null), 2200);
  }

  // Keep the pending badge fresh: on load, after processing, and on demand.
  function refreshReviewCount() {
    fetchPendingReviews()
      .then((items) => setPendingReviews(items.length))
      .catch(() => {
        /* badge is best-effort; ignore transient errors */
      });
  }

  async function handleSubmit(params: UploadParams) {
    setBusy(true);
    setError(null);
    setSelected(null);
    try {
      const file = params.file;
      // Keep the original document so the user can re-open it from history.
      let dataUri: string | undefined;
      if (file.size <= 1_500_000) {
        try {
          dataUri = await readAsDataUri(file);
        } catch {
          dataUri = undefined;
        }
      }
      const result = await uploadDocument({ ...params, sessionId: user.name });
      const doc = { name: file.name, mime: file.type || 'application/octet-stream', dataUri };
      const item = addHistory(user.name, doc, params.question ?? '', result);
      setHistory(loadHistory(user.name));
      setSelected(item);
      // A freshly processed document may have been routed to review.
      if (result.needsReview) {
        setPendingReviews((n) => n + 1);
      } else {
        refreshReviewCount();
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unexpected error');
    } finally {
      setBusy(false);
    }
  }

  function openDoc(item: HistoryItem) {
    if (!openUploadedDoc(item.doc)) {
      flash('Original document is not available for this entry');
    }
  }

  function share(item: HistoryItem) {
    const text = shareText(item);
    if (navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(text).then(
        () => flash('Review copied to clipboard'),
        () => flash('Copy failed'),
      );
    } else {
      flash('Clipboard not available');
    }
  }

  function remove(id: string) {
    setHistory(removeHistory(user.name, id));
    if (selected?.id === id) setSelected(null);
  }

  function clearAll() {
    clearHistory(user.name);
    setHistory([]);
    setSelected(null);
  }

  const showEmpty = !busy && !error && !selected;

  return (
    <div className="dash">
      <header className="topbar">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">IDP</span>
          <span className="brand-name">Ask an Expert</span>
        </div>
        <nav className="topbar-tabs" aria-label="Views">
          <button
            type="button"
            className={`tab ${view === 'process' ? 'tab-active' : ''}`}
            aria-current={view === 'process' ? 'page' : undefined}
            onClick={() => setView('process')}
          >
            Process
          </button>
          <button
            type="button"
            className={`tab ${view === 'review' ? 'tab-active' : ''}`}
            aria-current={view === 'review' ? 'page' : undefined}
            onClick={() => {
              setView('review');
              refreshReviewCount();
            }}
          >
            Review queue
            {pendingReviews > 0 && (
              <span className="tab-badge" aria-label={`${pendingReviews} pending`}>
                {pendingReviews}
              </span>
            )}
          </button>
        </nav>
        <div className="topbar-right">
          <a
            className="ghost"
            href={LANDING_URL}
            style={{ textDecoration: 'none' }}
          >
            Return to home
          </a>
          <span className="topbar-user">
            Signed in as <strong>{user.name}</strong>
          </span>
          <ThemeToggle theme={theme} onToggle={onToggleTheme} />
          <button type="button" className="ghost" onClick={onLogout}>
            Sign out
          </button>
        </div>
      </header>

      {view === 'review' ? (
        <div className="dash-single">
          <ReviewQueue
            reviewer={user.name}
            onCountChange={setPendingReviews}
            onNotify={flash}
          />
        </div>
      ) : (
      <div className="dash-grid">
        <main className="dash-main">
          <UploadForm agents={agents} busy={busy} onSubmit={handleSubmit} />

          {error && (
            <div className="banner banner-error" role="alert">
              <span aria-hidden="true">{'\u26A0\uFE0F'}</span>
              {error}
            </div>
          )}

          {busy && <ProcessingCard />}

          {selected && !busy && (
            <div className="detail">
              <div className="detail-bar">
                <div>
                  <span className="detail-file">{selected.filename}</span>
                  <span className="muted"> {'\u00B7'} {new Date(selected.ts).toLocaleString()}</span>
                </div>
                <div className="detail-actions">
                  {selected.doc?.dataUri && (
                    <button type="button" className="ghost" onClick={() => openDoc(selected)}>
                      View document
                    </button>
                  )}
                  <button type="button" className="ghost" onClick={() => share(selected)}>
                    Share
                  </button>
                </div>
              </div>
              <ResultView result={selected.result} />
            </div>
          )}

          {showEmpty && (
            <div className="card empty">
              <div className="empty-badge" aria-hidden="true">{'\uD83D\uDCC4'}</div>
              <h3>Your results will appear here</h3>
              <p>
                Choose a specialist above, drop in a document or try a sample, and the extraction,
                confidence, and alerts show up right here.
              </p>
            </div>
          )}
        </main>

        <HistoryPanel
          items={history}
          activeId={selected?.id ?? null}
          onOpen={setSelected}
          onOpenDoc={openDoc}
          onShare={share}
          onDelete={remove}
          onClear={clearAll}
        />
      </div>
      )}

      {toast && (
        <div className="toast" role="status">
          {toast}
        </div>
      )}
    </div>
  );
}
