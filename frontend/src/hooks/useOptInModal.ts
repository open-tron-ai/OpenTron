import { useEffect } from 'react';
import { useAppStore } from '../lib/store';

export function useOptInModal() {
  const optInModalSeen = useAppStore((s) => s.optInModalSeen);
  const setOptInModalOpen = useAppStore((s) => s.setOptInModalOpen);
  const markOptInModalSeen = useAppStore((s) => s.markOptInModalSeen);

  useEffect(() => {
    if (!optInModalSeen) {
      setOptInModalOpen(true);
      markOptInModalSeen();
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps
}
