import { useMemo, useState } from 'react';
import {
  ArrowRight,
  CalendarDays,
  Check,
  ChevronDown,
  MapPin,
  Package,
  Pill,
  Plus,
  RefreshCw,
  Truck,
  UserRound,
} from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { PageMeta } from '@/components/ui/PageMeta';
import { PageBotanicals } from '@/components/ui/Botanical';
import { Reveal } from '@/components/ui/Reveal';
import { useUiStore } from '@/store/ui';
import { currentUser } from '@/lib/mock';
import { useOrders, usePrescriptions, useProducts } from '@/lib/queries';
import { formatDate, formatPrice } from '@/lib/utils';
import { useCartStore } from '@/store/cart';
import { cn } from '@/lib/utils';
import type { Order } from '@/lib/types';

const tabs = [
  { id: 'overview', label: 'Overview', href: '/account' },
  { id: 'orders', label: 'Orders', href: '/account/orders' },
  { id: 'prescriptions', label: 'Prescriptions', href: '/account/prescriptions' },
  { id: 'details', label: 'Details', href: '/account/details' },
];

const statusStyles: Record<Order['status'], string> = {
  Processing: 'bg-clay-soft/50 text-clay-dark',
  Packed: 'bg-sage text-forest',
  'Out for delivery': 'bg-forest text-cream',
  Delivered: 'bg-sage/60 text-forest',
};

function StatusPill({ status }: { status: Order['status'] }) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[10px] font-bold uppercase tracking-[0.1em]',
        statusStyles[status],
      )}
    >
      <span className="size-1.5 rounded-full bg-current" aria-hidden="true" />
      {status}
    </span>
  );
}

