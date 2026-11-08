import { useMemo, useState } from 'react';
import { ChevronDown, SlidersHorizontal, X } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import { PageMeta } from '@/components/ui/PageMeta';
import { Breadcrumbs } from '@/components/ui/Breadcrumbs';
import { PageBotanicals } from '@/components/ui/Botanical';
import { Reveal } from '@/components/ui/Reveal';
import { ProductCard } from '@/components/product/ProductCard';
import { EmptyState } from '@/components/ui/EmptyState';
import { categories } from '@/lib/mock';
import { useProducts } from '@/lib/queries';
import { cn } from '@/lib/utils';
import { PackageSearch } from 'lucide-react';
import type { Product } from '@/lib/types';

type SortKey = 'featured' | 'price-asc' | 'price-desc' | 'rating';
type PriceBand = 'all' | 'under20' | '20to30' | 'over30';

const priceBands: { id: PriceBand; label: string }[] = [
  { id: 'all', label: 'Any price' },
  { id: 'under20', label: 'Under $20' },
  { id: '20to30', label: '$20 – $30' },
  { id: 'over30', label: 'Over $30' },
];

interface FilterPanelProps {
  activeCategory: string;
  onCategory: (category: string) => void;
  products: Product[];
  concerns: string[];
  selectedConcerns: string[];
  onConcern: (concern: string) => void;
  priceBand: PriceBand;
  onPriceBand: (band: PriceBand) => void;
  inStock: boolean;
  onInStock: (value: boolean) => void;
  onClear: () => void;
}

function FilterPanel({
  activeCategory,
  onCategory,
  products,
  concerns,
  selectedConcerns,
  onConcern,
  priceBand,
  onPriceBand,
  inStock,
  onInStock,
  onClear,
}: FilterPanelProps) {
  return (
    <div className="space-y-7">
      <div>
        <h2 className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted">Category</h2>
        <ul className="mt-3 space-y-1">
          <li>
            <button
              type="button"
              onClick={() => onCategory('all')}
              className={cn(
                'flex w-full items-center justify-between rounded-lg px-2.5 py-2 text-[13.5px] transition-colors',
                activeCategory === 'all'
                  ? 'bg-sage/70 font-semibold text-forest'
                  : 'text-ink/75 hover:bg-sage/35',
              )}
            >
              All products
              <span className="tnum text-[11px] text-muted">{products.length}</span>
            </button>
          </li>
          {categories.map((category) => {
            const count = products.filter((product) =>
              category.id === 'immunity' || category.id === 'heart'
                ? product.concerns.includes(
                    category.id === 'immunity' ? 'Immunity' : 'Heart',
                  )
                : product.category.toLowerCase() === category.id,
            ).length;
            if (count === 0) return null;
            return (
              <li key={category.id}>
                <button
                  type="button"
                  onClick={() => onCategory(category.id)}
                  className={cn(
                    'flex w-full items-center justify-between rounded-lg px-2.5 py-2 text-[13.5px] transition-colors',
                    activeCategory === category.id
                      ? 'bg-sage/70 font-semibold text-forest'
                      : 'text-ink/75 hover:bg-sage/35',
                  )}
                >
                  {category.label}
                  <span className="tnum text-[11px] text-muted">{count}</span>
                </button>
              </li>
            );
          })}
        </ul>
      </div>

      <div>
        <h2 className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted">
          Health concern
        </h2>
        <ul className="mt-3 max-h-64 space-y-2 overflow-y-auto pr-1">
          {concerns.map((concern) => (
            <li key={concern}>
              <label className="flex cursor-pointer items-center gap-2.5 text-[13.5px] text-ink/75">
                <input
                  type="checkbox"
                  checked={selectedConcerns.includes(concern)}
                  onChange={() => onConcern(concern)}
                  className="size-4 rounded border-line accent-forest"
                />
                {concern}
              </label>
            </li>
          ))}
        </ul>
      </div>

      <div>
        <h2 className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted">Price</h2>
        <ul className="mt-3 space-y-2">
          {priceBands.map((band) => (
            <li key={band.id}>
              <label className="flex cursor-pointer items-center gap-2.5 text-[13.5px] text-ink/75">
                <input
                  type="radio"
                  name="price"
                  checked={priceBand === band.id}
                  onChange={() => onPriceBand(band.id)}
                  className="size-4 accent-forest"
                />
                {band.label}
              </label>
            </li>
          ))}
        </ul>
      </div>

      <div>
        <label className="flex cursor-pointer items-center gap-2.5 text-[13.5px] text-ink/75">
          <input
            type="checkbox"
            checked={inStock}
            onChange={(event) => onInStock(event.target.checked)}
            className="size-4 rounded border-line accent-forest"
          />
          In stock only
        </label>
      </div>

      <button
        type="button"
        onClick={onClear}
        className="text-[12px] font-bold uppercase tracking-[0.1em] text-clay-dark hover:text-ink"
      >
        Clear all filters
      </button>
    </div>
  );
}

