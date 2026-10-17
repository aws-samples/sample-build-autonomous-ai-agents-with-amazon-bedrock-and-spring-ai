import type { RuleResult, Severity } from '../api';

const SEVERITY_BADGE: Record<Severity, string> = {
  INFO: 'badge badge-info',
  WARN: 'badge badge-warn',
  ALERT: 'badge badge-alert',
  BLOCK: 'badge badge-block',
};

const SEVERITY_ROW: Record<Severity, string> = {
  INFO: '',
  WARN: 'sev-warn',
  ALERT: 'sev-alert',
  BLOCK: 'sev-block',
};

// Highest severity first, so blocking issues surface at the top.
const ORDER: Record<Severity, number> = { BLOCK: 0, ALERT: 1, WARN: 2, INFO: 3 };

export function AlertsList({ alerts }: { alerts: RuleResult[] }) {
  if (!alerts || alerts.length === 0) {
    return <p className="muted">No workflow rules triggered.</p>;
  }
  const sorted = [...alerts].sort((a, b) => ORDER[a.severity] - ORDER[b.severity]);
  return (
    <ul className="alerts">
      {sorted.map((a) => (
        <li key={a.ruleId} className={`alert-item ${SEVERITY_ROW[a.severity]}`}>
          <span className={SEVERITY_BADGE[a.severity]}>{a.severity}</span>
          <span className="alert-message">{a.message}</span>
          <span className="alert-rule mono">{a.ruleId}</span>
        </li>
      ))}
    </ul>
  );
}
