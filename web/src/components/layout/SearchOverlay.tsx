import { useMemo, useRef, useState } from 'react';
import { ArrowRight, Search, X } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useUiStore } from '@/store/ui';
import { popularSearches } from '@/lib/mock';
import { useProducts } from '@/lib/queries';
import { formatPrice } from '@/lib/utils';
import { useEscape, useFocusTrap, useLockBodyScroll } from '@/components/ui/useOverlay';

export function SearchOverlay() {
  const panelRef = useRef<HTMLDivElement>(null);
  const open = useUiStore((state) => state.searchOpen);
  const close = useUiStore((state) => state.closeSearch);
  const products = useProducts();
  const [query, setQuery] = useState('');

  useLockBodyScroll(open);
  useEscape(open, close);
  useFocusTrap(open, panelRef);

  const results = useMemo(() => {
    const term = query.trim().toLowerCase();
    if (!term) return [];
    return products
      .filter((product) =>
        [product.name, product.subtitle, product.category, ...product.concerns]
          .join(' ')
          .toLowerCase()
          .includes(term),
      )
      .slice(0, 6);
  }, [products, query]);

  if (!open) return null;

  function handleClose() {
    setQuery('');
    close();
  }

  return (
    <div className="fixed inset-0 z-50" role="dialog" aria-modal="true" aria-label="Search">
      <button
        type="button"
        className="absolute inset-0 bg-ink/45 fade-in"
        onClick={handleClose}
        aria-label="Close search"
        tabIndex={-1}
      />
      <div
        ref={panelRef}
        className="relative max-h-[88vh] overflow-y-auto border-b border-line bg-cream shadow-[0_30px_80px_rgba(16,35,28,0.25)]"
      >
        <div className="container-page py-5 sm:py-7">
          <div className="flex items-center gap-3">
            <div className="flex h-14 flex-1 items-center gap-3 rounded-2xl border border-line bg-paper px-4">
              <Search className="size-5 shrink-0 text-muted" strokeWidth={1.7} aria-hidden="true" />
              <label htmlFor="site-search" className="sr-only">
                Search products
              </label>
              <input
                id="site-search"
                type="search"
                autoFocus
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Search treatments, products, advice…"
                className="h-full w-full bg-transparent text-[15px] outline-none placeholder:text-muted"
              />
              {query ? (
                <button
                  type="button"
                  onClick={() => setQuery('')}
                  className="text-xs font-semibold text-muted hover:text-ink"
                >
                  Clear
                </button>
              ) : null}
            </div>
            <button
              type="button"
              onClick={handleClose}
              className="grid size-11 shrink-0 place-items-center rounded-full border border-line transition-colors hover:bg-sage"
              aria-label="Close search"
            >
              <X className="size-5" aria-hidden="true" />
            </button>
          </div>

          {query.trim() === '' ? (
            <div className="py-7">
              <p className="eyebrow text-clay-dark">Popular right now</p>
              <div className="mt-4 flex flex-wrap gap-2">
                {popularSearches.map((term) => (
                  <button
                    key={term}
                    type="button"
                    onClick={() => setQuery(term)}
                    className="rounded-full border border-line bg-paper px-4 py-2 text-[13px] font-medium transition-colors hover:border-forest hover:bg-sage/50"
                  >
                    {term}
                  </button>
                ))}
              </div>
              <div className="mt-8 flex flex-wrap items-center justify-between gap-3 border-t border-line pt-5">
                <p className="text-sm text-muted">
                  Looking for something specific? Our pharmacists can help.
                </p>
                <Link to="/pharmacists" onClick={handleClose} className="link-quiet">
                  Ask a pharmacist
                  <ArrowRight className="size-4" aria-hidden="true" />
                </Link>
              </div>
            </div>
          ) : (
            <div className="py-5">
              <p className="text-xs font-semibold uppercase tracking-[0.14em] text-muted">
                {results.length} {results.length === 1 ? 'result' : 'results'}
              </p>
              {results.length > 0 ? (
                <ul className="mt-3 divide-y divide-line">
                  {results.map((product) => (
                    <li key={product.id}>
                      <Link
                        to={`/shop/${product.slug}`}
                        onClick={handleClose}
                        className="group flex items-center gap-4 py-3.5"
                      >
                        <span className="size-14 shrink-0 overflow-hidden rounded-xl bg-sage/50">
                          <img
                            src={product.image}
                            alt=""
                            width={112}
                            height={140}
                            className="size-full object-cover"
                          />
                        </span>
                        <span className="min-w-0 flex-1">
                          <span className="block text-[10px] font-bold uppercase tracking-[0.14em] text-muted">
                            {product.category} · {product.size}
                          </span>
                          <span className="mt-0.5 block truncate text-[15px] font-semibold tracking-[-0.02em] group-hover:text-forest">
                            {product.name}
                          </span>
                        </span>
                        <span className="tnum text-sm font-bold">{formatPrice(product.price)}</span>
                      </Link>
                    </li>
                  ))}
                </ul>
              ) : (
                <div className="py-10 text-center">
                  <p className="text-lg font-semibold tracking-[-0.02em]">
                    We couldn’t find “{query}”.
                  </p>
                  <p className="mt-2 text-sm text-muted">
                    Try a broader term, or ask a pharmacist for a recommendation.
                  </p>
                  <Link to="/pharmacists" onClick={handleClose} className="btn btn-outline mt-5">
                    Ask a pharmacist
                  </Link>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
