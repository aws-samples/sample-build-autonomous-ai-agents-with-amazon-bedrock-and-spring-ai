/** Animated placeholder shown while the backend classifies and extracts a document. */
export function ProcessingCard() {
  return (
    <div className="card processing" role="status" aria-live="polite">
      <div className="processing-head">
        <span className="spinner" aria-hidden="true" />
        <div>
          <strong>The specialist is reading your document</strong>
          <div className="muted" style={{ fontSize: '0.85rem' }}>
            Classifying, extracting fields, and running workflow rules.
          </div>
        </div>
      </div>
      <div className="progress-track" aria-hidden="true">
        <div className="progress-indeterminate" />
      </div>
      <div aria-hidden="true">
        <div className="skeleton skeleton-line medium" />
        <div className="skeleton skeleton-line" />
        <div className="skeleton skeleton-line short" />
      </div>
    </div>
  );
}
