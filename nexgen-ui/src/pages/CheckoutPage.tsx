import { useState, type FormEvent, type ReactNode } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  Banknote,
  Check,
  CreditCard,
  Lock,
  MapPin,
  PackageCheck,
  Store,
  Truck,
  Wallet,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { PageMeta } from '@/components/ui/PageMeta';
import { EmptyState } from '@/components/ui/EmptyState';
import { Breadcrumbs } from '@/components/ui/Breadcrumbs';
import { cartTotals, useCartStore, useCartSubtotal } from '@/store/cart';
import { usePlacedOrders } from '@/store/orders';
import { formatPrice } from '@/lib/utils';
import { cn } from '@/lib/utils';
import type { Order } from '@/lib/types';

type DeliveryMethod = 'standard' | 'express' | 'pickup';
type PaymentMethod = 'card' | 'wallet' | 'pickup';

interface Details {
  email: string;
  firstName: string;
  lastName: string;
  address: string;
  city: string;
  state: string;
  zip: string;
}

const steps = ['Delivery', 'Payment', 'Review'];

function Field({
  label,
  id,
  children,
  className,
}: {
  label: string;
  id: string;
  children: ReactNode;
  className?: string;
}) {
  return (
    <div className={className}>
      <label htmlFor={id} className="mb-1.5 block text-[11px] font-bold uppercase tracking-[0.12em] text-muted">
        {label}
      </label>
      {children}
    </div>
  );
}