function Overview() {
  const add = useCartStore((state) => state.add);
  const showToast = useUiStore((state) => state.showToast);
  const orders = useOrders();
  const prescriptions = usePrescriptions();
  const products = useProducts();
  const latest = useMemo(
    () =>
      [...orders].sort(
        (a, b) => new Date(b.placedAt).getTime() - new Date(a.placedAt).getTime(),
      )[0],
    [orders],
  );
  const readyRx = prescriptions.find((item) => item.status === 'Ready for pickup');

  if (!latest) {
    return (
      <div className="rounded-[26px] border border-line bg-paper p-7 text-center">
        <p className="text-lg font-semibold tracking-[-0.03em]">No orders yet</p>
        <p className="mt-2 text-[13px] text-muted">
          When you place an order, it will appear here with tracking and a reorder shortcut.
        </p>
        <Link to="/shop" className="btn btn-primary mt-5">
          Browse the shop
          <ArrowRight className="size-4" aria-hidden="true" />
        </Link>
      </div>
    );
  }

  function buyAgain(order: Order) {
    order.lines.forEach((line) => {
      const product = products.find((item) => item.id === line.productId);
      if (product) add(product, line.quantity);
    });
    showToast({ title: 'Added to your bag', detail: `${order.lines.length} items from ${order.number}` });
  }

  return (
    <div className="space-y-5">
      <div className="grid gap-5 md:grid-cols-2">
        <div className="rounded-[26px] border border-line bg-paper p-5 sm:p-6">
          <div className="flex items-center justify-between">
            <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-muted">
              Latest order
            </p>
            <StatusPill status={latest.status} />
          </div>
          <p className="mt-3 text-lg font-semibold tracking-[-0.03em]">{latest.number}</p>
          <p className="mt-1 text-[12.5px] text-muted">
            Placed {formatDate(latest.placedAt)} · {formatPrice(latest.total)}
          </p>
          <div className="mt-4 flex -space-x-2">
            {latest.lines.map((line) => (
              <span
                key={line.productId}
                className="size-11 overflow-hidden rounded-full border-2 border-paper bg-sage/50"
              >
                <img src={line.image} alt="" width={88} height={110} className="size-full object-cover" />
              </span>
            ))}
          </div>
          <div className="mt-5 flex items-center gap-4">
            <Link to="/account/orders" className="link-quiet text-[12.5px]">
              Track order
              <ArrowRight className="size-3.5" aria-hidden="true" />
            </Link>
            <button
              type="button"
              onClick={() => buyAgain(latest)}
              className="text-[12.5px] font-bold text-forest hover:text-clay-dark"
            >
              Buy again
            </button>
          </div>
        </div>

        <div className="rounded-[26px] border border-line bg-forest p-5 text-cream sm:p-6">
          <div className="flex items-center justify-between">
            <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-cream/70">
              Prescription
            </p>
            <Pill className="size-4 text-sage" strokeWidth={1.7} aria-hidden="true" />
          </div>
          {readyRx ? (
            <>
              <p className="mt-3 text-lg font-semibold tracking-[-0.03em]">{readyRx.medication}</p>
              <p className="mt-1 text-[12.5px] text-cream/60">
                {readyRx.refillsLeft} refills left · ready for pickup
              </p>
              <div className="mt-4 flex items-start gap-2 rounded-xl bg-cream/10 px-3 py-2.5 text-[11.5px] text-cream/75">
                <MapPin className="mt-0.5 size-3.5 shrink-0 text-clay-soft" aria-hidden="true" />
                48 Alder Street · open until 8pm
              </div>
              <Link to="/account/prescriptions" className="mt-4 inline-flex items-center gap-1.5 text-[12.5px] font-bold text-clay-soft hover:text-cream">
                View prescription
                <ArrowRight className="size-3.5" aria-hidden="true" />
              </Link>
            </>
          ) : (
            <>
              <p className="mt-3 text-lg font-semibold tracking-[-0.03em]">No active prescriptions</p>
              <p className="mt-1 text-[12.5px] text-cream/60">
                Upload a prescription and our pharmacists will take it from there.
              </p>
              <Link to="/prescriptions" className="btn btn-clay mt-4">
                Upload a prescription
              </Link>
            </>
          )}
        </div>
      </div>

      <div className="grid gap-5 md:grid-cols-3">
        {[
          {
            icon: Package,
            title: 'Free delivery',
            copy: 'On every order over $35, discreetly packed.',
            href: '/help#delivery',
            link: 'How delivery works',
          },
          {
            icon: CalendarDays,
            title: 'Book a review',
            copy: 'Twenty minutes with a pharmacist, at no cost.',
            href: '/pharmacists',
            link: 'Find a time',
          },
          {
            icon: RefreshCw,
            title: 'Refill reminders',
            copy: 'A gentle nudge before you run out.',
            href: '/account/prescriptions',
            link: 'Manage refills',
          },
        ].map(({ icon: Icon, title, copy, href, link }) => (
          <div key={title} className="rounded-[26px] border border-line bg-paper p-5">
            <Icon className="size-5 text-forest" strokeWidth={1.6} aria-hidden="true" />
            <p className="mt-3 text-[14px] font-semibold tracking-[-0.01em]">{title}</p>
            <p className="mt-1 text-[12px] leading-relaxed text-muted">{copy}</p>
            <Link to={href} className="mt-4 inline-flex text-[12px] font-bold text-forest hover:text-clay-dark">
              {link}
            </Link>
          </div>
        ))}
      </div>

      <div className="flex flex-col gap-4 rounded-[26px] border border-line bg-paper p-5 sm:flex-row sm:items-center sm:justify-between sm:p-6">
        <div className="flex items-start gap-3">
          <span className="grid size-11 place-items-center rounded-full bg-sage text-forest">
            <UserRound className="size-5" strokeWidth={1.6} aria-hidden="true" />
          </span>
          <div>
            <p className="text-[14px] font-semibold">Your pharmacist, Amara</p>
            <p className="mt-0.5 text-[12px] text-muted">Questions about a medicine? Ask directly.</p>
          </div>
        </div>
        <Link to="/pharmacists" className="btn btn-outline shrink-0">
          Book a call
        </Link>
      </div>
    </div>
  );
}

