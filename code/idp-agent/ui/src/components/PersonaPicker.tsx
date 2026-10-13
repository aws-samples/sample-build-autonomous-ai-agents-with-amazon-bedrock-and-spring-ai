import { personaFor } from '../personas';

interface Props {
  agents: string[];
  /** Empty string means auto-detect. */
  value: string;
  busy: boolean;
  onChange: (agentType: string) => void;
}

/**
 * Card-based specialist picker. Renders an "Auto-detect" option plus one card per agent
 * returned by the backend, showing what each specialist extracts. Behaves like a radio
 * group for keyboard and screen-reader users.
 */
export function PersonaPicker({ agents, value, busy, onChange }: Props) {
  return (
    <div className="persona-grid" role="radiogroup" aria-label="Choose a specialist">
      <button
        type="button"
        role="radio"
        aria-checked={value === ''}
        className={`persona-card ${value === '' ? 'selected' : ''}`}
        onClick={() => onChange('')}
        disabled={busy}
      >
        <div className="persona-top">
          <span className="persona-icon" aria-hidden="true">{'\u2728'}</span>
          <span className="persona-name">Auto-detect</span>
          {value === '' && <span className="persona-check" aria-hidden="true">{'\u2713'}</span>}
        </div>
        <p className="persona-tagline">Let the classifier route to the best specialist.</p>
        <div className="persona-extracts">
          <span className="tag">Smart routing</span>
        </div>
      </button>

      {agents.map((agent) => {
        const p = personaFor(agent);
        const selected = value === agent;
        return (
          <button
            key={agent}
            type="button"
            role="radio"
            aria-checked={selected}
            className={`persona-card ${selected ? 'selected' : ''}`}
            onClick={() => onChange(agent)}
            disabled={busy}
          >
            <div className="persona-top">
              <span className="persona-icon" aria-hidden="true">{p.icon}</span>
              <span className="persona-name">{p.name}</span>
              {selected && <span className="persona-check" aria-hidden="true">{'\u2713'}</span>}
            </div>
            <p className="persona-tagline">{p.tagline}</p>
            <div className="persona-extracts">
              {p.extracts.map((x) => (
                <span className="tag" key={x}>
                  {x}
                </span>
              ))}
            </div>
          </button>
        );
      })}
    </div>
  );
}
