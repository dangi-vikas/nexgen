import { useRef } from 'react';
import { ArrowRight, ShoppingBag, Trash2, X } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useCartStore, useCartSubtotal, FREE_SHIPPING_THRESHOLD } from '@/store/cart';
import { formatPrice } from '@/lib/utils';
import { QuantityStepper } from '@/components/ui/QuantityStepper';
import { useEscape, useFocusTrap, useLockBodyScroll } from '@/components/ui/useOverlay';

export function CartDrawer() {
  const panelRef = useRef<HTMLElement>(null);
  const isOpen = useCartStore((state) => state.isOpen);
  const close = useCartStore((state) => state.close);
  const lines = useCartStore((state) => state.lines);
  const setQuantity = useCartStore((state) => state.setQuantity);
  const remove = useCartStore((state) => state.remove);
  const subtotal = useCartSubtotal();
  const itemCount = lines.reduce((total, line) => total + line.quantity, 0);
  const remaining = Math.max(0, FREE_SHIPPING_THRESHOLD - subtotal);
  const progress = Math.min(100, (subtotal / FREE_SHIPPING_THRESHOLD) * 100);

  useLockBodyScroll(isOpen);
  useEscape(isOpen, close);
  useFocusTrap(isOpen, panelRef);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50" role="dialog" aria-modal="true" aria-labelledby="cart-title">
      <button
        type="button"
        className="absolute inset-0 bg-ink/45 fade-in"
        onClick={close}
        aria-label="Close bag"
        tabIndex={-1}
      />
      <aside
        ref={panelRef}
        className="drawer-enter absolute inset-y-0 right-0 flex w-full max-w-[460px] flex-col bg-cream shadow-[0_0_80px_rgba(16,35,28,0.3)]"
      >
        <div className="flex items-center justify-between border-b border-line px-5 py-4 sm:px-6">
          <div>
            <h2 id="cart-title" className="text-lg font-semibold tracking-[-0.03em]">
              Your bag
            </h2>
            <p className="mt-0.5 text-xs text-muted">
              {lines.length === 0
                ? 'Nothing here yet'
                : `${itemCount} ${itemCount === 1 ? 'item' : 'items'}`}
            </p>
          </div>
          <button
            type="button"
            onClick={close}
            className="grid size-10 place-items-center rounded-full border border-line transition-colors hover:bg-sage"
            aria-label="Close bag"
            autoFocus
          >
            <X className="size-5" aria-hidden="true" />
          </button>
        </div>

        {lines.length === 0 ? (
          <div className="flex flex-1 flex-col items-center justify-center px-8 text-center">
            <span className="grid size-16 place-items-center rounded-full bg-sage text-forest">
              <ShoppingBag className="size-7" strokeWidth={1.5} aria-hidden="true" />
            </span>
            <h3 className="mt-5 text-xl font-semibold tracking-[-0.03em]">Your bag is empty</h3>
            <p className="mt-2 max-w-xs text-sm leading-relaxed text-muted">
              Start with the everyday essentials, or upload a prescription and we’ll take it from
              there.
            </p>
            <Link to="/shop" onClick={close} className="btn btn-primary mt-6">
              Shop essentials
              <ArrowRight className="size-4" aria-hidden="true" />
            </Link>
            <Link
              to="/prescriptions"
              onClick={close}
              className="link-quiet mt-4"
            >
              Upload a prescription
            </Link>
          </div>
        ) : (
          <>
            <div className="border-b border-line px-5 py-4 sm:px-6">
              <p className="text-[13px] text-muted">
                {remaining > 0 ? (
                  <>
                    You’re <span className="font-bold text-ink">{formatPrice(remaining)}</span> away
                    from free delivery.
                  </>
                ) : (
                  <span className="font-semibold text-forest">Free delivery unlocked.</span>
                )}
              </p>
              <div
                className="mt-2 h-1.5 overflow-hidden rounded-full bg-sage"
                role="progressbar"
                aria-valuenow={Math.round(progress)}
                aria-valuemin={0}
                aria-valuemax={100}
                aria-label="Progress towards free delivery"
              >
                <div
                  className="h-full rounded-full bg-forest transition-[width] duration-500"
                  style={{ width: `${progress}%` }}
                />
              </div>
            </div>

            <ul className="flex-1 divide-y divide-line overflow-y-auto px-5 sm:px-6">
              {lines.map((line) => (
                <li key={line.product.id} className="flex gap-4 py-5">
                  <Link
                    to={`/shop/${line.product.slug}`}
                    onClick={close}
                    className="size-20 shrink-0 overflow-hidden rounded-2xl bg-sage/50"
                  >
                    <img
                      src={line.product.image}
                      alt={line.product.name}
                      width={160}
                      height={200}
                      className="size-full object-cover"
                    />
                  </Link>
                  <div className="flex min-w-0 flex-1 flex-col">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <Link
                          to={`/shop/${line.product.slug}`}
                          onClick={close}
                          className="text-[14px] font-semibold leading-snug tracking-[-0.02em] hover:text-forest"
                        >
                          {line.product.name}
                        </Link>
                        <p className="mt-0.5 text-xs text-muted">{line.product.size}</p>
                      </div>
                      <span className="tnum shrink-0 text-sm font-bold">
                        {formatPrice(line.product.price * line.quantity)}
                      </span>
                    </div>
                    <div className="mt-3 flex items-center justify-between">
                      <QuantityStepper
                        value={line.quantity}
                        onChange={(quantity) => setQuantity(line.product.id, quantity)}
                        max={line.product.stock}
                        size="sm"
                      />
                      <button
                        type="button"
                        onClick={() => remove(line.product.id)}
                        className="grid size-8 place-items-center rounded-full text-muted transition-colors hover:bg-sage hover:text-ink"
                        aria-label={`Remove ${line.product.name} from bag`}
                      >
                        <Trash2 className="size-4" strokeWidth={1.7} aria-hidden="true" />
                      </button>
                    </div>
                  </div>
                </li>
              ))}
            </ul>

            <div className="border-t border-line bg-paper px-5 py-5 sm:px-6">
              <div className="flex items-center justify-between text-sm">
                <span className="text-muted">Subtotal</span>
                <span className="tnum text-lg font-bold">{formatPrice(subtotal)}</span>
              </div>
              <p className="mt-1 text-[11px] text-muted">
                Taxes and delivery calculated at checkout.
              </p>
              <Link to="/checkout" onClick={close} className="btn btn-primary mt-4 w-full">
                Checkout
                <ArrowRight className="size-4" aria-hidden="true" />
              </Link>
              <Link to="/cart" onClick={close} className="btn btn-outline mt-2 w-full">
                View bag
              </Link>
            </div>
          </>
        )}
      </aside>
    </div>
  );
}
