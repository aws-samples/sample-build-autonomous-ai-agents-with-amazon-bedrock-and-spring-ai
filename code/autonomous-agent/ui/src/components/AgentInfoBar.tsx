import type { AgentInfo } from '../api';

interface Props {
  info: AgentInfo | null;
  connected: boolean;
  theme: 'dark' | 'light';
  onToggleTheme: () => void;
}

export function AgentInfoBar({ info, connected, theme, onToggleTheme }: Props) {
  return (
    <aside className="info-bar">
      <div className="brand">
        <div className="brand-mark" aria-hidden="true">◆</div>
        <div>
          <div className="brand-title">{info?.name ?? 'Autonomous Agent'}</div>
          <div className={`status ${connected ? 'status-ok' : 'status-wait'}`}>
            <span className="status-dot" />
            {connected ? 'Connected' : 'Connecting...'}
          </div>
        </div>
      </div>

      <p className="muted">
        {info?.description ??
          'A Spring AI agent on Amazon Bedrock AgentCore that plans, browses the web, runs code, and remembers the conversation.'}
      </p>

      {info && (
        <>
          <h3>Capabilities</h3>
          <div className="chips">
            {info.capabilities.map((c) => (
              <span className="chip chip-cap" key={c}>
                {c}
              </span>
            ))}
          </div>

          <h3>Tools</h3>
          <div className="chips">
            {info.tools.map((t) => (
              <span className="chip chip-tool" key={t}>
                {t}
              </span>
            ))}
          </div>

          <h3>Model</h3>
          <code className="model">{info.model}</code>
        </>
      )}

      <div className="info-footer">
        <button className="theme-toggle" onClick={onToggleTheme} title="Toggle theme">
          {theme === 'dark' ? 'Light mode' : 'Dark mode'}
        </button>
        <a
          className="md-link"
          href="https://github.com/spring-ai-community/spring-ai-agentcore"
          target="_blank"
          rel="noopener noreferrer"
        >
          Spring AI AgentCore
        </a>
      </div>
    </aside>
  );
}