function OrdersView() {
  const orders = useOrders();
  const products = useProducts();
  const sortedOrders = useMemo(
    () =>
      [...orders].sort(
        (a, b) => new Date(b.placedAt).getTime() - new Date(a.placedAt).getTime(),
      ),
    [orders],
  );
  const [expanded, setExpanded] = useState<string | null>(sortedOrders[0]?.id ?? null);
  const add = useCartStore((state) => state.add);
  const showToast = useUiStore((state) => state.showToast);

  function buyAgain(order: Order) {
    order.lines.forEach((line) => {
      const product = products.find((item) => item.id === line.productId);
      if (product) add(product, line.quantity);
    });
    showToast({ title: 'Added to your bag', detail: `${order.lines.length} items from ${order.number}` });
  }

  return (
    <div className="space-y-4">
      {sortedOrders.map((order) => {
        const isOpen = expanded === order.id;
        return (
          <div key={order.id} className="overflow-hidden rounded-[26px] border border-line bg-paper">
            <div className="flex flex-wrap items-center justify-between gap-4 border-b border-line px-5 py-4 sm:px-6">
              <div>
                <div className="flex flex-wrap items-center gap-2.5">
                  <p className="text-[14px] font-semibold">{order.number}</p>
                  <StatusPill status={order.status} />
                </div>
                <p className="mt-1 text-[11.5px] text-muted">
                  Placed {formatDate(order.placedAt)} · {formatPrice(order.total)}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setExpanded(isOpen ? null : order.id)}
                className="inline-flex items-center gap-1.5 text-[12px] font-bold text-forest"
                aria-expanded={isOpen}
              >
                {isOpen ? 'Hide details' : 'View details'}
                <ChevronDown
                  className={cn('size-4 transition-transform', isOpen && 'rotate-180')}
                  aria-hidden="true"
                />
              </button>
            </div>

            <div className="flex flex-wrap items-center gap-3 px-5 py-4 sm:px-6">
              <div className="flex -space-x-2">
                {order.lines.map((line) => (
                  <span
                    key={line.productId}
                    className="size-12 overflow-hidden rounded-full border-2 border-paper bg-sage/50"
                  >
                    <img src={line.image} alt="" width={96} height={120} className="size-full object-cover" />
                  </span>
                ))}
              </div>
              <p className="text-[12.5px] text-muted">
                {order.lines.map((line) => line.name).join(', ')}
              </p>
              <div className="ml-auto flex items-center gap-3">
                <button
                  type="button"
                  onClick={() => buyAgain(order)}
                  className="text-[12px] font-bold text-forest hover:text-clay-dark"
                >
                  Buy again
                </button>
                <Link to="/help#delivery" className="text-[12px] font-semibold text-muted hover:text-ink">
                  Get help
                </Link>
              </div>
            </div>

            {isOpen ? (
              <div className="border-t border-line bg-cream/60 px-5 py-5 sm:px-6">
                <div className="grid gap-6 sm:grid-cols-[1.4fr_0.6fr]">
                  <ul className="space-y-3">
                    {order.lines.map((line) => (
                      <li key={line.productId} className="flex items-center gap-3">
                        <span className="size-14 shrink-0 overflow-hidden rounded-xl bg-sage/50">
                          <img src={line.image} alt="" width={112} height={140} className="size-full object-cover" />
                        </span>
                        <span className="min-w-0 flex-1">
                          <span className="block text-[13px] font-semibold">{line.name}</span>
                          <span className="block text-[11.5px] text-muted">
                            {line.size} · Qty {line.quantity}
                          </span>
                        </span>
                        <span className="tnum text-[13px] font-semibold">{formatPrice(line.price)}</span>
                      </li>
                    ))}
                  </ul>
                  <div className="text-[12.5px] text-muted">
                    <p className="flex items-center gap-1.5 font-semibold text-ink">
                      <Truck className="size-4 text-forest" aria-hidden="true" />
                      {order.status === 'Delivered' ? 'Delivered' : 'On its way'}
                    </p>
                    <p className="mt-1.5">
                      {order.tracking ? `Tracking ${order.tracking}` : 'Tracking will appear here.'}
                    </p>
                    <p className="mt-4 text-[11px] leading-relaxed">
                      Questions about this order? Our pharmacy team can help seven days a week.
                    </p>
                    <Link to="/help" className="link-quiet mt-3 text-[12px]">
                      Contact us
                      <ArrowRight className="size-3.5" aria-hidden="true" />
                    </Link>
                  </div>
                </div>
              </div>
            ) : null}
          </div>
        );
      })}
    </div>
  );
}

