// Light/dark theme management. The active theme is applied to the <html> element via a
// `data-theme` attribute (matching the design tokens in styles.css) and persisted in
// localStorage so a user's choice survives reloads.

export type Theme = 'dark' | 'light';

const KEY = 'idp.theme';

/** Read the persisted theme, falling back to the OS preference, then dark. */
export function getInitialTheme(): Theme {
  try {
    const saved = localStorage.getItem(KEY);
    if (saved === 'light' || saved === 'dark') {
      return saved;
    }
  } catch {
    /* localStorage unavailable, fall through */
  }
  if (typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: light)').matches) {
    return 'light';
  }
  return 'dark';
}

/** Apply a theme to the document root and persist it. */
export function applyTheme(theme: Theme): void {
  if (typeof document !== 'undefined') {
    document.documentElement.setAttribute('data-theme', theme);
  }
  try {
    localStorage.setItem(KEY, theme);
  } catch {
    /* ignore persistence errors */
  }
}
