import { createElement, type ReactNode } from 'react';

const DATA_IMAGE_URL = /^data:image\/(png|jpe?g|gif|webp);base64,[a-z0-9+/=\s]+$/i;
const INLINE_TOKEN = /!\[([^\]]*)\]\(([^)\s]+)\)|\[([^\]]+)\]\(([^)\s]+)\)|`([^`]+)`|\*\*([^*]+)\*\*|\*([^*]+)\*/g;
const FENCED_CODE = /```([\w-]*)\n?([\s\S]*?)```/g;

function safeUrl(raw: string, allowDataImage = false): string | null {
  const value = raw.trim();

  if (allowDataImage && DATA_IMAGE_URL.test(value)) {
    return value;
  }

  try {
    const url = new URL(value);
    return url.protocol === 'https:' || url.protocol === 'http:' ? url.toString() : null;
  } catch {
    return null;
  }
}

function renderInline(text: string, keyPrefix: string): ReactNode[] {
  const nodes: ReactNode[] = [];
  let lastIndex = 0;
  let tokenIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = INLINE_TOKEN.exec(text)) !== null) {
    if (match.index > lastIndex) {
      nodes.push(text.slice(lastIndex, match.index));
    }

    const key = `${keyPrefix}-${tokenIndex++}`;

    if (match[1] !== undefined) {
      const url = safeUrl(match[2], true);
      nodes.push(
        url
          ? createElement('img', {
              key,
              className: 'md-img',
              src: url,
              alt: match[1],
              loading: 'lazy',
            })
          : match[1],
      );
    } else if (match[3] !== undefined) {
      const url = safeUrl(match[4]);
      nodes.push(
        url
          ? createElement(
              'a',
              {
                key,
                className: 'md-link',
                href: url,
                target: '_blank',
                rel: 'noopener noreferrer',
              },
              match[3],
            )
          : match[3],
      );
    } else if (match[5] !== undefined) {
      nodes.push(createElement('code', { key, className: 'md-code' }, match[5]));
    } else if (match[6] !== undefined) {
      nodes.push(createElement('strong', { key }, match[6]));
    } else if (match[7] !== undefined) {
      nodes.push(createElement('em', { key }, match[7]));
    }

    lastIndex = INLINE_TOKEN.lastIndex;
  }

  if (lastIndex < text.length) {
    nodes.push(text.slice(lastIndex));
  }

  return nodes;
}

function renderTextBlocks(input: string, keyPrefix: string): ReactNode[] {
  return input.split(/\n{2,}/).flatMap((block, index) => {
    const trimmed = block.trim();
    if (!trimmed) {
      return [];
    }

    const key = `${keyPrefix}-${index}`;
    const heading = trimmed.match(/^(#{1,4})\s+(.*)$/);
    if (heading) {
      return [
        createElement(
          `h${heading[1].length}`,
          { key, className: 'md-h' },
          renderInline(heading[2], `${key}-heading`),
        ),
      ];
    }

    const lines = trimmed.split('\n');
    if (lines.every((line) => /^\s*[-*]\s+/.test(line))) {
      return [
        createElement(
          'ul',
          { key, className: 'md-ul' },
          lines.map((line, itemIndex) =>
            createElement(
              'li',
              { key: `${key}-item-${itemIndex}` },
              renderInline(line.replace(/^\s*[-*]\s+/, ''), `${key}-item-${itemIndex}`),
            ),
          ),
        ),
      ];
    }

    const children: ReactNode[] = [];
    lines.forEach((line, lineIndex) => {
      if (lineIndex > 0) {
        children.push(createElement('br', { key: `${key}-break-${lineIndex}` }));
      }
      children.push(...renderInline(line, `${key}-line-${lineIndex}`));
    });

    return [createElement('p', { key, className: 'md-p' }, children)];
  });
}

/**
 * Render supported Markdown as React elements instead of constructing HTML strings.
 * React handles content and attribute escaping, while URLs are independently
 * allowlisted before they are assigned to links or images.
 */
export function renderMarkdown(input: string): ReactNode[] {
  const nodes: ReactNode[] = [];
  let lastIndex = 0;
  let fenceIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = FENCED_CODE.exec(input)) !== null) {
    nodes.push(...renderTextBlocks(input.slice(lastIndex, match.index), `block-${fenceIndex}`));

    const key = `fence-${fenceIndex++}`;
    const language = match[1];
    const props: { key: string; className: string; 'data-lang'?: string } = {
      key,
      className: 'md-pre',
    };
    if (language) {
      props['data-lang'] = language;
    }

    nodes.push(
      createElement(
        'pre',
        props,
        createElement('code', undefined, match[2].replace(/\n$/, '')),
      ),
    );
    lastIndex = FENCED_CODE.lastIndex;
  }

  nodes.push(...renderTextBlocks(input.slice(lastIndex), `block-${fenceIndex}`));
  return nodes;
}
