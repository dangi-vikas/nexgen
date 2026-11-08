import { useMemo, useState, type ReactNode } from 'react';
import {
  ArrowRight,
  Check,
  ChevronDown,
  Heart,
  PackageCheck,
  ShieldCheck,
  Star,
  Truck,
} from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import type { Product } from '@/lib/types';
import { useProduct, useProducts } from '@/lib/queries';
import { PageMeta } from '@/components/ui/PageMeta';
import { Breadcrumbs } from '@/components/ui/Breadcrumbs';
import { LeafSprig } from '@/components/ui/Botanical';
import { Reveal } from '@/components/ui/Reveal';
import { QuantityStepper } from '@/components/ui/QuantityStepper';
import { StockBadge } from '@/components/ui/StockBadge';
import { ProductCard } from '@/components/product/ProductCard';
import { EmptyState } from '@/components/ui/EmptyState';
import { useCartStore } from '@/store/cart';
import { cn, formatPrice } from '@/lib/utils';
import { PackageSearch } from 'lucide-react';

function AccordionItem({
  title,
  children,
  defaultOpen = false,
}: {
  title: string;
  children: ReactNode;
  defaultOpen?: boolean;
}) {
  return (
    <details className="group border-b border-line" open={defaultOpen}>
      <summary className="flex cursor-pointer list-none items-center justify-between gap-4 py-4 text-[13.5px] font-semibold marker:hidden">
        {title}
        <ChevronDown
          className="size-4 shrink-0 text-muted transition-transform duration-200 group-open:rotate-180"
          aria-hidden="true"
        />
      </summary>
      <div className="pb-5 text-[13.5px] leading-relaxed text-muted">{children}</div>
    </details>
  );
}

