import { useEffect } from 'react';
import { isTauri } from '../lib/api';
import { useAppStore } from '../lib/store';

export function useOverlayImporter() {
  const importOverlay = useAppStore((s) => s.importOverlayConversation);

  useEffect(() => {
    if (!isTauri()) return;
    importOverlay();
    const interval = setInterval(importOverlay, 5000);
    return () => clearInterval(interval);
  }, [importOverlay]);
}
