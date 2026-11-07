import { Minus, Plus } from 'lucide-react';
import { cn } from '@/lib/utils';

interface QuantityStepperProps {
  value: number;
  onChange: (value: number) => void;
  min?: number;
  max?: number;
  size?: 'sm' | 'md';
  className?: string;
}

export function QuantityStepper({
  value,
  onChange,
  min = 1,
  max = 99,
  size = 'md',
  className,
}: QuantityStepperProps) {
  const buttonSize = size === 'sm' ? 'size-8' : 'size-10';

  return (
    <div
      className={cn(
        'inline-flex items-center rounded-full border border-line bg-paper',
        className,
      )}
    >
      <button
        type="button"
        className={cn(
          buttonSize,
          'grid place-items-center rounded-full text-ink transition-colors hover:bg-sage disabled:opacity-35',
        )}
        onClick={() => onChange(Math.max(min, value - 1))}
        disabled={value <= min}
        aria-label="Decrease quantity"
      >
        <Minus className="size-4" aria-hidden="true" />
      </button>
      <span
        className={cn('tnum min-w-7 text-center font-bold', size === 'sm' ? 'text-sm' : 'text-base')}
        aria-live="polite"
        aria-label={`Quantity ${value}`}
      >
        {value}
      </span>
      <button
        type="button"
        className={cn(
          buttonSize,
          'grid place-items-center rounded-full text-ink transition-colors hover:bg-sage disabled:opacity-35',
        )}
        onClick={() => onChange(Math.min(max, value + 1))}
        disabled={value >= max}
        aria-label="Increase quantity"
      >
        <Plus className="size-4" aria-hidden="true" />
      </button>
    </div>
  );
}
