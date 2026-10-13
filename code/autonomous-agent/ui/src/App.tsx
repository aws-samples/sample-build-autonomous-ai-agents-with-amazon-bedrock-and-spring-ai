import { useEffect, useMemo, useRef, useState } from 'react';
import { fetchAgentInfo, streamChat, type AgentInfo } from './api';
import { AgentInfoBar } from './components/AgentInfoBar';
import { ChatMessage, type Message } from './components/ChatMessage';

interface Suggestion {
  label: string;
  prompt: string;
  tag: string;
}

// Each suggestion exercises a different AgentCore capability so first-time users can
// see planning, web browsing, code execution, and the domain tools in one place.
const SUGGESTIONS: Suggestion[] = [
  {
    tag: 'Plan + tools',
    label: 'Plan a 3-day Tokyo trip with weather and a rough fare',
    prompt:
      'Plan a 3-day trip to Tokyo. Check the weather, estimate a round-trip fare from Seattle, and suggest points of interest.',
  },
  {
    tag: 'Code interpreter',
    label: 'Plot a sine wave and return the chart',
    prompt:
      'Use the code interpreter to plot y = sin(x) for x from 0 to 2*pi with matplotlib. Save it as sine.png and show me the chart.',
  },
  {
    tag: 'Web browsing',
    label: 'Browse a page and summarize it',
    prompt: 'Browse https://spring.io and summarize what the project is in three bullet points.',
  },
  {
    tag: 'Data analysis (hard)',
    label: 'Cohort retention + churn from raw events with pandas',
    prompt:
      'Use the code interpreter with pandas. Create a DataFrame of subscription events with columns [user_id, event, ts] where event is one of signup, active, cancel, and ts is a datetime, using this data: user 1 signup 2024-01-05, active 2024-02-03, active 2024-03-10, cancel 2024-04-02; user 2 signup 2024-01-20, active 2024-02-15; user 3 signup 2024-02-11, active 2024-03-01, cancel 2024-03-20; user 4 signup 2024-03-05, active 2024-04-09. Build a monthly signup-cohort retention matrix (percentage of each cohort active in month 0..N), compute the overall monthly churn rate, and identify which signup cohort has the best month-2 retention. Show the code, the retention matrix, and a one-line conclusion.',
  },
];

const THEME_KEY = 'agent-theme';

function newSessionId(): string {
  return `web-${Math.random().toString(36).slice(2, 10)}`;
}

export function App() {
  const [info, setInfo] = useState<AgentInfo | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [theme, setTheme] = useState<'dark' | 'light'>(
    () => (localStorage.getItem(THEME_KEY) as 'dark' | 'light') ?? 'dark',
  );
  const sessionId = useRef(newSessionId());
  const listEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    fetchAgentInfo().then(setInfo).catch((e) => setError(String(e)));
  }, []);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem(THEME_KEY, theme);
  }, [theme]);

  useEffect(() => {
    listEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Auto-grow the composer up to a cap.
  useEffect(() => {
    const el = textareaRef.current;
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 180)}px`;
  }, [input]);

  const turns = useMemo(() => messages.filter((m) => m.role === 'user').length, [messages]);

  async function send(prompt: string) {
    const text = prompt.trim();
    if (!text || busy) return;

    setError(null);
    setBusy(true);
    setInput('');

    const userMsg: Message = { id: `u-${Date.now()}`, role: 'user', content: text };
    const assistantId = `a-${Date.now()}`;
    setMessages((prev) => [
      ...prev,
      userMsg,
      { id: assistantId, role: 'assistant', content: '', streaming: true },
    ]);

    try {
      await streamChat({
        prompt: text,
        sessionId: sessionId.current,
        onToken: (chunk) => {
          setMessages((prev) =>
            prev.map((m) => (m.id === assistantId ? { ...m, content: m.content + chunk } : m)),
          );
        },
      });
    } catch (e) {
      setError(String(e));
      setMessages((prev) =>
        prev.map((m) =>
          m.id === assistantId
            ? { ...m, content: m.content || 'Something went wrong. Please try again.' }
            : m,
        ),
      );
    } finally {
      setMessages((prev) => prev.map((m) => (m.id === assistantId ? { ...m, streaming: false } : m)));
      setBusy(false);
    }
  }

  function resetConversation() {
    sessionId.current = newSessionId();
    setMessages([]);
    setError(null);
  }

  return (
    <div className="app">
      <AgentInfoBar
        info={info}
        connected={info != null}
        theme={theme}
        onToggleTheme={() => setTheme((t) => (t === 'dark' ? 'light' : 'dark'))}
      />

      <main className="chat">
        <header className="chat-header">
          <div>
            <h1>{info?.name ?? 'Autonomous Agent'}</h1>
            <span className="session-tag">
              session {sessionId.current} · {turns} turn{turns === 1 ? '' : 's'}
            </span>
          </div>
          <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
            <a
              className="ghost"
              href="http://localhost:8080/"
              style={{ textDecoration: 'none' }}
            >
              Return to home
            </a>
            <button className="ghost" onClick={resetConversation} disabled={busy}>
              New conversation
            </button>
          </div>
        </header>

        <div className="messages">
          {messages.length === 0 && (
            <div className="empty">
              <div className="empty-badge">◆</div>
              <h2>What can I help you build today?</h2>
              <p>
                I plan work in steps and use tools to get it done: web browsing, a code sandbox,
                live facts, and per-session memory. Try one of these:
              </p>
              <div className="suggestions">
                {SUGGESTIONS.map((s) => (
                  <button
                    key={s.label}
                    className="suggestion"
                    onClick={() => send(s.prompt)}
                    disabled={busy}
                  >
                    <span className="suggestion-tag">{s.tag}</span>
                    <span className="suggestion-label">{s.label}</span>
                  </button>
                ))}
              </div>
            </div>
          )}
          {messages.map((m) => (
            <ChatMessage key={m.id} message={m} />
          ))}
          <div ref={listEndRef} />
        </div>

        {error && <div className="banner error">{error}</div>}

        <form
          className="composer"
          onSubmit={(e) => {
            e.preventDefault();
            send(input);
          }}
        >
          <textarea
            ref={textareaRef}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                send(input);
              }
            }}
            placeholder="Ask the agent to plan, browse, or run code... (Enter to send, Shift+Enter for a new line)"
            rows={1}
            disabled={busy}
          />
          <button type="submit" disabled={busy || !input.trim()}>
            {busy ? 'Thinking...' : 'Send'}
          </button>
        </form>
      </main>
    </div>
  );
}
