interface Props {
  value: number;
  /** Hide the numeric label (used in tight table-like layouts if needed). */
  showLabel?: boolean;
}

export function ConfidenceBar({ value, showLabel = true }: Props) {
  const pct = Math.max(0, Math.min(100, Math.round(value * 100)));
  const tone = pct >= 80 ? 'high' : pct >= 60 ? 'medium' : 'low';
  const toneLabel = tone === 'high' ? 'High' : tone === 'medium' ? 'Medium' : 'Low';
  return (
    <div
      className="confidence"
      role="progressbar"
      aria-valuenow={pct}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-label={`Confidence ${pct} percent, ${toneLabel}`}
      title={`${toneLabel} confidence: ${pct}%`}
    >
      <div className={`confidence-fill confidence-${tone}`} style={{ width: `${pct}%` }} />
      {showLabel && <span className="confidence-label">{pct}%</span>}
    </div>
  );
}
