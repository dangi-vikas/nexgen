import { cn } from '@/lib/utils';

interface StockBadgeProps {
  stock: number;
  className?: string;
}

export function StockBadge({ stock, className }: StockBadgeProps) {
  if (stock === 0) {
    return (
      <span className={cn('inline-flex items-center gap-1.5 text-xs font-semibold text-muted', className)}>
        <span className="size-1.5 rounded-full bg-muted" aria-hidden="true" />
        Out of stock
      </span>
    );
  }

  if (stock < 20) {
    return (
      <span className={cn('inline-flex items-center gap-1.5 text-xs font-semibold text-clay-dark', className)}>
        <span className="size-1.5 rounded-full bg-clay-dark" aria-hidden="true" />
        Only {stock} left
      </span>
    );
  }

  return (
    <span className={cn('inline-flex items-center gap-1.5 text-xs font-semibold text-forest', className)}>
      <span className="size-1.5 rounded-full bg-forest" aria-hidden="true" />
      In stock
    </span>
  );
}
