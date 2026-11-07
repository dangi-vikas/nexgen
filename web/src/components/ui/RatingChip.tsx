import { Star } from 'lucide-react';
import { cn } from '@/lib/utils';

interface RatingChipProps {
  rating: number;
  reviews?: number;
  className?: string;
}

export function RatingChip({ rating, reviews, className }: RatingChipProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 rounded-full bg-paper/95 px-2.5 py-1 text-[11px] font-bold text-ink shadow-[0_2px_10px_rgba(16,35,28,0.10)]',
        className,
      )}
      aria-label={`Rated ${rating} out of 5${reviews ? ` from ${reviews} reviews` : ''}`}
    >
      <Star className="size-3 fill-clay text-clay" strokeWidth={0} aria-hidden="true" />
      <span className="tnum">{rating.toFixed(1)}</span>
      {reviews ? <span className="tnum font-medium text-muted">({reviews})</span> : null}
    </span>
  );
}
