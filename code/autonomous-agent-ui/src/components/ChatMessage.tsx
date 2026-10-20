import { useState } from 'react';
import { renderMarkdown } from '../lib/markdown';

export type Role = 'user' | 'assistant';

export interface Message {
  id: string;
  role: Role;
  content: string;
  streaming?: boolean;
}

export function ChatMessage({ message }: { message: Message }) {
  const isUser = message.role === 'user';
  const [copied, setCopied] = useState(false);

  async function copy() {
    try {
      await navigator.clipboard.writeText(message.content);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      /* clipboard unavailable, ignore */
    }
  }

  return (
    <div className={`msg ${isUser ? 'msg-user' : 'msg-assistant'}`}>
      <div className="msg-avatar" aria-hidden="true">
        {isUser ? 'You' : 'AI'}
      </div>
      <div className="msg-body">
        <div className="msg-role">
          <span>{isUser ? 'You' : 'Agent'}</span>
          {!isUser && message.content && !message.streaming && (
            <button className="msg-copy" onClick={copy} title="Copy reply">
              {copied ? 'Copied' : 'Copy'}
            </button>
          )}
        </div>
        <div className="msg-bubble">
          {isUser ? (
            <span className="msg-text">{message.content}</span>
          ) : (
            <div className="md">{renderMarkdown(message.content)}</div>
          )}
          {message.streaming && <span className="caret" aria-hidden="true" />}
        </div>
      </div>
    </div>
  );
}
