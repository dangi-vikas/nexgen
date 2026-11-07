import { useState, type FormEvent } from 'react';
import { ArrowRight, Check, Lock, ShoppingBag, ShieldCheck, Trash2, Truck } from 'lucide-react';
import { Link } from 'react-router-dom';
import { PageMeta } from '@/components/ui/PageMeta';
import { QuantityStepper } from '@/components/ui/QuantityStepper';
import { EmptyState } from '@/components/ui/EmptyState';
import {
  FREE_SHIPPING_THRESHOLD,
  cartTotals,
  useCartStore,
  useCartSubtotal,
} from '@/store/cart';
import { formatPrice } from '@/lib/utils';

export function CartPage() {
  const lines = useCartStore((state) => state.lines);
  const setQuantity = useCartStore((state) => state.setQuantity);
  const remove = useCartStore((state) => state.remove);
  const promoCode = useCartStore((state) => state.promoCode);
  const applyPromo = useCartStore((state) => state.applyPromo);
  const removePromo = useCartStore((state) => state.removePromo);
  const subtotal = useCartSubtotal();
  const totals = cartTotals(subtotal, promoCode);
  const itemCount = lines.reduce((sum, line) => sum + line.quantity, 0);
  const remaining = Math.max(0, FREE_SHIPPING_THRESHOLD - subtotal);
  const progress = Math.min(100, (subtotal / FREE_SHIPPING_THRESHOLD) * 100);
  const [promo, setPromo] = useState('');
  const [promoError, setPromoError] = useState('');

  function handlePromo(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (applyPromo(promo)) {
      setPromoError('');
      return;
    }
    setPromoError('That code isn’t valid. Try WELCOME10.');
  }

  function clearPromo() {
    removePromo();
    setPromo('');
    setPromoError('');
  }

  return (
    <>
      <PageMeta
        title="Your bag — Northleaf Pharmacy"
        description="Review your Northleaf bag and continue to checkout."
      />

      <div className="container-page py-10 sm:py-14">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="eyebrow text-clay-dark">Your order</p>
            <h1 className="display mt-3 text-[2.5rem] sm:text-[3.2rem]">
              Your bag
              {itemCount > 0 ? (
                <span className="ml-3 align-middle text-base font-semibold tracking-normal text-muted">
                  ({itemCount} {itemCount === 1 ? 'item' : 'items'})
                </span>
              ) : null}
            </h1>
          </div>
          <Link to="/shop" className="link-quiet">
            Continue shopping
            <ArrowRight className="size-4" aria-hidden="true" />
          </Link>
        </div>

        {lines.length === 0 ? (
          <EmptyState
            icon={ShoppingBag}
            title="Your bag is empty"
            description="Start with a few everyday essentials, or upload a prescription and we’ll take it from there."
            action={{ label: 'Shop essentials', href: '/shop' }}
          />
        ) : (
          <div className="mt-8 grid items-start gap-10 lg:grid-cols-[1.45fr_0.75fr] lg:gap-14">
            <div>
              <div className="rounded-2xl border border-line bg-paper px-5 py-4">
                <p className="text-[13px] text-muted">
                  {remaining > 0 ? (
                    <>
                      You’re <span className="font-bold text-ink">{formatPrice(remaining)}</span>{' '}
                      away from free delivery.
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

              <ul className="mt-5 divide-y divide-line">
                {lines.map((line) => (
                  <li key={line.product.id} className="flex gap-4 py-6 sm:gap-5">
                    <Link
                      to={`/shop/${line.product.slug}`}
                      className="size-24 shrink-0 overflow-hidden rounded-2xl bg-sage/50 sm:size-28"
                    >
                      <img
                        src={line.product.image}
                        alt={line.product.name}
                        width={224}
                        height={280}
                        className="size-full object-cover"
                      />
                    </Link>
                    <div className="flex min-w-0 flex-1 flex-col">
                      <div className="flex items-start justify-between gap-4">
                        <div>
                          <p className="text-[10px] font-bold uppercase tracking-[0.14em] text-muted">
                            {line.product.category}
                          </p>
                          <h2 className="mt-1 text-[15px] font-semibold leading-snug tracking-[-0.02em] sm:text-base">
                            <Link
                              to={`/shop/${line.product.slug}`}
                              className="hover:text-forest"
                            >
                              {line.product.name}
                            </Link>
                          </h2>
                          <p className="mt-0.5 text-[12px] text-muted">
                            {line.product.size} · {formatPrice(line.product.price)} each
                          </p>
                        </div>
                        <span className="tnum text-[15px] font-bold">
                          {formatPrice(line.product.price * line.quantity)}
                        </span>
                      </div>
                      <div className="mt-auto flex items-center justify-between gap-3 pt-4">
                        <QuantityStepper
                          value={line.quantity}
                          onChange={(quantity) => setQuantity(line.product.id, quantity)}
                          max={line.product.stock}
                          size="sm"
                        />
                        <button
                          type="button"
                          onClick={() => remove(line.product.id)}
                          className="inline-flex items-center gap-1.5 text-[11.5px] font-semibold text-muted transition-colors hover:text-clay-dark"
                        >
                          <Trash2 className="size-3.5" aria-hidden="true" />
                          Remove
                        </button>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>

              <div className="mt-6 grid gap-3 rounded-2xl bg-sage/40 p-4 sm:grid-cols-3">
                {[
                  { icon: Truck, text: 'Free delivery over $35' },
                  { icon: ShieldCheck, text: 'Pharmacist checked' },
                  { icon: Lock, text: 'Secure checkout' },
                ].map(({ icon: Icon, text }) => (
                  <p key={text} className="flex items-center gap-2 text-[12px] font-medium text-ink/70">
                    <Icon className="size-4 text-forest" strokeWidth={1.7} aria-hidden="true" />
                    {text}
                  </p>
                ))}
              </div>
            </div>

            <aside className="lg:sticky lg:top-28">
              <div className="rounded-[26px] border border-line bg-paper p-5 sm:p-6">
                <h2 className="text-lg font-semibold tracking-[-0.03em]">Order summary</h2>

                <dl className="mt-5 space-y-3 text-[13.5px]">
                  <div className="flex justify-between">
                    <dt className="text-muted">Subtotal</dt>
                    <dd className="tnum font-semibold">{formatPrice(subtotal)}</dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-muted">Delivery</dt>
                    <dd className="tnum font-semibold">
                      {totals.shipping === 0 ? (
                        <span className="text-forest">Free</span>
                      ) : (
                        formatPrice(totals.shipping)
                      )}
                    </dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-muted">Estimated tax</dt>
                    <dd className="tnum font-semibold">{formatPrice(totals.tax)}</dd>
                  </div>
                  {promoCode ? (
                    <div className="flex justify-between text-forest">
                      <dt className="flex items-center gap-1.5">
                        <Check className="size-3.5" aria-hidden="true" />
                        {promoCode}
                      </dt>
                      <dd className="tnum font-semibold">−{formatPrice(totals.discount)}</dd>
                    </div>
                  ) : null}
                  <div className="flex items-baseline justify-between border-t border-line pt-3.5">
                    <dt className="text-[15px] font-semibold">Total</dt>
                    <dd className="tnum text-xl font-bold">{formatPrice(totals.total)}</dd>
                  </div>
                </dl>

                <form onSubmit={handlePromo} className="mt-5">
                  <label htmlFor="promo" className="text-[11px] font-bold uppercase tracking-[0.14em] text-muted">
                    Promo code
                  </label>
                  <div className="mt-2 flex gap-2">
                    <input
                      id="promo"
                      value={promo}
                      onChange={(event) => {
                        setPromo(event.target.value);
                        setPromoError('');
                      }}
                      placeholder="WELCOME10"
                      className="field h-10 min-h-0 flex-1 text-[13px] uppercase placeholder:normal-case"
                    />
                    <button type="submit" className="btn btn-outline h-10 min-h-0 px-4 text-[12px]">
                      Apply
                    </button>
                  </div>
                  {promoError ? (
                    <p className="mt-2 text-[11.5px] text-clay-dark" role="alert">
                      {promoError}
                    </p>
                  ) : null}
                  {promoCode ? (
                    <p className="mt-2 flex items-center justify-between gap-3 text-[11.5px] text-forest">
                      <span>10% off applied to your order.</span>
                      <button
                        type="button"
                        onClick={clearPromo}
                        className="font-semibold underline underline-offset-2 hover:text-clay-dark"
                      >
                        Remove
                      </button>
                    </p>
                  ) : null}
                </form>

                <Link to="/checkout" className="btn btn-primary mt-5 w-full">
                  Continue to checkout
                  <ArrowRight className="size-4" aria-hidden="true" />
                </Link>
                <p className="mt-3 text-center text-[11px] leading-relaxed text-muted">
                  Totals are confirmed by our pharmacy at checkout.
                </p>
              </div>
            </aside>
          </div>
        )}
      </div>
    </>
  );
}
