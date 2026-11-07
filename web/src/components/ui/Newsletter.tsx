import { useState, type FormEvent } from 'react';
import { ArrowRight, Check } from 'lucide-react';
import { cn } from '@/lib/utils';

interface NewsletterProps {
  tone?: 'light' | 'dark';
  compact?: boolean;
}

export function Newsletter({ tone = 'light', compact = false }: NewsletterProps) {
  const [email, setEmail] = useState('');
  const [status, setStatus] = useState<'idle' | 'error' | 'success'>('idle');

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      setStatus('error');
      return;
    }
    setStatus('success');
    setEmail('');
  }

  const dark = tone === 'dark';

  return (
    <div className={cn(compact ? 'w-full' : 'max-w-xl')}>
      <form onSubmit={handleSubmit} noValidate>
        <label htmlFor={`newsletter-${tone}`} className="sr-only">
          Email address
        </label>
        <div
          className={cn(
            'flex items-center gap-2 rounded-full border p-1.5 pl-5',
            dark ? 'border-cream/20 bg-cream/5' : 'border-line bg-paper',
          )}
        >
          <input
            id={`newsletter-${tone}`}
            type="email"
            name="email"
            autoComplete="email"
            placeholder="Your email address"
            value={email}
            onChange={(event) => {
              setEmail(event.target.value);
              if (status !== 'idle') setStatus('idle');
            }}
            className={cn(
              'min-w-0 flex-1 bg-transparent py-2 text-sm outline-none',
              dark ? 'text-cream placeholder:text-cream/60' : 'text-ink placeholder:text-muted',
            )}
            aria-invalid={status === 'error'}
            aria-describedby={status === 'error' ? `newsletter-error-${tone}` : undefined}
          />
          <button
            type="submit"
            className={cn(
              'grid size-10 shrink-0 place-items-center rounded-full transition-colors',
              dark ? 'bg-clay text-ink hover:bg-clay-soft' : 'bg-forest text-cream hover:bg-ink',
            )}
            aria-label="Subscribe to the newsletter"
          >
            {status === 'success' ? (
              <Check className="size-4" aria-hidden="true" />
            ) : (
              <ArrowRight className="size-4" aria-hidden="true" />
            )}
          </button>
        </div>
        <div className="mt-2.5 min-h-5 px-1 text-xs" aria-live="polite">
          {status === 'error' ? (
            <span id={`newsletter-error-${tone}`} className="text-clay-dark">
              Please enter a valid email address.
            </span>
          ) : null}
          {status === 'success' ? (
            <span className={dark ? 'text-sage' : 'text-forest'}>
              You’re on the list. Look out for our next letter.
            </span>
          ) : null}
          {status === 'idle' ? (
            <span className={dark ? 'text-cream/60' : 'text-muted'}>
              One letter a month. Unsubscribe anytime.
            </span>
          ) : null}
        </div>
      </form>
    </div>
  );
}
