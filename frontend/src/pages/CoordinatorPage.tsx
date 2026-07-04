import { CoordinatorPanel } from '../components/Coordinator/CoordinatorPanel';
import { ChevronLeft } from 'lucide-react';

export function CoordinatorPage({ onBack }: { onBack: () => void }) {
  return (
    <div className="flex-1 overflow-y-auto px-6 py-10">
      <div className="max-w-6xl mx-auto">
        {/* Back button */}
        <button
          onClick={onBack}
          className="flex items-center gap-1 mb-4 text-sm cursor-pointer"
          style={{ color: 'var(--color-text-secondary)' }}
        >
          <ChevronLeft size={16} /> Back to agents
        </button>

        {/* Header */}
        <div className="mb-6">
          <h1 className="text-2xl font-bold mb-2" style={{ color: 'var(--color-text)' }}>
            AI Multi-Agent Coordinator
          </h1>
          <p
            className="text-sm max-w-2xl"
            style={{ color: 'var(--color-text-secondary)' }}
          >
            Route tasks to specialized AI agents (Backend, Frontend, QA, DevOps) running
            in parallel. The coordinator analyzes your request and delegates to the
            appropriate experts in under a second.
          </p>
        </div>

        {/* Coordinator Panel */}
        <CoordinatorPanel />
      </div>
    </div>
  );
}
