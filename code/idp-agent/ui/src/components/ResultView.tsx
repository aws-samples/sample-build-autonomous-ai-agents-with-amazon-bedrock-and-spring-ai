import type { DocumentResult } from '../api';
import { personaFor } from '../personas';
import { AlertsList } from './AlertsList';
import { ConfidenceBar } from './ConfidenceBar';

export function ResultView({ result }: { result: DocumentResult }) {
  const { extraction } = result;
  const persona = personaFor(result.agentType);
  const lowConfidence = typeof extraction.confidence === 'number' && extraction.confidence < 0.6;

  return (
    <div className="card result">
      <div className="result-head">
        <h2>Result</h2>
        <span className="badge badge-agent" title="Specialist that handled the document">
          <span aria-hidden="true">{persona.icon}</span> {persona.name}
        </span>
        <span className="badge badge-type" title="Classified document type">
          {extraction.documentType || 'UNKNOWN'}
        </span>
      </div>

      {result.blocked && (
        <div className="banner banner-block" role="alert">
          <span aria-hidden="true">{'\u26D4'}</span>
          Blocked for manual review, a BLOCK-severity rule fired.
        </div>
      )}

      {result.needsReview && (
        <div className="banner banner-review" role="status">
          <span className="banner-review-head">
            <span aria-hidden="true">{'\uD83D\uDC65'}</span>
            Needs human review
          </span>
          {result.reviewReasons?.length > 0 && (
            <ul className="banner-review-reasons">
              {result.reviewReasons.map((reason, i) => (
                <li key={i}>{reason}</li>
              ))}
            </ul>
          )}
          <span className="banner-review-note">
            This document was routed to the review queue for a person to approve or reject.
          </span>
        </div>
      )}

      {!result.blocked && lowConfidence && (
        <div className="banner banner-lowconf" role="status">
          <span aria-hidden="true">{'\u26A0\uFE0F'}</span>
          Low overall confidence. Review the extracted fields carefully before relying on them.
        </div>
      )}

      <section>
        <h3 className="section-label">Overall confidence</h3>
        <div className="confidence-overall">
          <ConfidenceBar value={extraction.confidence} />
        </div>
      </section>

      {extraction.summary && (
        <section>
          <h3 className="section-label">Summary</h3>
          <p className="prose">{extraction.summary}</p>
        </section>
      )}

      {extraction.answer && (
        <section>
          <h3 className="section-label">Answer</h3>
          <p className="prose answer-box">{extraction.answer}</p>
        </section>
      )}

      <section>
        <h3 className="section-label">Extracted fields</h3>
        {extraction.fields?.length ? (
          <div className="field-list">
            {extraction.fields.map((f) => (
              <div className="field-row" key={f.name}>
                <span className="field-name">{f.name}</span>
                <span className={`field-value ${f.value ? '' : 'empty'}`}>
                  {f.value || 'Not found'}
                </span>
                <ConfidenceBar value={f.confidence} />
              </div>
            ))}
          </div>
        ) : (
          <p className="muted">No fields extracted from this document.</p>
        )}
      </section>

      {extraction.validationIssues?.length > 0 && (
        <section>
          <h3 className="section-label">Validation issues</h3>
          <ul className="issues">
            {extraction.validationIssues.map((issue, i) => (
              <li key={i}>{issue}</li>
            ))}
          </ul>
        </section>
      )}

      <section>
        <h3 className="section-label">Workflow alerts</h3>
        <AlertsList alerts={result.alerts} />
      </section>
    </div>
  );
}
