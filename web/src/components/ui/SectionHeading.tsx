import type { ReactNode } from 'react';
import { ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import { cn } from '@/lib/utils';

interface SectionHeadingProps {
  eyebrow?: string;
  title: ReactNode;
  description?: string;
  action?: { label: string; href: string };
  align?: 'left' | 'center';
  tone?: 'light' | 'dark';
  className?: string;
}

export function SectionHeading({
  eyebrow,
  title,
  description,
  action,
  align = 'left',
  tone = 'light',
  className,
}: SectionHeadingProps) {
  const centered = align === 'center';
  return (
    <div
      className={cn(
        'flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between',
        centered && 'sm:flex-col sm:items-center sm:text-center',
        className,
      )}
    >
      <div className={cn('max-w-2xl', centered && 'mx-auto')}>
        {eyebrow ? (
          <p className={cn('eyebrow mb-3', tone === 'dark' ? 'text-sage-deep' : 'text-clay-dark')}>
            <span
              className={cn('inline-block h-px w-6', tone === 'dark' ? 'bg-sage-deep/70' : 'bg-clay-dark/70')}
              aria-hidden="true"
            />
            {eyebrow}
          </p>
        ) : null}
        <h2
          className={cn(
            'display text-[2rem] leading-[1.02] sm:text-[2.6rem] lg:text-[3rem]',
            tone === 'dark' ? 'text-cream' : 'text-ink',
          )}
        >
          {title}
        </h2>
        {description ? (
          <p
            className={cn(
              'mt-3 max-w-xl text-[15px] leading-relaxed',
              tone === 'dark' ? 'text-cream/70' : 'text-muted',
              centered && 'mx-auto',
            )}
          >
            {description}
          </p>
        ) : null}
      </div>
      {action ? (
        <Link
          to={action.href}
          className={cn('link-quiet shrink-0', tone === 'dark' && 'text-sage')}
        >
          {action.label}
          <ArrowRight className="size-4" aria-hidden="true" />
        </Link>
      ) : null}
    </div>
  );
}
