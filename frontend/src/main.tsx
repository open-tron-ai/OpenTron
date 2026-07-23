import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { ErrorBoundary } from './components/ErrorBoundary';
import App from './App';
import { initApiBase } from './lib/api';
import { initAnalytics } from './lib/analytics';
import './index.css';

function applyTheme() {
  try {
    const raw = localStorage.getItem('OpenTron-settings');
    const settings = raw ? JSON.parse(raw) : {};
    const theme = settings.theme || 'system';
    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
      document.documentElement.classList.remove('light');
    } else if (theme === 'light') {
      document.documentElement.classList.add('light');
      document.documentElement.classList.remove('dark');
    }
  } catch { /* use system default */ }
}

applyTheme();

// Intercept all external links globally and open in system browser
function setupLinkInterceptor() {
  document.addEventListener('click', async (e) => {
    const target = (e.target as Element)?.closest('a');
    if (!target) return;

    const href = target.getAttribute('href');
    if (!href) return;

    // Only intercept http/https external links
    if (!href.startsWith('http://') && !href.startsWith('https://')) {
      return;
    }

    // Check if it's localhost (internal) - allow it
    try {
      const url = new URL(href);
      if (url.hostname === 'localhost' || url.hostname === '127.0.0.1') {
        return;
      }

      // External link - prevent default and open in system browser
      e.preventDefault();
      e.stopPropagation();

      try {
        const { open } = await import('@tauri-apps/plugin-shell');
        await open(href);
      } catch (err) {
        console.error('Failed to open URL:', err);
        // Fallback to window.open
        window.open(href, '_blank');
      }
    } catch (err) {
      console.error('Failed to parse URL:', err);
    }
  }, true); // Use capture phase to intercept before other handlers
}

setupLinkInterceptor();

// Fetch the API base URL from the Tauri backend before rendering.
// This ensures TRON_PORT is defined in one place (the Rust backend).
// In non-Tauri environments this is a no-op.
initApiBase().finally(() => {
  // Kick off analytics init in the background — it's never awaited so
  // a slow/failed identity fetch never delays UI render.
  void initAnalytics();

  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <ErrorBoundary>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </ErrorBoundary>
    </StrictMode>,
  );
});