function DetailView({ product }: { product: Product }) {
  const [activeImage, setActiveImage] = useState(0);
  const [quantity, setQuantity] = useState(1);
  const [saved, setSaved] = useState(false);
  const products = useProducts();
  const add = useCartStore((state) => state.add);
  const openCart = useCartStore((state) => state.open);

  const related = useMemo(() => {
    const sameCategory = products.filter(
      (item) => item.id !== product.id && item.category === product.category,
    );
    const fill = products.filter(
      (item) => item.id !== product.id && item.category !== product.category,
    );
    return [...sameCategory, ...fill].slice(0, 4);
  }, [product, products]);

  function handleAdd() {
    add(product, quantity);
    openCart();
  }

  return (
    <>
      <PageMeta
        title={`${product.name} — NexGen Pharmacy`}
        description={product.description}
      />

      <div className="container-page pb-16 pt-6 sm:pt-8">
        <Breadcrumbs
          items={[
            { label: 'Shop', href: '/shop' },
            { label: product.category, href: `/shop?category=${product.category.toLowerCase()}` },
            { label: product.name },
          ]}
        />

        <div className="mt-7 grid gap-10 lg:grid-cols-[1.02fr_0.98fr] lg:gap-14 xl:gap-20">
          <div className="self-start lg:sticky lg:top-28">
            <div className="relative">
              <div className="relative overflow-hidden rounded-[28px] bg-sage/50">
                <img
                  src={product.gallery[activeImage] ?? product.image}
                  alt={`${product.name}, view ${activeImage + 1}`}
                  width={900}
                  height={1125}
                  className="w-full aspect-[4/4.6] object-cover"
                />
                {product.badge ? (
                  <span className="absolute left-4 top-4 rounded-full bg-cream/95 px-3 py-1.5 text-[10px] font-bold uppercase tracking-[0.13em] text-forest">
                    {product.badge}
                  </span>
                ) : null}
              </div>
              <div className="pointer-events-none absolute -left-5 -top-5 h-24 w-16 text-forest/20">
                <LeafSprig className="leaf-sway size-full" />
              </div>
              <div className="pointer-events-none absolute -bottom-5 -right-4 h-28 w-20 rotate-180 text-forest/15">
                <LeafSprig className="leaf-sway-slow size-full" />
              </div>
            </div>
            <div className="mt-3 grid grid-cols-4 gap-3">
              {product.gallery.map((image, index) => (
                <button
                  key={image}
                  type="button"
                  onClick={() => setActiveImage(index)}
                  className={cn(
                    'overflow-hidden rounded-xl border-2 bg-sage/40 p-0 transition-colors',
                    activeImage === index ? 'border-forest' : 'border-transparent hover:border-line-strong',
                  )}
                  aria-label={`Show image ${index + 1} of ${product.name}`}
                  aria-pressed={activeImage === index}
                >
                  <img
                    src={image}
                    alt=""
                    width={200}
                    height={240}
                    className="w-full aspect-[4/5] object-cover"
                  />
                </button>
              ))}
            </div>
          </div>

          <Reveal variant="right" delay={80} className="pb-4">
            <p className="eyebrow text-clay-dark">{product.category}</p>
            <h1 className="display mt-3 text-[2.3rem] leading-[1.02] sm:text-[2.9rem]">
              {product.name}
            </h1>
            <p className="mt-2 text-[15px] text-muted">{product.subtitle}</p>

            <div className="mt-4 flex flex-wrap items-center gap-x-4 gap-y-2">
              <span className="flex items-center gap-1.5 text-[13px]">
                <span className="flex" aria-hidden="true">
                  {Array.from({ length: 5 }).map((_, index) => (
                    <Star
                      key={index}
                      className={cn(
                        'size-3.5',
                        index < Math.round(product.rating) ? 'fill-clay text-clay' : 'text-line-strong',
                      )}
                      strokeWidth={0}
                    />
                  ))}
                </span>
                <span className="font-bold">{product.rating.toFixed(1)}</span>
                <span className="text-muted">({product.reviews} reviews)</span>
              </span>
              <StockBadge stock={product.stock} />
            </div>

            <div className="mt-5 flex items-baseline gap-3">
              <span className="tnum text-[2rem] font-bold tracking-[-0.03em]">
                {formatPrice(product.price)}
              </span>
              {product.compareAt ? (
                <>
                  <span className="tnum text-sm text-muted line-through">
                    {formatPrice(product.compareAt)}
                  </span>
                  <span className="rounded-full bg-clay-soft/50 px-2.5 py-1 text-[10px] font-bold uppercase tracking-[0.1em] text-clay-dark">
                    Save {formatPrice(product.compareAt - product.price)}
                  </span>
                </>
              ) : null}
            </div>
            <p className="mt-1 text-[11.5px] text-muted">
              {product.size} · Tax included. Delivery calculated at checkout.
            </p>

            <p className="mt-6 text-[14.5px] leading-relaxed text-ink/75">{product.description}</p>

            <div className="mt-6 flex items-stretch gap-2 sm:gap-3">
              <QuantityStepper
                value={quantity}
                onChange={setQuantity}
                max={Math.max(1, product.stock)}
              />
              <button
                type="button"
                onClick={handleAdd}
                disabled={product.stock === 0}
                className="btn btn-primary h-11 min-w-0 flex-1 px-4 disabled:cursor-not-allowed disabled:opacity-45 sm:px-6"
              >
                <span className="sm:hidden">Add to bag</span>
                <span className="hidden sm:inline">
                  Add to bag · {formatPrice(product.price * quantity)}
                </span>
                <ArrowRight className="size-4 shrink-0" aria-hidden="true" />
              </button>
              <button
                type="button"
                onClick={() => setSaved((value) => !value)}
                className={cn(
                  'grid size-11 shrink-0 place-items-center rounded-full border transition-colors',
                  saved
                    ? 'border-clay bg-clay/15 text-clay-dark'
                    : 'border-line text-ink hover:bg-sage',
                )}
                aria-label={saved ? 'Remove from saved items' : 'Save for later'}
                aria-pressed={saved}
              >
                <Heart className={cn('size-4', saved && 'fill-clay text-clay')} aria-hidden="true" />
              </button>
            </div>

            <ul className="mt-6 grid gap-2.5 rounded-2xl border border-line bg-paper p-4 text-[12.5px] text-muted sm:grid-cols-3 sm:gap-2">
              <li className="flex items-center gap-2">
                <Truck className="size-4 text-forest" strokeWidth={1.7} aria-hidden="true" />
                Free over $35
              </li>
              <li className="flex items-center gap-2">
                <PackageCheck className="size-4 text-forest" strokeWidth={1.7} aria-hidden="true" />
                Same-day pickup
              </li>
              <li className="flex items-center gap-2">
                <ShieldCheck className="size-4 text-forest" strokeWidth={1.7} aria-hidden="true" />
                Pharmacist checked
              </li>
            </ul>

            <div className="mt-6">
              <AccordionItem title="Ingredients" defaultOpen>
                <ul className="space-y-1.5">
                  {product.ingredients.map((ingredient) => (
                    <li key={ingredient} className="flex items-start gap-2">
                      <Check className="mt-0.5 size-3.5 shrink-0 text-forest" aria-hidden="true" />
                      {ingredient}
                    </li>
                  ))}
                </ul>
              </AccordionItem>
              <AccordionItem title="How to use">{product.dosage ?? product.description}</AccordionItem>
              <AccordionItem title="Delivery & returns">
                Free delivery on orders over $35. Order before 4pm for same-day pickup at 48 Alder
                Street. Unopened products can be returned within 30 days; medicines are exempt once
                dispensed.
              </AccordionItem>
            </div>

            <div className="mt-6 flex items-start gap-3 rounded-2xl bg-sage/55 p-4">
              <span className="grid size-9 shrink-0 place-items-center rounded-full bg-forest text-cream">
                <LeafMark />
              </span>
              <p className="text-[12.5px] leading-relaxed text-ink/70">
                Not sure if this is right for you?{' '}
                <Link to="/pharmacists" className="font-semibold text-forest underline underline-offset-2">
                  Ask a pharmacist
                </Link>{' '}
                — a real conversation, free of charge.
              </p>
            </div>
          </Reveal>
        </div>
      </div>

      <section className="border-t border-line py-14 sm:py-16">
        <div className="container-page">
          <div className="flex items-end justify-between gap-4">
            <div>
              <p className="eyebrow text-clay-dark">Pairs well with</p>
              <h2 className="display mt-2 text-[1.8rem] sm:text-[2.2rem]">A few quiet essentials</h2>
            </div>
            <Link to="/shop" className="link-quiet hidden sm:inline-flex">
              Shop all
              <ArrowRight className="size-4" aria-hidden="true" />
            </Link>
          </div>
          <div className="mt-8 grid grid-cols-2 gap-x-4 gap-y-8 sm:gap-x-5 lg:grid-cols-4">
            {related.map((item) => (
              <ProductCard key={item.id} product={item} />
            ))}
          </div>
        </div>
      </section>
    </>
  );
}

function LeafMark() {
  return (
    <svg viewBox="0 0 24 24" className="size-4" fill="none" aria-hidden="true">
      <path
        d="M19 5c-6 .4-9.6 2.9-10 7.7-.2 2 .5 3.6 1.7 4.7 1.2 1.1 2.9 1.6 4.5 1.2 4.7-1.3 5.8-5.9 3.8-13.6Z"
        fill="currentColor"
      />
      <path d="M5 19.5c1.5-4.2 4-7.1 7.6-8.9" stroke="#E4823C" strokeWidth="1.4" strokeLinecap="round" />
    </svg>
  );
}

export function ProductPage() {
  const { slug } = useParams<{ slug: string }>();
  const product = useProduct(slug);

  if (!product) {
    return (
      <div className="container-page py-20">
        <EmptyState
          icon={PackageSearch}
          title="We couldn’t find that product"
          description="It may have been renamed or is no longer stocked. The rest of the shelf is right here."
          action={{ label: 'Back to shop', href: '/shop' }}
        />
      </div>
    );
  }

  return <DetailView product={product} />;
}
