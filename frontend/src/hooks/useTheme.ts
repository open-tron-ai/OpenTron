import { useEffect } from 'react';
import { useAppStore } from '../lib/store';

export function useTheme() {
  const settings = useAppStore((s) => s.settings);

  useEffect(() => {
    const root = document.documentElement;
    root.classList.remove('dark', 'light');
    if (settings.theme === 'dark') root.classList.add('dark');
    else if (settings.theme === 'light') root.classList.add('light');
  }, [settings.theme]);
}
