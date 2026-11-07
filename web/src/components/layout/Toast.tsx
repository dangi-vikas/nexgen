import { useEffect } from 'react';
import { ArrowRight, X } from 'lucide-react';
import { useUiStore } from '@/store/ui';
import { useCartStore } from '@/store/cart';

export function Toast() {
  const toast = useUiStore((state) => state.toast);
  const hideToast = useUiStore((state) => state.hideToast);
  const openCart = useCartStore((state) => state.open);

  useEffect(() => {
    if (!toast) return;
    const timer = window.setTimeout(hideToast, 4500);
    return () => window.clearTimeout(timer);
  }, [toast, hideToast]);

  if (!toast) return null;

  return (
    <div
      className="toast-in fixed bottom-4 left-4 right-4 z-[60] sm:bottom-6 sm:left-auto sm:right-6 sm:w-[370px]"
      role="status"
      aria-live="polite"
    >
      <div className="flex items-center gap-3.5 rounded-2xl border border-line bg-paper p-3 shadow-[0_24px_60px_-20px_rgba(16,35,28,0.45)]">
        {toast.image ? (
          <span className="size-14 shrink-0 overflow-hidden rounded-xl bg-sage/50">
            <img src={toast.image} alt="" width={112} height={140} className="size-full object-cover" />
          </span>
        ) : null}
        <div className="min-w-0 flex-1">
          <p className="text-[13.5px] font-semibold tracking-[-0.01em]">{toast.title}</p>
          {toast.detail ? <p className="mt-0.5 truncate text-xs text-muted">{toast.detail}</p> : null}
          <button
            type="button"
            onClick={() => {
              hideToast();
              openCart();
            }}
            className="mt-1.5 inline-flex items-center gap-1 text-xs font-bold text-forest hover:text-clay-dark"
          >
            View bag
            <ArrowRight className="size-3" aria-hidden="true" />
          </button>
        </div>
        <button
          type="button"
          onClick={hideToast}
          className="grid size-8 shrink-0 place-items-center self-start rounded-full text-muted transition-colors hover:bg-sage hover:text-ink"
          aria-label="Dismiss notification"
        >
          <X className="size-4" aria-hidden="true" />
        </button>
      </div>
    </div>
  );
}
