import { useEffect, useRef } from 'react';
import { useAppStore } from '../lib/store';
import { track, hashId } from '../lib/analytics';

export function useModelChangeTracker() {
  const prevModelRef = useRef<string>('');
  const selectedModel = useAppStore((s) => s.selectedModel);

  useEffect(() => {
    const prev = prevModelRef.current;
    const curr = selectedModel || '';
    prevModelRef.current = curr;
    if (!prev || !curr || prev === curr) return;
    void (async () => {
      const [fromHash, toHash] = await Promise.all([hashId(prev), hashId(curr)]);
      track('model_changed', {
        from_model_hash: fromHash,
        to_model_hash: toHash,
      });
    })();
  }, [selectedModel]);
}
