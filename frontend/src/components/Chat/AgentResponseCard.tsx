import { useMemo } from 'react';
import ReactMarkdown from 'react-markdown';
import rehypeHighlight from 'rehype-highlight';
import rehypeKatex from 'rehype-katex';
import remarkGfm from 'remark-gfm';
import remarkMath from 'remark-math';
import type { ChatMessage } from '../../types';

interface AgentResponseCardProps {
  message: ChatMessage;
  isThinking?: boolean;
}

function CodeBlockPre({ children, ...props }: any) {
  const codeElement = Array.isArray(children) ? children[0] : children;
  const className = codeElement?.props?.className || '';
  const match = /language-([\w-]+)/.exec(className);
  const lang = match ? match[1] : '';

  return (
    <div
      className="code-block-wrapper relative my-3"
      style={{ borderRadius: 'var(--radius-md)', overflow: 'hidden' }}
    >
      <div
        className="flex items-center justify-between px-4 py-1.5 text-xs"
        style={{ background: 'var(--color-bg-tertiary)', color: 'var(--color-text-tertiary)' }}
      >
        <span className="font-mono">{lang || 'code'}</span>
      </div>
      <pre {...props} style={{ margin: 0, borderRadius: 0 }}>
        {children}
      </pre>
    </div>
  );
}

function getAgentName(content: string): string {
  // Extract agent name from formats like:
  // "🤔 Backend Agent is thinking..."
  // "✓ Backend Agent\n\n[response]"
  // "❌ Backend Agent\n\n[error]"
  const match = content.match(/(?:🤔|💭|✓|❌)\s+(\w+)\s+(?:Agent|agent)/);
  return match ? match[1] : 'Agent';
}

function isThinkingMessage(content: string): boolean {
  return content.includes('is thinking') || content.includes('thinking');
}

function extractResponseContent(content: string): string {
  // If content starts with emoji + Agent, extract the response part
  // Format: "✓ Backend Agent\n\n[response content]"
  const match = content.match(/^[🤔💭✓❌]\s+\w+\s+Agent\s*\n\n([\s\S]*)$/);
  return match ? match[1] : content;
}

function sanitizeMarkdown(markdown: string): string {
  // Fix malformed code blocks: standalone language identifiers before ```
  // e.g., "java\n```" -> "```java"
  markdown = markdown.replace(/^(java|javascript|typescript|python|java|csharp|go|rust|cpp|c|sql|html|css|xml|json|yaml|bash|shell|sh)\n(```)/gm, '```$1');
  
  // Fix language identifiers followed by backticks without newline
  // e.g., "java```" -> "```java"
  markdown = markdown.replace(/^(java|javascript|typescript|python|csharp|go|rust|cpp|c|sql|html|css|xml|json|yaml|bash|shell|sh)```/gm, '```$1');
  
  // Remove stray language keywords that appear before code blocks
  markdown = markdown.replace(/^(less|markdown)\n(```)/gm, '```');
  
  // Fix typos in annotations and keywords
  markdown = markdown.replace(/@Cacheab\s*le/g, '@Cacheable');
  markdown = markdown.replace(/\.leng\s*th\(/g, '.length(');
  markdown = markdown.replace(/O\(\s*1\)/g, 'O(1)');
  
  // Fix bold formatting issues
  markdown = markdown.replace(/\*\s+\*/g, '**');
  
  return markdown;
}

export function AgentResponseCard({ message, isThinking }: AgentResponseCardProps) {
  const agentName = useMemo(() => getAgentName(message.content), [message.content]);
  const thinking = useMemo(() => isThinking !== undefined ? isThinking : isThinkingMessage(message.content), [message.content, isThinking]);
  const responseContent = useMemo(() => sanitizeMarkdown(extractResponseContent(message.content)), [message.content]);

  if (thinking) {
    // Thinking state - simple loading indicator
    return (
      <div className="mb-3 rounded-lg p-3 border animate-pulse" style={{ borderColor: 'var(--color-border)', background: 'var(--color-bg-secondary)' }}>
        <div className="flex items-center justify-between">
          <div style={{ fontSize: '14px', fontWeight: 600 }}>
            🤔 {agentName} Agent
          </div>
        </div>
        <div style={{ fontSize: '13px', color: 'var(--color-text-tertiary)', marginTop: '8px' }}>
          Thinking...
        </div>
      </div>
    );
  }

  // Full response state
  return (
    <div className="mb-3 rounded-lg p-4 border" style={{ borderColor: 'var(--color-border)', background: 'var(--color-bg-secondary)' }}>
      <div className="flex items-center justify-between mb-3">
        <div style={{ fontSize: '13px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.5px', color: 'var(--color-text-secondary)' }}>
          ✓ {agentName} Agent Response
        </div>
      </div>
      <div className="prose max-w-none text-sm">
        <ReactMarkdown
          remarkPlugins={[remarkGfm, remarkMath]}
          rehypePlugins={[[rehypeHighlight, { detect: true }], rehypeKatex]}
          components={{ pre: CodeBlockPre }}
        >
          {responseContent}
        </ReactMarkdown>
      </div>
    </div>
  );
}