function PrescriptionsView() {
  const showToast = useUiStore((state) => state.showToast);
  const prescriptions = usePrescriptions();

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-[26px] border border-line bg-paper p-5">
        <div className="flex items-start gap-3">
          <span className="grid size-11 place-items-center rounded-full bg-sage text-forest">
            <Pill className="size-5" strokeWidth={1.6} aria-hidden="true" />
          </span>
          <div>
            <p className="text-[14px] font-semibold">Have a new prescription?</p>
            <p className="mt-0.5 text-[12px] text-muted">
              Upload a photo or ask your prescriber to send it directly.
            </p>
          </div>
        </div>
        <Link to="/prescriptions" className="btn btn-primary shrink-0">
          <Plus className="size-4" aria-hidden="true" />
          Upload
        </Link>
      </div>

      {prescriptions.map((item) => (
        <div key={item.id} className="rounded-[26px] border border-line bg-paper p-5 sm:p-6">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p className="text-[15px] font-semibold tracking-[-0.02em]">{item.medication}</p>
              <p className="mt-1 text-[12px] text-muted">
                Prescribed by {item.prescriber} · submitted {formatDate(item.submittedAt)}
              </p>
            </div>
            <span
              className={cn(
                'rounded-full px-2.5 py-1 text-[10px] font-bold uppercase tracking-[0.1em]',
                item.status === 'Ready for pickup'
                  ? 'bg-clay-soft/50 text-clay-dark'
                  : item.status === 'Completed'
                    ? 'bg-sage/60 text-forest'
                    : 'bg-sage text-forest',
              )}
            >
              {item.status}
            </span>
          </div>
          <div className="mt-4 flex flex-wrap items-center gap-3 border-t border-line pt-4 text-[12px] text-muted">
            <span className="flex items-center gap-1.5">
              <RefreshCw className="size-3.5 text-forest" aria-hidden="true" />
              {item.refillsLeft} refills remaining
            </span>
            {item.status === 'Ready for pickup' ? (
              <span className="flex items-center gap-1.5">
                <Check className="size-3.5 text-forest" aria-hidden="true" />
                Ready at 48 Alder Street
              </span>
            ) : null}
            <button
              type="button"
              onClick={() =>
                showToast({
                  title: 'Refill requested',
                  detail: `${item.medication} · our pharmacy will confirm by email`,
                })
              }
              className="ml-auto rounded-full border border-line px-3.5 py-2 text-[11.5px] font-bold text-forest transition-colors hover:bg-sage/40"
            >
              Request refill
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}

function DetailsView() {
  const showToast = useUiStore((state) => state.showToast);
  const [form, setForm] = useState({
    name: currentUser.name,
    email: currentUser.email,
    phone: currentUser.phone ?? '',
    notes: '',
  });

  return (
    <div className="rounded-[26px] border border-line bg-paper p-5 sm:p-7">
      <h2 className="text-lg font-semibold tracking-[-0.03em]">Your details</h2>
      <p className="mt-1 text-[12px] text-muted">
        These are used for prescriptions, delivery and pharmacist callbacks.
      </p>
      <form
        className="mt-5 grid gap-4 sm:grid-cols-2"
        onSubmit={(event) => {
          event.preventDefault();
          showToast({ title: 'Details saved', detail: 'Your pharmacy record is up to date.' });
        }}
      >
        {[
          { id: 'name', label: 'Full name', key: 'name' as const, type: 'text' },
          { id: 'email', label: 'Email', key: 'email' as const, type: 'email' },
          { id: 'phone', label: 'Phone', key: 'phone' as const, type: 'tel' },
        ].map((field) => (
          <div key={field.id} className={field.id === 'name' ? 'sm:col-span-1' : ''}>
            <label
              htmlFor={field.id}
              className="mb-1.5 block text-[11px] font-bold uppercase tracking-[0.12em] text-muted"
            >
              {field.label}
            </label>
            <input
              id={field.id}
              type={field.type}
              value={form[field.key]}
              onChange={(event) => setForm((current) => ({ ...current, [field.key]: event.target.value }))}
              className="field"
            />
          </div>
        ))}
        <div>
          <label
            htmlFor="notes"
            className="mb-1.5 block text-[11px] font-bold uppercase tracking-[0.12em] text-muted"
          >
            Notes for the pharmacy
          </label>
          <input
            id="notes"
            value={form.notes}
            onChange={(event) => setForm((current) => ({ ...current, notes: event.target.value }))}
            className="field"
            placeholder="Allergies, accessibility needs…"
          />
        </div>
        <div className="sm:col-span-2">
          <button type="submit" className="btn btn-primary">
            Save details
          </button>
        </div>
      </form>
    </div>
  );
}

export function AccountPage() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const user = useUiStore((state) => state.user);
  const signOut = useUiStore((state) => state.signOut);

  const active = pathname.endsWith('/orders')
    ? 'orders'
    : pathname.endsWith('/prescriptions')
      ? 'prescriptions'
      : pathname.endsWith('/details')
        ? 'details'
        : 'overview';

  if (!user) {
    return (
      <div className="container-page py-16">
        <div className="mx-auto max-w-md rounded-[26px] border border-line bg-paper p-7 text-center">
          <h1 className="text-2xl font-semibold tracking-[-0.03em]">Sign in to your account</h1>
          <p className="mt-2 text-sm text-muted">View orders, prescriptions and delivery details.</p>
          <Link to="/login" className="btn btn-primary mt-6">
            Sign in
          </Link>
        </div>
      </div>
    );
  }

  return (
    <>
      <PageMeta title="Your account — NexGen Pharmacy" />
      <section className="page-hero py-10 sm:py-12">
        <PageBotanicals />
        <div className="container-page">
          <Reveal>
            <p className="eyebrow text-clay-dark">Your account</p>
            <div className="mt-3 flex flex-wrap items-end justify-between gap-4">
              <h1 className="display text-[2.4rem] sm:text-[3rem]">
                Hello, <span className="serif text-forest">{user.name.split(' ')[0]}.</span>
              </h1>
              <div className="flex items-center gap-4 text-[12px]">
                <button
                  type="button"
                  onClick={() => {
                    signOut();
                    navigate('/');
                  }}
                  className="font-semibold text-muted hover:text-ink"
                >
                  Sign out
                </button>
              </div>
            </div>

            <nav className="mt-7 flex flex-wrap gap-1.5" aria-label="Account sections">
              {tabs.map((tab) => (
                <Link
                  key={tab.id}
                  to={tab.href}
                  className={cn(
                    'shrink-0 rounded-full px-4 py-2 text-[12.5px] font-semibold transition-colors',
                    active === tab.id
                      ? 'bg-forest text-cream'
                      : 'border border-line bg-paper text-ink/70 hover:bg-sage/40',
                  )}
                  aria-current={active === tab.id ? 'page' : undefined}
                >
                  {tab.label}
                </Link>
              ))}
            </nav>
          </Reveal>
        </div>
      </section>

      <div className="container-page py-8 sm:py-10">
        {active === 'overview' ? <Overview /> : null}
        {active === 'orders' ? <OrdersView /> : null}
        {active === 'prescriptions' ? <PrescriptionsView /> : null}
        {active === 'details' ? <DetailsView /> : null}
      </div>
    </>
  );
}
