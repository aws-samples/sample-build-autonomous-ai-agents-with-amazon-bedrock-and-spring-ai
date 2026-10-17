import { useRef, useState } from 'react';
import type { UploadParams } from '../api';
import { SAMPLES, fetchSampleFile } from '../samples';
import { personaFor } from '../personas';
import { PersonaPicker } from './PersonaPicker';

interface Props {
  agents: string[];
  busy: boolean;
  onSubmit: (params: UploadParams) => void;
}

const ACCEPT = '.txt,.pdf,.png,.jpg,.jpeg,application/pdf,image/*,text/plain';
const ALLOWED_EXT = ['txt', 'pdf', 'png', 'jpg', 'jpeg'];
const MAX_BYTES = 15 * 1024 * 1024; // 15 MB

function isAllowed(file: File): boolean {
  const ext = file.name.split('.').pop()?.toLowerCase() ?? '';
  const okType =
    file.type.startsWith('image/') ||
    file.type.startsWith('text/') ||
    file.type === 'application/pdf' ||
    file.type === '';
  return okType && (ALLOWED_EXT.includes(ext) || file.type !== '');
}

function humanSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function UploadForm({ agents, busy, onSubmit }: Props) {
  const [agentType, setAgentType] = useState('');
  const [question, setQuestion] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [dragging, setDragging] = useState(false);
  const [fileError, setFileError] = useState<string | null>(null);
  const fileInput = useRef<HTMLInputElement>(null);

  function acceptFile(next: File | null) {
    if (!next) return;
    if (!isAllowed(next)) {
      setFileError('Unsupported file type. Use a PDF, image (PNG or JPG), or text file.');
      return;
    }
    if (next.size > MAX_BYTES) {
      setFileError(`File is too large (${humanSize(next.size)}). The limit is 15 MB.`);
      return;
    }
    setFileError(null);
    setFile(next);
  }

  function onDrop(e: React.DragEvent) {
    e.preventDefault();
    setDragging(false);
    if (busy) return;
    acceptFile(e.dataTransfer.files?.[0] ?? null);
  }

  function openPicker() {
    if (!busy) fileInput.current?.click();
  }

  function clearFile() {
    setFile(null);
    setFileError(null);
    if (fileInput.current) fileInput.current.value = '';
  }

  function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!file || busy) return;
    onSubmit({ file, agentType: agentType || undefined, question: question || undefined });
  }

  async function runSample(id: string) {
    const sample = SAMPLES.find((s) => s.id === id);
    if (!sample || busy) return;
    try {
      const sampleFile = await fetchSampleFile(sample);
      setAgentType(sample.agentType);
      setQuestion(sample.question);
      setFile(sampleFile);
      setFileError(null);
      onSubmit({ file: sampleFile, agentType: sample.agentType, question: sample.question });
    } catch {
      setFileError('Could not load that sample document. Please try again.');
    }
  }

  return (
    <form className="card form-stack" onSubmit={submit}>
      <div>
        <h2>Review a document</h2>
        <p className="card-sub">
          Pick a specialist (or auto-detect), attach a document, and ask a question.
        </p>
      </div>

      <div>
        <p className="section-label" id="persona-label">
          1. Choose a specialist
        </p>
        <PersonaPicker agents={agents} value={agentType} busy={busy} onChange={setAgentType} />
      </div>

      <div>
        <p className="section-label">2. Add your document</p>
        {file ? (
          <div className="file-pill">
            <span className="file-icon" aria-hidden="true">{'\uD83D\uDCC4'}</span>
            <span className="file-meta">
              <span className="file-name">{file.name}</span>
              <span className="file-size">{humanSize(file.size)}</span>
            </span>
            <button type="button" className="ghost" onClick={clearFile} disabled={busy}>
              Remove
            </button>
          </div>
        ) : (
          <div
            className={`dropzone ${dragging ? 'dragging' : ''} ${fileError ? 'invalid' : ''}`}
            onClick={openPicker}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                openPicker();
              }
            }}
            onDragOver={(e) => {
              e.preventDefault();
              if (!busy) setDragging(true);
            }}
            onDragLeave={() => setDragging(false)}
            onDrop={onDrop}
            role="button"
            tabIndex={0}
            aria-label="Upload a document. Drag and drop a file here, or activate to browse."
          >
            <div className="dropzone-icon" aria-hidden="true">{'\u2191'}</div>
            <div className="dropzone-title">Drag and drop your document here</div>
            <div className="dropzone-hint">or click to browse. PDF, image, or text, up to 15 MB.</div>
          </div>
        )}
        <input
          ref={fileInput}
          type="file"
          className="sr-only"
          accept={ACCEPT}
          onChange={(e) => acceptFile(e.target.files?.[0] ?? null)}
          disabled={busy}
          tabIndex={-1}
        />
        {fileError && (
          <p className="field-error" role="alert">
            {fileError}
          </p>
        )}
      </div>

      <label>
        3. Ask a question (optional)
        <input
          type="text"
          placeholder="e.g. Is this lease tenant-friendly? What are the risks?"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          disabled={busy}
        />
      </label>

      <button type="submit" className="btn btn-block" disabled={busy || !file}>
        {busy ? 'Reviewing your document...' : 'Review document'}
      </button>

      <div className="samples">
        <span className="section-label" style={{ margin: 0 }}>
          No document handy? Try a sample
        </span>
        <div className="samples-chips">
          {SAMPLES.map((s) => {
            const p = personaFor(s.agentType);
            return (
              <button
                key={s.id}
                type="button"
                className="sample-chip"
                onClick={() => runSample(s.id)}
                disabled={busy}
                title={s.question}
              >
                <span aria-hidden="true">{p.icon}</span>
                {s.label}
              </button>
            );
          })}
        </div>
      </div>
    </form>
  );
}
