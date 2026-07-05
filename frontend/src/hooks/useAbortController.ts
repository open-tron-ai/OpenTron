import { useEffect, useRef } from 'react';

/**
 * Hook to manage AbortController lifecycle for fetch operations
 * Automatically aborts pending requests on unmount or dependency change
 * 
 * @returns An AbortSignal to pass to fetch calls within the effect
 * @example
 * ```tsx
 * const signal = useAbortController([dependency1, dependency2]);
 * useEffect(() => {
 *   fetch('/api/data', { signal }).then(...)
 * }, [dependency1, dependency2])
 * ```
 */
export const useAbortController = (deps?: React.DependencyList): AbortSignal => {
  const controllerRef = useRef<AbortController>(new AbortController());

  useEffect(() => {
    // Create new controller for this effect cycle
    controllerRef.current = new AbortController();

    return () => {
      // Abort pending requests on cleanup
      controllerRef.current.abort();
    };
  }, deps);

  return controllerRef.current.signal;
};

/**
 * Wrapper around fetch that properly handles AbortSignal
 * @param input URL or Request object
 * @param init Request init options
 * @returns Promise<Response>
 */
export const fetchWithSignal = (
  input: RequestInfo | URL,
  init?: RequestInit & { signal?: AbortSignal },
): Promise<Response> => {
  return fetch(input, init);
};
