import { useEffect } from 'react';
import { ChatArea } from '../components/Chat/ChatArea';
import { SystemPanel } from '../components/Chat/SystemPanel';
import { useAppStore } from '../lib/store';

export function ChatPage() {
  const activeId = useAppStore((s) => s.activeId);
  const conversations = useAppStore((s) => s.conversations);
  const createConversation = useAppStore((s) => s.createConversation);
  const selectConversation = useAppStore((s) => s.selectConversation);
  const systemPanelOpen = useAppStore((s) => s.systemPanelOpen);

  // Auto-create first conversation if none exist
  useEffect(() => {
    if (conversations.length === 0 && !activeId) {
      createConversation();
    }
  }, [conversations.length, activeId, createConversation]);

  // Auto-select first conversation if none selected
  useEffect(() => {
    if (!activeId && conversations.length > 0) {
      selectConversation(conversations[0].id);
    }
  }, [activeId, conversations, selectConversation]);

  return (
    <div className="flex h-full overflow-hidden bg-slate-950">
      <div className="flex-1 min-w-0">
        <ChatArea />
      </div>
      {systemPanelOpen && <SystemPanel />}
    </div>
  );
}
