import { Link } from 'react-router-dom';
import { cn } from '@/lib/utils';

interface LogoProps {
  tone?: 'dark' | 'light';
  className?: string;
  withDescriptor?: boolean;
  descriptorClassName?: string;
}

export function Logo({
  tone = 'dark',
  className,
  withDescriptor = false,
  descriptorClassName,
}: LogoProps) {
  const text = tone === 'dark' ? 'text-ink' : 'text-cream';
  return (
    <Link to="/" className={cn('inline-flex items-center gap-2.5', className)}>
      <span
        className={cn(
          'grid size-9 place-items-center rounded-[11px]',
          tone === 'dark' ? 'bg-forest' : 'bg-cream',
        )}
        aria-hidden="true"
      >
        <svg viewBox="0 0 24 24" className="size-5" fill="none">
          <path
            d="M19.8 4.2c-6.5.4-10.4 3.1-10.9 8.3-.2 2.2.6 4 1.9 5.2 1.3 1.2 3.1 1.8 4.9 1.3 5.1-1.4 6.3-6.3 4.1-14.8Z"
            fill={tone === 'dark' ? '#F7F4EC' : '#1B4334'}
          />
          <path
            d="M5.6 20.2c1.6-4.6 4.3-7.8 8.3-9.7"
            stroke="#E4823C"
            strokeWidth="1.6"
            strokeLinecap="round"
          />
        </svg>
      </span>
      <span className="flex flex-col leading-none">
        <span className={cn('text-[1.3rem] font-extrabold tracking-[-0.055em]', text)}>
          northleaf
        </span>
        {withDescriptor ? (
          <span
            className={cn(
              'mt-1 whitespace-nowrap text-[8px] font-bold uppercase tracking-[0.28em]',
              tone === 'dark' ? 'text-muted' : 'text-cream/70',
              descriptorClassName,
            )}
          >
            Pharmacy & wellness
          </span>
        ) : null}
      </span>
    </Link>
  );
}
