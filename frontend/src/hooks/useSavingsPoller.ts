import { useEffect } from 'react';
import { fetchSavings, submitSavings } from '../lib/api';
import { useAppStore } from '../lib/store';

export function useSavingsPoller() {
  const setSavings = useAppStore((s) => s.setSavings);
  const optInEnabled = useAppStore((s) => s.optInEnabled);
  const optInDisplayName = useAppStore((s) => s.optInDisplayName);
  const optInEmail = useAppStore((s) => s.optInEmail);
  const optInAnonId = useAppStore((s) => s.optInAnonId);

  useEffect(() => {
    let isMounted = true;

    const refresh = () => {
      if (!isMounted) return;

      fetchSavings()
        .then((data) => {
          if (!isMounted) return;

          setSavings(data);
          if (optInEnabled && optInDisplayName && data) {
            const claudeEntry = data.per_provider.find(
              (p) => p.provider === 'claude-opus-4.6',
            );
            const dollarSavings = claudeEntry ? claudeEntry.total_cost : 0;
            const energySaved = data.per_provider.reduce(
              (sum, p) => sum + (p.energy_wh || 0),
              0,
            );
            const flopsSaved = data.per_provider.reduce(
              (sum, p) => sum + (p.flops || 0),
              0,
            );
            submitSavings({
              anon_id: optInAnonId,
              display_name: optInDisplayName,
              email: optInEmail,
              total_calls: data.total_calls,
              total_tokens: data.total_tokens,
              dollar_savings: dollarSavings,
              energy_wh_saved: energySaved,
              flops_saved: flopsSaved,
              token_counting_version: data.token_counting_version ?? 1,
            });
          }
        })
        .catch((err) => {
          if (isMounted && err.name !== 'AbortError') {
            console.warn('[App] Failed to fetch savings', err);
          }
        });
    };

    refresh();
    const interval = setInterval(refresh, 30000);

    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, [optInEnabled, optInDisplayName, optInAnonId]); // eslint-disable-line react-hooks/exhaustive-deps
}
