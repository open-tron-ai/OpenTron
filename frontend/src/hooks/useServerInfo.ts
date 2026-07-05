import { useEffect } from 'react';
import { fetchServerInfo } from '../lib/api';
import { useAppStore } from '../lib/store';

export function useServerInfo() {
  const setServerInfo = useAppStore((s) => s.setServerInfo);

  useEffect(() => {
    let isMounted = true;

    fetchServerInfo()
      .then((info) => {
        if (isMounted) setServerInfo(info);
      })
      .catch((err) => {
        if (isMounted && err.name !== 'AbortError') {
          console.warn('[App] Failed to fetch server info', err);
        }
      });

    return () => {
      isMounted = false;
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps
}
