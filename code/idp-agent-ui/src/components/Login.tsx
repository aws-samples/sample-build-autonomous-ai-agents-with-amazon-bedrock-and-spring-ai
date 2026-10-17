import { useState } from 'react';
import { LANDING_URL } from '../api';
import type { Theme } from '../lib/theme';
import { ThemeToggle } from './ThemeToggle';

interface Props {
  theme: Theme;
  onToggleTheme: () => void;
  onLogin: (name: string) => void;
}

export function Login({ theme, onToggleTheme, onLogin }: Props) {
  const [name, setName] = useState('');
  const [password, setPassword] = useState('');

  function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!name.trim()) return;
    onLogin(name);
  }

  return (
    <div className="login-page">
      <div className="login-theme">
        <a
          className="ghost"
          href={LANDING_URL}
          style={{ textDecoration: 'none' }}
        >
          Return to home
        </a>
        <ThemeToggle theme={theme} onToggle={onToggleTheme} />
      </div>

      <div className="login-card">
        <div className="login-brand">
          <span className="brand-mark" aria-hidden="true">IDP</span>
          <div>
            <h1>Ask an Expert</h1>
            <div className="status status-ok">
              <span className="status-dot" />
              Intelligent Document Processing
            </div>
          </div>
        </div>

        <p className="login-value">
          Upload a document and a specialist agent classifies it, extracts the key fields with
          confidence scores, and runs your workflow rules, all on Amazon Bedrock AgentCore with
          Spring AI.
        </p>

        <ul className="login-highlights">
          <li>
            <span className="tick" aria-hidden="true">{'\u2713'}</span>
            Lawyer, Mortgage, Insurance, Tax and General specialists
          </li>
          <li>
            <span className="tick" aria-hidden="true">{'\u2713'}</span>
            Confidence-scored fields plus severity-ranked alerts
          </li>
          <li>
            <span className="tick" aria-hidden="true">{'\u2713'}</span>
            Private per-user history you can re-open any time
          </li>
        </ul>

        <form className="login-form" onSubmit={submit}>
          <label>
            Name
            <input
              type="text"
              placeholder="Your name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              autoFocus
              autoComplete="name"
            />
          </label>
          <label>
            Password
            <input
              type="password"
              placeholder="Any value (demo)"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
            />
          </label>
          <button type="submit" className="btn btn-block" disabled={!name.trim()}>
            Sign in
          </button>
        </form>

        <p className="login-note">
          Demo sign-in, no real authentication. In production this is an Amazon Cognito
          (OAuth2 / JWT) login, and your document history is scoped to your verified identity.
        </p>
      </div>
    </div>
  );
}
