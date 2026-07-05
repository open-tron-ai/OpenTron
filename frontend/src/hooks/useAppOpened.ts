import { useEffect } from 'react';
import { track } from '../lib/analytics';

export function useAppOpened() {
  useEffect(() => {
    const t = setTimeout(() => {
      track('app_opened', {});
    }, 500);
    return () => clearTimeout(t);
  }, []);
}
