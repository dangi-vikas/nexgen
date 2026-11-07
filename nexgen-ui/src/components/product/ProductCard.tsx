import { useState } from 'react';
import { Heart, Plus } from 'lucide-react';
import { Link } from 'react-router-dom';
import type { Product } from '@/lib/types';
import { useCartStore } from '@/store/cart';
import { useUiStore } from '@/store/ui';
import { cn, formatPrice } from '@/lib/utils';
import { RatingChip } from '@/components/ui/RatingChip';

interface ProductCardProps {
  product: Product;
  className?: string;
  priority?: boolean;
}

export function ProductCard({ product, className, priority = false }: ProductCardProps) {
  const [saved, setSaved] = useState(false);
  const add = useCartStore((state) => state.add);
  const showToast = useUiStore((state) => state.showToast);

  function handleAdd() {
    add(product);
    showToast({
      title: 'Added to your bag',
      detail: `${product.name} · ${product.size}`,
      image: product.image,
    });
  }

  return (
    <article className={cn('group flex flex-col', className)}>
      <div className="relative">
        <Link
          to={`/shop/${product.slug}`}
          className="block aspect-[4/5] overflow-hidden rounded-[26px] bg-sage/55"
          aria-label={`View ${product.name}`}
        >
          <img
            src={product.image}
            alt={product.name}
            width={900}
            height={1125}
            loading={priority ? 'eager' : 'lazy'}
            decoding="async"
            className="size-full object-cover transition-transform duration-700 ease-[cubic-bezier(.22,1,.36,1)] group-hover:scale-[1.035]"
          />
        </Link>

        <div className="pointer-events-none absolute inset-x-2.5 top-2.5 flex items-start justify-between gap-1.5 sm:inset-x-3 sm:top-3 sm:gap-2">
          <RatingChip rating={product.rating} reviews={product.reviews} />
          <button
            type="button"
            onClick={() => setSaved((value) => !value)}
            className="pointer-events-auto grid size-8 place-items-center rounded-full bg-paper/95 text-ink shadow-[0_2px_10px_rgba(16,35,28,0.10)] transition-colors hover:bg-paper sm:size-9"
            aria-label={saved ? `Remove ${product.name} from saved items` : `Save ${product.name}`}
            aria-pressed={saved}
          >
            <Heart
              className={cn('size-3.5 transition-colors sm:size-4', saved && 'fill-clay text-clay')}
              strokeWidth={1.7}
              aria-hidden="true"
            />
          </button>
        </div>

        {product.badge ? (
          <span className="absolute bottom-2.5 left-2.5 rounded-full bg-cream/95 px-2 py-0.5 text-[9px] font-bold uppercase tracking-[0.1em] text-forest max-[359px]:hidden sm:bottom-3 sm:left-3 sm:px-2.5 sm:py-1 sm:text-[10px] sm:tracking-[0.12em]">
            {product.badge}
          </span>
        ) : null}

        <button
          type="button"
          onClick={handleAdd}
          disabled={product.stock === 0}
          className="absolute bottom-2.5 right-2.5 grid size-10 place-items-center rounded-full bg-forest text-cream shadow-[0_8px_20px_-8px_rgba(16,35,28,0.7)] transition-all duration-300 hover:scale-105 hover:bg-clay hover:text-ink disabled:cursor-not-allowed disabled:opacity-40 sm:bottom-3 sm:right-3 sm:size-11"
          aria-label={`Add ${product.name} to bag`}
        >
          <Plus className="size-5" strokeWidth={2} aria-hidden="true" />
        </button>
      </div>

      <div className="flex flex-1 flex-col pt-4">
        <div className="flex flex-wrap items-center justify-between gap-x-2 gap-y-0.5 text-[9.5px] font-bold uppercase tracking-[0.12em] text-muted sm:gap-x-3 sm:text-[10px] sm:tracking-[0.15em]">
          <span>{product.category}</span>
          <span className="normal-case tracking-normal">{product.size}</span>
        </div>
        <h3 className="mt-1.5 text-[0.98rem] font-semibold leading-snug tracking-[-0.025em] sm:text-[1.02rem]">
          <Link to={`/shop/${product.slug}`} className="transition-colors hover:text-forest">
            {product.name}
          </Link>
        </h3>
        <p className="mt-0.5 line-clamp-1 text-[13px] text-muted">{product.subtitle}</p>
        <div className="mt-auto flex flex-wrap items-baseline gap-x-2 gap-y-1 pt-2.5">
          <span className="tnum text-[1.05rem] font-bold">{formatPrice(product.price)}</span>
          {product.compareAt ? (
            <span className="tnum text-xs text-muted line-through">
              {formatPrice(product.compareAt)}
            </span>
          ) : null}
          {product.stock > 0 && product.stock < 20 ? (
            <span className="ml-auto text-[10px] font-semibold text-clay-dark sm:text-[11px]">
              Only {product.stock} left
            </span>
          ) : null}
        </div>
      </div>
    </article>
  );
}
