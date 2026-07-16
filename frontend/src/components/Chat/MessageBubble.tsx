import { useState, useMemo } from 'react';
import ReactMarkdown from 'react-markdown';
import rehypeHighlight from 'rehype-highlight';
import rehypeKatex from 'rehype-katex';
import remarkGfm from 'remark-gfm';
import remarkMath from 'remark-math';
import 'katex/dist/katex.min.css';
import { Copy, Check } from 'lucide-react';
import { AudioPlayer } from './AudioPlayer';
import { ToolCallCard } from './ToolCallCard';
import { ResearchTimeline } from './ResearchTimeline';
import { AgentResponseCard } from './AgentResponseCard';
import { rehypeCitations } from '../../lib/rehype-citations';
import { XRayFooter } from './XRayFooter';
import type { ChatMessage } from '../../types';

function stripThinkTags(text: string): string {
  let cleaned = text.replace(/<think>[\s\S]*?<\/think>\s*/gi, '');
  cleaned = cleaned.replace(/^[\s\S]*?<\/think>\s*/i, '');
  return cleaned.trim();
}

interface Props {
  message: ChatMessage;
  isLive?: boolean;
}

function getTextContent(node: any): string {
  if (typeof node === 'string' || typeof node === 'number') {
    return String(node);
  }
  if (Array.isArray(node)) {
    return node.map(getTextContent).join('');
  }
  if (node?.props?.children) {
    return getTextContent(node.props.children);
  }
  return '';
}

function CodeBlockPre({ children, ...props }: any) {
  const [copied, setCopied] = useState(false);
  const codeElement = Array.isArray(children) ? children[0] : children;
  const className = codeElement?.props?.className || '';
  const match = /language-([\w-]+)/.exec(className);
  const lang = match ? match[1] : '';
  const code = getTextContent(codeElement?.props?.children).replace(/\n$/, '');

  const handleCopy = () => {
    navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

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
        <button
          onClick={handleCopy}
          className="flex items-center gap-1 px-2 py-0.5 rounded transition-colors cursor-pointer"
          style={{ color: 'var(--color-text-tertiary)' }}
          onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--color-text-secondary)')}
          onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--color-text-tertiary)')}
        >
          {copied ? <Check size={12} /> : <Copy size={12} />}
          {copied ? 'Copied' : 'Copy'}
        </button>
      </div>
      <pre {...props} style={{ margin: 0, borderRadius: 0 }}>
        {children}
      </pre>
    </div>
  );
}

function CopyMessageButton({ content }: { content: string }) {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <button
      onClick={handleCopy}
      className="p-1 rounded opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer"
      style={{ color: 'var(--color-text-tertiary)' }}
      title="Copy message"
    >
      {copied ? <Check size={14} /> : <Copy size={14} />}
    </button>
  );
}

