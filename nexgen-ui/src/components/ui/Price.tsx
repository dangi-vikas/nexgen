import { cn, formatPrice } from '@/lib/utils';

interface PriceProps {
  value: number;
  className?: string;
  size?: 'sm' | 'md' | 'lg';
}

const sizes = {
  sm: 'text-sm',
  md: 'text-base',
  lg: 'text-2xl',
};

export function Price({ value, className, size = 'md' }: PriceProps) {
  return (
    <span className={cn('tnum font-semibold', sizes[size], className)}>{formatPrice(value)}</span>
  );
}
