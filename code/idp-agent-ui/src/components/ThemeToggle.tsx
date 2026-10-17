import type { Theme } from '../lib/theme';

interface Props {
  theme: Theme;
  onToggle: () => void;
}

/** Small persisted light/dark switch used in the login and dashboard chrome. */
export function ThemeToggle({ theme, onToggle }: Props) {
  const next = theme === 'dark' ? 'light' : 'dark';
  return (
    <button
      type="button"
      className="theme-toggle"
      onClick={onToggle}
      aria-label={`Switch to ${next} mode`}
      title={`Switch to ${next} mode`}
    >
      <span aria-hidden="true">{theme === 'dark' ? '\u2600\uFE0F' : '\uD83C\uDF19'}</span>
      {theme === 'dark' ? 'Light' : 'Dark'}
    </button>
  );
}