export function MessageBubble({ message, isLive = false }: Props) {
  const isUser = message.role === 'user';

  if (isUser) {
    return (
      <div className="flex justify-end mb-4">
        <div
          className="max-w-[85%] px-4 py-2.5 text-sm leading-relaxed"
          style={{
            background: 'var(--color-user-bubble)',
            color: 'var(--color-user-bubble-text)',
            borderRadius: 'var(--radius-xl) var(--radius-xl) var(--radius-sm) var(--radius-xl)',
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-word',
          }}
        >
          {message.content}
        </div>
      </div>
    );
  }

  const cleanContent = useMemo(() => stripThinkTags(message.content), [message.content]);
  
  // Check if this is an Analysis Results message
  const isAnalysisResults = cleanContent?.startsWith('## 🎯 Analysis Results');

  // Check if this is an agent response message (thinking or completed)
  const isAgentResponse = useMemo(() => {
    const content = cleanContent || '';
    // Agent messages start with emoji + agent name
    return /^[🤔💭✓❌]\s+\w+\s+(?:Agent|agent)/.test(content);
  }, [cleanContent]);
  // Detect grouped agent sections formatted as:
  // **AgentName Agent:**\n<content>\n\n**Other Agent Agent:**\n<content>
  const agentSections = useMemo(() => {
    const text = cleanContent || '';
    const regex = /\*\*([^*]+?) Agent:\*\*\s*\n?([\s\S]*?)(?=(\n\n\*\*[^*]+? Agent:\*\*|$))/g;
    const parts: { name: string; text: string }[] = [];
    let m: RegExpExecArray | null;
    while ((m = regex.exec(text)) !== null) {
      const name = (m[1] || '').trim();
      const txt = (m[2] || '').trim();
      if (name) parts.push({ name, text: txt });
    }
    return parts.length > 0 ? parts : null;
  }, [cleanContent]);

  // Build a ref→source lookup once per render. Memoized so the rehype plugin
  // identity stays stable until the source list actually changes.
  const sourcesMap = useMemo(() => {
    const m = new Map<number, NonNullable<ChatMessage['researchSources']>[number]>();
    for (const s of message.researchSources ?? []) {
      if (typeof s.ref === 'number') m.set(s.ref, s);
    }
    return m;
  }, [message.researchSources]);

  const rehypePlugins = useMemo(() => {
    const base: any[] = [[rehypeHighlight, { detect: true }], rehypeKatex];
    if (sourcesMap.size > 0) base.push([rehypeCitations, { sources: sourcesMap }]);
    return base;
  }, [sourcesMap]);

  return (
    <div className="group mb-6">
      {/* Deep Research timeline (steps + status) */}
      {(message.isResearch || (message.researchTraces && message.researchTraces.length > 0)) && (
        <ResearchTimeline
          traces={message.researchTraces ?? []}
          isLive={isLive}
          hasContent={cleanContent.length > 0}
        />
      )}

      {/* Tool calls */}
      {message.toolCalls && message.toolCalls.length > 0 && (
        <div className="mb-3 flex flex-col gap-2">
          {message.toolCalls.map((tc) => (
            <ToolCallCard key={tc.id} toolCall={tc} />
          ))}
        </div>
      )}

      {/* Audio player (e.g. morning digest) */}
      {message.audio?.url && <AudioPlayer src={message.audio.url} />}

      {/* Agent response card (thinking or completed) */}
      {isAgentResponse && <AgentResponseCard message={{ ...message, content: cleanContent }} />}

      {/* Assistant message or grouped agent cards */}
      {!isAgentResponse && agentSections ? (
        <div className="flex flex-col gap-3">
          {agentSections.map((sec, index) => (
            <div key={`${sec.name}-${index}`} className="rounded-lg p-3 border" style={{ borderColor: 'var(--color-border)', background: 'var(--color-bg-secondary)' }}>
              <div className="flex items-center justify-between mb-2">
                <div style={{ fontSize: '12px', fontWeight: 600 }}>{sec.name} Agent</div>
              </div>
              <div className="prose max-w-none">
                <ReactMarkdown
                  remarkPlugins={[remarkGfm, remarkMath]}
                  rehypePlugins={rehypePlugins}
                  components={{ pre: CodeBlockPre }}
                >
                  {sec.text}
                </ReactMarkdown>
              </div>
            </div>
          ))}
        </div>
      ) : !isAgentResponse && isAnalysisResults ? (
        <div className="rounded-lg p-4 border-2" style={{ borderColor: 'var(--color-accent, #10b981)', background: 'var(--color-bg-secondary)' }}>
          <div className="prose max-w-none">
            <ReactMarkdown
              remarkPlugins={[remarkGfm, remarkMath]}
              rehypePlugins={rehypePlugins}
              components={{
                pre: CodeBlockPre,
              }}
            >
              {cleanContent}
            </ReactMarkdown>
          </div>
        </div>
      ) : (
        !isAgentResponse && cleanContent && (
          <div className="prose max-w-none">
            <ReactMarkdown
              remarkPlugins={[remarkGfm, remarkMath]}
              rehypePlugins={rehypePlugins}
              components={{
                pre: CodeBlockPre,
              }}
            >
              {cleanContent}
            </ReactMarkdown>
          </div>
        )
      )}

      {/* Footer: copy + x-ray */}
      <div className="flex items-center gap-2 mt-1.5">
        <CopyMessageButton content={cleanContent} />
      </div>
      <XRayFooter
        usage={message.usage}
        telemetry={message.telemetry}
        isResearch={message.isResearch}
      />
    </div>
  );
}