export function CheckoutPage() {
  const lines = useCartStore((state) => state.lines);
  const clear = useCartStore((state) => state.clear);
  const promoCode = useCartStore((state) => state.promoCode);
  const recordOrder = usePlacedOrders((state) => state.add);
  const subtotal = useCartSubtotal();
  const totals = cartTotals(subtotal, promoCode);

  const [step, setStep] = useState(0);
  const [delivery, setDelivery] = useState<DeliveryMethod>('standard');
  const [payment, setPayment] = useState<PaymentMethod>('card');
  const [error, setError] = useState('');
  const [terms, setTerms] = useState(false);
  const [orderNumber, setOrderNumber] = useState<string | null>(null);
  const [details, setDetails] = useState<Details>({
    email: '',
    firstName: '',
    lastName: '',
    address: '',
    city: '',
    state: '',
    zip: '',
  });
  const [card, setCard] = useState({ number: '', expiry: '', cvc: '', name: '' });

  const shipping =
    delivery === 'express' ? 9.95 : delivery === 'pickup' ? 0 : totals.shipping;
  const total = Math.max(
    0,
    Math.round((subtotal + shipping + totals.tax - totals.discount) * 100) / 100,
  );

  function updateDetail(key: keyof Details, value: string) {
    setDetails((current) => ({ ...current, [key]: value }));
    setError('');
  }

  function continueFromDelivery(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const pickup = delivery === 'pickup';
    if (
      !details.email ||
      !details.firstName ||
      !details.lastName ||
      (!pickup && (!details.address || !details.city || !details.state || !details.zip))
    ) {
      setError(
        pickup
          ? 'Please add your contact details so we can let you know when it’s ready.'
          : 'Please complete every field so we can deliver your order.',
      );
      return;
    }
    setError('');
    setStep(1);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function continueFromPayment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (payment === 'card' && (!card.number || !card.expiry || !card.cvc || !card.name)) {
      setError('Please complete your card details.');
      return;
    }
    setError('');
    setStep(2);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function placeOrder() {
    if (!terms) {
      setError('Please accept the terms before placing your order.');
      return;
    }
    setError('');
    const number = `NL-${new Date().toISOString().slice(2, 10).replace(/-/g, '')}-${Math.floor(1000 + Math.random() * 9000)}`;
    const order: Order = {
      id: `order-${Date.now()}`,
      number,
      placedAt: new Date().toISOString(),
      status: 'Processing',
      total,
      lines: lines.map((line) => ({
        productId: line.product.id,
        name: line.product.name,
        size: line.product.size,
        price: line.product.price,
        quantity: line.quantity,
        image: line.product.image,
      })),
    };
    recordOrder(order);
    setOrderNumber(number);
    clear();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  if (orderNumber) {
    return (
      <>
        <PageMeta title="Order confirmed — NexGen Pharmacy" />
        <div className="container-page py-14 sm:py-20">
          <div className="mx-auto max-w-xl text-center">
            <span className="mx-auto grid size-16 place-items-center rounded-full bg-sage text-forest">
              <Check className="size-7" strokeWidth={2} aria-hidden="true" />
            </span>
            <p className="eyebrow mt-6 justify-center text-clay-dark">Order confirmed</p>
            <h1 className="display mt-3 text-[2.4rem] sm:text-[3rem]">
              Thank you, <span className="serif text-forest">{details.firstName || 'friend'}.</span>
            </h1>
            <p className="mt-4 text-[15px] leading-relaxed text-muted">
              Your order <span className="font-semibold text-ink">{orderNumber}</span> is with our
              pharmacy team. We’ll email {details.email || 'you'} when it’s on its way.
            </p>
            <div className="mt-7 rounded-2xl border border-line bg-paper p-5 text-left">
              <div className="flex items-start gap-3">
                {delivery === 'pickup' ? (
                  <Store className="mt-0.5 size-5 text-forest" aria-hidden="true" />
                ) : (
                  <Truck className="mt-0.5 size-5 text-forest" aria-hidden="true" />
                )}
                <div>
                  <p className="text-sm font-semibold">
                    {delivery === 'pickup'
                      ? 'Ready for pickup today'
                      : delivery === 'express'
                        ? 'Arriving tomorrow'
                        : 'Arriving in 3–5 working days'}
                  </p>
                  <p className="mt-1 text-[12.5px] text-muted">
                    {delivery === 'pickup'
                      ? '48 Alder Street, Suite 2 · Bring your order number.'
                      : 'A plain outer box, with tracking sent by email.'}
                  </p>
                </div>
              </div>
            </div>
            <div className="mt-7 flex flex-wrap justify-center gap-3">
              <Link to="/account" className="btn btn-primary">
                View your orders
                <ArrowRight className="size-4" aria-hidden="true" />
              </Link>
              <Link to="/shop" className="btn btn-outline">
                Continue shopping
              </Link>
            </div>
          </div>
        </div>
      </>
    );
  }

  if (lines.length === 0) {
    return (
      <div className="container-page py-16">
        <EmptyState
          icon={PackageCheck}
          title="There’s nothing to check out"
          description="Add a few essentials to your bag and come back when you’re ready."
          action={{ label: 'Shop essentials', href: '/shop' }}
        />
      </div>
    );
  }

  return (
    <>
      <PageMeta
        title="Checkout — NexGen Pharmacy"
        description="Secure checkout with delivery, pickup and pharmacist support."
      />

      <div className="container-page py-8 sm:py-10">
        <Breadcrumbs items={[{ label: 'Bag', href: '/cart' }, { label: 'Checkout' }]} />
        <div className="mt-6 max-w-2xl">
          <p className="eyebrow text-clay-dark">Secure checkout</p>
          <h1 className="display mt-3 text-[2.4rem] sm:text-[3rem]">
            Almost <span className="serif text-forest">there.</span>
          </h1>
        </div>

        <ol className="mt-7 flex items-center gap-2 sm:gap-4" aria-label="Checkout steps">
          {steps.map((label, index) => {
            const complete = index < step;
            const current = index === step;
            return (
              <li key={label} className="flex flex-1 items-center gap-2 sm:gap-3">
                <span
                  className={cn(
                    'grid size-7 shrink-0 place-items-center rounded-full text-[11px] font-bold',
                    complete && 'bg-forest text-cream',
                    current && 'bg-clay text-ink',
                    !complete && !current && 'border border-line text-muted',
                  )}
                  aria-current={current ? 'step' : undefined}
                >
                  {complete ? <Check className="size-3.5" aria-hidden="true" /> : index + 1}
                </span>
                <span
                  className={cn(
                    'text-[12px] font-semibold',
                    current ? 'text-ink' : 'text-muted',
                  )}
                >
                  {label}
                </span>
                {index < steps.length - 1 ? (
                  <span className="h-px flex-1 bg-line" aria-hidden="true" />
                ) : null}
              </li>
            );
          })}
        </ol>

        <div className="mt-8 grid items-start gap-10 lg:grid-cols-[1.35fr_0.75fr] lg:gap-14">
          <div>
            {step === 0 ? (
              <form onSubmit={continueFromDelivery} className="rounded-[26px] border border-line bg-paper p-5 sm:p-7">
                <h2 className="text-lg font-semibold tracking-[-0.03em]">Contact & delivery</h2>
                <div className="mt-5 grid gap-4 sm:grid-cols-2">
                  <Field label="Email address" id="email" className="sm:col-span-2">
                    <input
                      id="email"
                      type="email"
                      autoComplete="email"
                      value={details.email}
                      onChange={(event) => updateDetail('email', event.target.value)}
                      className="field"
                      placeholder="you@example.com"
                    />
                  </Field>
                  <Field label="First name" id="firstName">
                    <input
                      id="firstName"
                      autoComplete="given-name"
                      value={details.firstName}
                      onChange={(event) => updateDetail('firstName', event.target.value)}
                      className="field"
                    />
                  </Field>
                  <Field label="Last name" id="lastName">
                    <input
                      id="lastName"
                      autoComplete="family-name"
                      value={details.lastName}
                      onChange={(event) => updateDetail('lastName', event.target.value)}
                      className="field"
                    />
                  </Field>
                  <Field label="Street address" id="address" className="sm:col-span-2">
                    <input
                      id="address"
                      autoComplete="street-address"
                      value={details.address}
                      onChange={(event) => updateDetail('address', event.target.value)}
                      className="field"
                      placeholder="48 Alder Street, Apt 2"
                      disabled={delivery === 'pickup'}
                    />
                  </Field>
                  <Field label="City" id="city">
                    <input
                      id="city"
                      autoComplete="address-level2"
                      value={details.city}
                      onChange={(event) => updateDetail('city', event.target.value)}
                      className="field"
                      disabled={delivery === 'pickup'}
                    />
                  </Field>
                  <div className="grid grid-cols-2 gap-3">
                    <Field label="State" id="state">
                      <input
                        id="state"
                        autoComplete="address-level1"
                        value={details.state}
                        onChange={(event) => updateDetail('state', event.target.value)}
                        className="field"
                        placeholder="CA"
                        disabled={delivery === 'pickup'}
                      />
                    </Field>
                    <Field label="ZIP" id="zip">
                      <input
                        id="zip"
                        autoComplete="postal-code"
                        value={details.zip}
                        onChange={(event) => updateDetail('zip', event.target.value)}
                        className="field"
                        placeholder="94110"
                        disabled={delivery === 'pickup'}
                      />
                    </Field>
                  </div>
                </div>

                <fieldset className="mt-6">
                  <legend className="text-[11px] font-bold uppercase tracking-[0.12em] text-muted">
                    Delivery method
                  </legend>
                  <div className="mt-2.5 space-y-2.5">
                    {[
                      {
                        id: 'standard' as const,
                        icon: Truck,
                        title: 'Standard delivery',
                        copy: '3–5 working days',
                        price: totals.shipping === 0 ? 'Free' : formatPrice(totals.shipping),
                      },
                      {
                        id: 'express' as const,
                        icon: PackageCheck,
                        title: 'Express delivery',
                        copy: 'Tomorrow if ordered before 2pm',
                        price: '$9.95',
                      },
                      {
                        id: 'pickup' as const,
                        icon: Store,
                        title: 'Collect in store',
                        copy: '48 Alder Street · today after 4pm',
                        price: 'Free',
                      },
                    ].map((option) => {
                      const Icon = option.icon;
                      return (
                        <label
                          key={option.id}
                          className={cn(
                            'flex cursor-pointer items-center gap-3 rounded-xl border p-3.5 transition-colors',
                            delivery === option.id
                              ? 'border-forest bg-sage/35'
                              : 'border-line hover:bg-sage/20',
                          )}
                        >
                          <input
                            type="radio"
                            name="delivery"
                            value={option.id}
                            checked={delivery === option.id}
                            onChange={() => {
                              setDelivery(option.id);
                              setError('');
                            }}
                            className="size-4 accent-forest"
                          />
                          <Icon className="size-4 text-forest" strokeWidth={1.7} aria-hidden="true" />
                          <span className="min-w-0 flex-1">
                            <span className="block text-[13px] font-semibold">{option.title}</span>
                            <span className="block text-[11.5px] text-muted">{option.copy}</span>
                          </span>
                          <span className="tnum text-[12.5px] font-semibold">{option.price}</span>
                        </label>
                      );
                    })}
                  </div>
                </fieldset>

                {error ? (
                  <p className="mt-4 text-[12.5px] font-medium text-clay-dark" role="alert">
                    {error}
                  </p>
                ) : null}

                <button type="submit" className="btn btn-primary mt-5 w-full sm:w-auto">
                  Continue to payment
                  <ArrowRight className="size-4" aria-hidden="true" />
                </button>
              </form>
            ) : null}

            {step === 1 ? (
              <form onSubmit={continueFromPayment} className="rounded-[26px] border border-line bg-paper p-5 sm:p-7">
                <h2 className="text-lg font-semibold tracking-[-0.03em]">Payment</h2>
                <p className="mt-1 text-[12px] text-muted">
                  This is a demo checkout. No payment will be taken.
                </p>

                <fieldset className="mt-5">
                  <legend className="sr-only">Payment method</legend>
                  <div className="grid gap-2.5 sm:grid-cols-3">
                    {[
                      { id: 'card' as const, icon: CreditCard, label: 'Card' },
                      { id: 'wallet' as const, icon: Wallet, label: 'Wallet' },
                      { id: 'pickup' as const, icon: Banknote, label: 'Pay on pickup' },
                    ].map((option) => {
                      const Icon = option.icon;
                      return (
                        <label
                          key={option.id}
                          className={cn(
                            'flex cursor-pointer items-center gap-2 rounded-xl border px-3.5 py-3 text-[13px] font-semibold transition-colors',
                            payment === option.id
                              ? 'border-forest bg-sage/35'
                              : 'border-line hover:bg-sage/20',
                          )}
                        >
                          <input
                            type="radio"
                            name="payment"
                            value={option.id}
                            checked={payment === option.id}
                            onChange={() => {
                              setPayment(option.id);
                              setError('');
                            }}
                            className="size-4 accent-forest"
                          />
                          <Icon className="size-4 text-forest" strokeWidth={1.7} aria-hidden="true" />
                          {option.label}
                        </label>
                      );
                    })}
                  </div>
                </fieldset>

                {payment === 'card' ? (
                  <div className="mt-5 grid gap-4 sm:grid-cols-2">
                    <Field label="Card number" id="cardNumber" className="sm:col-span-2">
                      <input
                        id="cardNumber"
                        inputMode="numeric"
                        autoComplete="cc-number"
                        value={card.number}
                        onChange={(event) => {
                          setCard((current) => ({ ...current, number: event.target.value }));
                          setError('');
                        }}
                        className="field"
                        placeholder="4242 4242 4242 4242"
                      />
                    </Field>
                    <Field label="Name on card" id="cardName" className="sm:col-span-2">
                      <input
                        id="cardName"
                        autoComplete="cc-name"
                        value={card.name}
                        onChange={(event) => {
                          setCard((current) => ({ ...current, name: event.target.value }));
                          setError('');
                        }}
                        className="field"
                      />
                    </Field>
                    <Field label="Expiry" id="expiry">
                      <input
                        id="expiry"
                        autoComplete="cc-exp"
                        value={card.expiry}
                        onChange={(event) => {
                          setCard((current) => ({ ...current, expiry: event.target.value }));
                          setError('');
                        }}
                        className="field"
                        placeholder="MM / YY"
                      />
                    </Field>
                    <Field label="CVC" id="cvc">
                      <input
                        id="cvc"
                        inputMode="numeric"
                        autoComplete="cc-csc"
                        value={card.cvc}
                        onChange={(event) => {
                          setCard((current) => ({ ...current, cvc: event.target.value }));
                          setError('');
                        }}
                        className="field"
                        placeholder="123"
                      />
                    </Field>
                  </div>
                ) : (
                  <div className="mt-5 rounded-xl bg-sage/40 p-4 text-[12.5px] leading-relaxed text-ink/70">
                    {payment === 'wallet'
                      ? 'You’ll be asked to confirm payment with your wallet after reviewing the order.'
                      : 'Pay at the counter when you collect. We’ll hold your order for 48 hours.'}
                  </div>
                )}

                {error ? (
                  <p className="mt-4 text-[12.5px] font-medium text-clay-dark" role="alert">
                    {error}
                  </p>
                ) : null}

                <div className="mt-5 flex flex-wrap items-center gap-3">
                  <button type="button" onClick={() => setStep(0)} className="btn btn-outline">
                    <ArrowLeft className="size-4" aria-hidden="true" />
                    Back
                  </button>
                  <button type="submit" className="btn btn-primary">
                    Review order
                    <ArrowRight className="size-4" aria-hidden="true" />
                  </button>
                </div>
              </form>
            ) : null}

            {step === 2 ? (
              <div className="space-y-4">
                <div className="rounded-[26px] border border-line bg-paper p-5 sm:p-7">
                  <div className="flex items-center justify-between">
                    <h2 className="text-lg font-semibold tracking-[-0.03em]">Review</h2>
                    <button
                      type="button"
                      onClick={() => setStep(0)}
                      className="text-[12px] font-semibold text-forest hover:text-clay-dark"
                    >
                      Edit
                    </button>
                  </div>
                  <div className="mt-4 grid gap-5 sm:grid-cols-2">
                    <div>
                      <p className="text-[10px] font-bold uppercase tracking-[0.14em] text-muted">
                        Deliver to
                      </p>
                      <p className="mt-1.5 text-[13.5px] font-semibold">
                        {details.firstName} {details.lastName}
                      </p>
                      <p className="mt-0.5 text-[12.5px] leading-relaxed text-muted">
                        {delivery === 'pickup'
                          ? 'Collect at 48 Alder Street, Suite 2'
                          : (
                              <>
                                {details.address}
                                <br />
                                {details.city}, {details.state} {details.zip}
                              </>
                            )}
                      </p>
                    </div>
                    <div>
                      <p className="text-[10px] font-bold uppercase tracking-[0.14em] text-muted">
                        Payment
                      </p>
                      <p className="mt-1.5 text-[13.5px] font-semibold">
                        {payment === 'card'
                          ? `Card ending ${card.number.replace(/\s/g, '').slice(-4) || '••••'}`
                          : payment === 'wallet'
                            ? 'Digital wallet'
                            : 'Pay on pickup'}
                      </p>
                      <p className="mt-0.5 text-[12.5px] text-muted">{details.email}</p>
                    </div>
                  </div>
                </div>

                <div className="rounded-[26px] border border-line bg-paper p-5 sm:p-7">
                  <h2 className="text-lg font-semibold tracking-[-0.03em]">Before you go</h2>
                  <label className="mt-4 flex cursor-pointer items-start gap-2.5 text-[12.5px] leading-relaxed text-muted">
                    <input
                      type="checkbox"
                      checked={terms}
                      onChange={(event) => {
                        setTerms(event.target.checked);
                        setError('');
                      }}
                      className="mt-0.5 size-4 rounded accent-forest"
                    />
                    I confirm the details above and accept the terms of sale and privacy policy.
                  </label>
                  {error ? (
                    <p className="mt-3 text-[12.5px] font-medium text-clay-dark" role="alert">
                      {error}
                    </p>
                  ) : null}
                  <div className="mt-5 flex flex-wrap items-center gap-3">
                    <button type="button" onClick={() => setStep(1)} className="btn btn-outline">
                      <ArrowLeft className="size-4" aria-hidden="true" />
                      Back
                    </button>
                    <button type="button" onClick={placeOrder} className="btn btn-primary">
                      Place order · {formatPrice(total)}
                      <Lock className="size-4" aria-hidden="true" />
                    </button>
                  </div>
                </div>
              </div>
            ) : null}
          </div>

          <aside className="lg:sticky lg:top-28">
            <div className="rounded-[26px] border border-line bg-paper p-5 sm:p-6">
              <h2 className="text-lg font-semibold tracking-[-0.03em]">Order summary</h2>
              <ul className="mt-4 space-y-3">
                {lines.map((line) => (
                  <li key={line.product.id} className="flex items-center gap-3">
                    <span className="size-12 shrink-0 overflow-hidden rounded-lg bg-sage/50">
                      <img
                        src={line.product.image}
                        alt=""
                        width={96}
                        height={120}
                        className="size-full object-cover"
                      />
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-[12.5px] font-semibold">
                        {line.product.name}
                      </span>
                      <span className="block text-[11px] text-muted">
                        Qty {line.quantity} · {line.product.size}
                      </span>
                    </span>
                    <span className="tnum text-[12.5px] font-semibold">
                      {formatPrice(line.product.price * line.quantity)}
                    </span>
                  </li>
                ))}
              </ul>

              <dl className="mt-4 space-y-2.5 border-t border-line pt-4 text-[13px]">
                <div className="flex justify-between">
                  <dt className="text-muted">Subtotal</dt>
                  <dd className="tnum font-semibold">{formatPrice(subtotal)}</dd>
                </div>
                <div className="flex justify-between">
                  <dt className="text-muted">Delivery</dt>
                  <dd className="tnum font-semibold">
                    {shipping === 0 ? <span className="text-forest">Free</span> : formatPrice(shipping)}
                  </dd>
                </div>
                <div className="flex justify-between">
                  <dt className="text-muted">Estimated tax</dt>
                  <dd className="tnum font-semibold">{formatPrice(totals.tax)}</dd>
                </div>
                {totals.discount > 0 ? (
                  <div className="flex justify-between text-forest">
                    <dt>{promoCode}</dt>
                    <dd className="tnum font-semibold">−{formatPrice(totals.discount)}</dd>
                  </div>
                ) : null}
                <div className="flex items-baseline justify-between border-t border-line pt-3">
                  <dt className="text-[14px] font-semibold">Total</dt>
                  <dd className="tnum text-xl font-bold">{formatPrice(total)}</dd>
                </div>
              </dl>

              <div className="mt-4 flex items-center gap-2 rounded-lg bg-sage/40 px-3 py-2.5 text-[11px] text-muted">
                <MapPin className="size-3.5 shrink-0 text-forest" aria-hidden="true" />
                Fulfilled by NexGen Pharmacy, San Francisco
              </div>
            </div>
          </aside>
        </div>
      </div>
    </>
  );
}
