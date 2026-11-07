import { useEffect, useRef, type RefObject } from 'react';

export function useLockBodyScroll(active: boolean) {
  useEffect(() => {
    if (!active) return;
    const original = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = original;
    };
  }, [active]);
}

export function useEscape(active: boolean, onEscape: () => void) {
  useEffect(() => {
    if (!active) return;
    function handle(event: KeyboardEvent) {
      if (event.key === 'Escape') onEscape();
    }
    window.addEventListener('keydown', handle);
    return () => window.removeEventListener('keydown', handle);
  }, [active, onEscape]);
}

const FOCUSABLE_SELECTOR = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled]):not([type="hidden"])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(',');

/**
 * Keeps keyboard focus inside an open dialog and restores it to the element
 * that opened the dialog when it closes.
 */
export function useFocusTrap(active: boolean, containerRef: RefObject<HTMLElement | null>) {
  const previousRef = useRef<HTMLElement | null>(null);
  const wasActiveRef = useRef(false);

  // Capture the trigger during render, before autoFocus can move focus into
  // the dialog.
  if (active && !wasActiveRef.current && typeof document !== 'undefined') {
    previousRef.current =
      document.activeElement instanceof HTMLElement ? document.activeElement : null;
  }
  wasActiveRef.current = active;

  useEffect(() => {
    if (!active) return;
    const container = containerRef.current;
    if (!container) return;
    const node = container;
    const previous = previousRef.current;

    function getFocusable() {
      return Array.from(node.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR)).filter(
        (element) => element.getClientRects().length > 0,
      );
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key !== 'Tab') return;
      const items = getFocusable();
      if (items.length === 0) {
        event.preventDefault();
        return;
      }
      const first = items[0];
      const last = items[items.length - 1];
      const current = document.activeElement;
      if (event.shiftKey) {
        if (current === first || !node.contains(current)) {
          event.preventDefault();
          last.focus();
        }
        return;
      }
      if (current === last || !node.contains(current)) {
        event.preventDefault();
        first.focus();
      }
    }

    document.addEventListener('keydown', handleKeyDown, true);
    const frame = window.requestAnimationFrame(() => {
      if (!node.contains(document.activeElement)) {
        getFocusable()[0]?.focus();
      }
    });

    return () => {
      window.cancelAnimationFrame(frame);
      document.removeEventListener('keydown', handleKeyDown, true);
      // Let React finish removing the dialog before restoring focus.
      window.setTimeout(() => {
        if (previous && document.contains(previous)) previous.focus();
      }, 0);
    };
  }, [active, containerRef]);
}
