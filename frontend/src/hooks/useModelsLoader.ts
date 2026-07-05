import { useEffect } from 'react';
import { useAppStore } from '../lib/store';
import { fetchModels } from '../lib/api';
import { useAbortController } from './useAbortController';

export function useModelsLoader() {
  const setModels = useAppStore((s) => s.setModels);
  const setModelsLoading = useAppStore((s) => s.setModelsLoading);
  const setSelectedModel = useAppStore((s) => s.setSelectedModel);
  const selectedModel = useAppStore((s) => s.selectedModel);

  useAbortController([]); // ensure abort controller exists for potential extension

  useEffect(() => {
    let isMounted = true;

    fetchModels()
      .then((m) => {
        if (isMounted) {
          setModels(m);
          if (!selectedModel && m.length > 0) setSelectedModel(m[0].id);
        }
      })
      .catch((err) => {
        if (isMounted && err.name !== 'AbortError') {
          console.warn('[App] Failed to fetch models', err);
          setModels([]);
        }
      })
      .finally(() => {
        if (isMounted) setModelsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps
}