export function ShopPage() {
  const [params, setParams] = useSearchParams();
  const products = useProducts();
  const activeCategory = params.get('category') ?? 'all';
  const [selectedConcerns, setSelectedConcerns] = useState<string[]>([]);
  const [priceBand, setPriceBand] = useState<PriceBand>('all');
  const [inStock, setInStock] = useState(false);
  const [sort, setSort] = useState<SortKey>('featured');
  const [filtersOpen, setFiltersOpen] = useState(false);

  const concernOptions = useMemo(
    () => [...new Set(products.flatMap((product) => product.concerns))].sort(),
    [products],
  );

  const filtered = useMemo(() => {
    const list = products.filter((product) => {
      if (activeCategory !== 'all') {
        const matchesCategory =
          product.category.toLowerCase() === activeCategory ||
          (activeCategory === 'immunity' && product.concerns.includes('Immunity')) ||
          (activeCategory === 'heart' && product.concerns.includes('Heart'));
        if (!matchesCategory) return false;
      }
      if (selectedConcerns.length > 0) {
        if (!selectedConcerns.some((concern) => product.concerns.includes(concern))) {
          return false;
        }
      }
      if (priceBand === 'under20' && product.price >= 20) return false;
      if (priceBand === '20to30' && (product.price < 20 || product.price > 30)) return false;
      if (priceBand === 'over30' && product.price <= 30) return false;
      if (inStock && product.stock === 0) return false;
      return true;
    });

    switch (sort) {
      case 'price-asc':
        return [...list].sort((a, b) => a.price - b.price);
      case 'price-desc':
        return [...list].sort((a, b) => b.price - a.price);
      case 'rating':
        return [...list].sort((a, b) => b.rating - a.rating);
      default:
        return list;
    }
  }, [activeCategory, inStock, priceBand, products, selectedConcerns, sort]);

  function setCategory(category: string) {
    const next = new URLSearchParams(params);
    if (category === 'all') next.delete('category');
    else next.set('category', category);
    setParams(next, { replace: true });
  }

  function toggleConcern(concern: string) {
    setSelectedConcerns((current) =>
      current.includes(concern)
        ? current.filter((item) => item !== concern)
        : [...current, concern],
    );
  }

  function clearFilters() {
    setSelectedConcerns([]);
    setPriceBand('all');
    setInStock(false);
    setCategory('all');
    setFiltersOpen(false);
  }

  const activeFilterCount =
    (activeCategory !== 'all' ? 1 : 0) + selectedConcerns.length + (priceBand !== 'all' ? 1 : 0) + (inStock ? 1 : 0);

  const filterProps: FilterPanelProps = {
    activeCategory,
    onCategory: setCategory,
    products,
    concerns: concernOptions,
    selectedConcerns,
    onConcern: toggleConcern,
    priceBand,
    onPriceBand: setPriceBand,
    inStock,
    onInStock: setInStock,
    onClear: clearFilters,
  };

  return (
    <>
      <PageMeta
        title="Shop all — NexGen Pharmacy"
        description="Browse pharmacist-curated skin, supplements, herbal remedies and everyday essentials."
      />

      <section className="page-hero py-10 sm:py-12">
        <PageBotanicals />
        <div className="container-page">
          <Breadcrumbs items={[{ label: 'Shop' }]} />
          <Reveal>
            <div className="mt-6 flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <p className="eyebrow text-clay-dark">
                  <span className="inline-block h-px w-7 bg-clay-dark/70" aria-hidden="true" />
                  The shelf
                </p>
                <h1 className="display mt-3 text-[2.5rem] sm:text-[3.3rem]">
                  Everyday care, <span className="serif text-forest">considered.</span>
                </h1>
                <p className="mt-3 max-w-xl text-[14.5px] leading-relaxed text-muted">
                  A small, deliberate range chosen by our pharmacists. If it is here, we can tell
                  you why.
                </p>
              </div>
              <p className="text-[12px] font-semibold uppercase tracking-[0.14em] text-muted">
                {filtered.length} {filtered.length === 1 ? 'product' : 'products'}
              </p>
            </div>
          </Reveal>
        </div>
      </section>

      <section className="py-8 sm:py-10">
        <div className="container-page">
          <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
            <button
              type="button"
              onClick={() => setFiltersOpen((open) => !open)}
              className="inline-flex items-center gap-2 rounded-full border border-line bg-paper px-4 py-2.5 text-[13px] font-semibold lg:hidden"
              aria-expanded={filtersOpen}
              aria-controls="mobile-filters"
            >
              <SlidersHorizontal className="size-4" aria-hidden="true" />
              Filters
              {activeFilterCount > 0 ? (
                <span className="grid size-5 place-items-center rounded-full bg-forest text-[10px] font-bold text-cream">
                  {activeFilterCount}
                </span>
              ) : null}
            </button>

            <div className="ml-auto flex items-center gap-2">
              <label htmlFor="sort" className="text-[12px] font-medium text-muted">
                Sort
              </label>
              <div className="relative">
                <select
                  id="sort"
                  value={sort}
                  onChange={(event) => setSort(event.target.value as SortKey)}
                  className="appearance-none rounded-full border border-line bg-paper py-2.5 pl-4 pr-9 text-[13px] font-semibold outline-none transition-colors focus:border-forest"
                >
                  <option value="featured">Featured</option>
                  <option value="price-asc">Price: low to high</option>
                  <option value="price-desc">Price: high to low</option>
                  <option value="rating">Top rated</option>
                </select>
                <ChevronDown
                  className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-muted"
                  aria-hidden="true"
                />
              </div>
            </div>
          </div>

          {filtersOpen ? (
            <div id="mobile-filters" className="mb-7 rounded-2xl border border-line bg-paper p-5 lg:hidden">
              <div className="mb-4 flex items-center justify-between">
                <p className="text-sm font-bold">Filters</p>
                <button
                  type="button"
                  onClick={() => setFiltersOpen(false)}
                  className="grid size-8 place-items-center rounded-full hover:bg-sage"
                  aria-label="Close filters"
                >
                  <X className="size-4" aria-hidden="true" />
                </button>
              </div>
              <FilterPanel {...filterProps} />
            </div>
          ) : null}

          <h2 className="sr-only">Products</h2>

          <div className="grid gap-10 lg:grid-cols-[230px_1fr] lg:gap-12">
            <aside className="hidden lg:block">
              <div className="sticky top-28">
                <FilterPanel {...filterProps} />
              </div>
            </aside>

            <div>
              {filtered.length > 0 ? (
                <div className="grid grid-cols-2 gap-x-4 gap-y-10 sm:gap-x-5 md:grid-cols-3">
                  {filtered.map((product, index) => (
                    <Reveal key={product.id} delay={(index % 3) * 55} variant="zoom">
                      <ProductCard product={product} priority={index < 3} />
                    </Reveal>
                  ))}
                </div>
              ) : (
                <EmptyState
                  icon={PackageSearch}
                  title="Nothing matches those filters"
                  description="Try removing a filter or two, or browse the full shelf."
                  action={{ label: 'Clear filters', onClick: clearFilters }}
                />
              )}
            </div>
          </div>
        </div>
      </section>
    </>
  );
}
