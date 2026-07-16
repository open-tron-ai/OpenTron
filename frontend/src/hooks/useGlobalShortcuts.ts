import { useEffect } from 'react';
import { useAppStore } from '../lib/store';

export function useGlobalShortcuts() {
  const commandPaletteOpen = useAppStore((s) => s.commandPaletteOpen);
  const setCommandPaletteOpen = useAppStore((s) => s.setCommandPaletteOpen);
  const toggleSystemPanel = useAppStore((s) => s.toggleSystemPanel);
  const createConversation = useAppStore((s) => s.createConversation);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      const isMetaOrCtrl = e.metaKey || e.ctrlKey;
      
      if (isMetaOrCtrl && e.key === 'k') {
        e.preventDefault();
        setCommandPaletteOpen(!commandPaletteOpen);
      }
      if (isMetaOrCtrl && e.key === 'i') {
        e.preventDefault();
        toggleSystemPanel();
      }
      if (isMetaOrCtrl && e.key === 'n') {
        e.preventDefault();
        createConversation();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [commandPaletteOpen, setCommandPaletteOpen, toggleSystemPanel, createConversation]);
}
