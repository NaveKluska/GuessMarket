import { useEffect, useRef, useState } from 'react';

export const POLL_INTERVAL_MS = 1000;

/**
 * Calls fetcher now, then again one second after each answer arrives - never while a previous
 * call is still in flight, so a slow server can't pile up overlapping requests or have an older
 * answer land after a newer one. When deps change (e.g. a different event is selected) the old
 * result is dropped so stale data never shows under a new heading. While enabled is false (its
 * tab is hidden) polling pauses but the last result is kept. A failed call keeps the last good
 * data and just reports the error. keepOnChange keeps the old result through a deps change, for
 * callers whose results are looked up by name and so can never be shown under the wrong heading.
 */
export function usePoll(fetcher, deps, enabled = true, keepOnChange = false) {
    const [data, setData] = useState(null);
    const [error, setError] = useState(null);
    const tickRef = useRef(null);
    const keyRef = useRef(null);
    const key = JSON.stringify(deps);

    useEffect(() => {
        if (keyRef.current !== key) {
            keyRef.current = key;
            if (!keepOnChange) {
                setData(null);
                setError(null);
            }
        }
        if (!enabled) {
            tickRef.current = null;
            return undefined;
        }

        let cancelled = false;
        let running = false;
        let again = false;
        let timer = null;

        async function tick() {
            if (running) {
                // refresh() during an in-flight call: run once more as soon as it finishes, so
                // the screen reflects the action instead of the answer that was already on its way.
                again = true;
                return;
            }
            running = true;
            clearTimeout(timer);
            try {
                const result = await fetcher();
                if (!cancelled) {
                    setData(result);
                    setError(null);
                }
            } catch (ex) {
                if (!cancelled) {
                    setError(ex.message);
                }
            } finally {
                running = false;
                if (!cancelled) {
                    timer = setTimeout(tick, again ? 0 : POLL_INTERVAL_MS);
                    again = false;
                }
            }
        }

        tickRef.current = tick;
        tick();
        return () => {
            cancelled = true;
            clearTimeout(timer);
        };
    }, [key, enabled]);

    return { data, error, refresh: () => tickRef.current?.() };
}

/** Runs fn over items with at most limit calls in flight, keeping the results in order. */
export async function mapLimit(items, limit, fn) {
    const results = new Array(items.length);
    let next = 0;
    async function worker() {
        while (next < items.length) {
            const index = next++;
            results[index] = await fn(items[index]);
        }
    }
    await Promise.all(Array.from({ length: Math.min(limit, items.length) }, worker));
    return results;
}
