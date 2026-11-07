import { useRef } from 'react';
import { X } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useFocusTrap, useLockBodyScroll, useEscape } from '@/components/ui/useOverlay';
import { Logo } from '@/components/ui/Logo';
import { useUiStore } from '@/store/ui';

interface MobileNavProps {
  open: boolean;
  onClose: () => void;
}

const links = [
  { label: 'Shop all', href: '/shop' },
  { label: 'Prescriptions', href: '/prescriptions' },
  { label: 'Pharmacists', href: '/pharmacists' },
  { label: 'Health goals', href: '/#goals' },
  { label: 'Journal', href: '/journal' },
  { label: 'Help & contact', href: '/help' },
];

export function MobileNav({ open, onClose }: MobileNavProps) {
  const panelRef = useRef<HTMLDivElement>(null);
  const user = useUiStore((state) => state.user);
  useLockBodyScroll(open);
  useEscape(open, onClose);
  useFocusTrap(open, panelRef);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 lg:hidden" role="dialog" aria-modal="true" aria-label="Menu">
      <button
        type="button"
        className="absolute inset-0 bg-ink/45 fade-in"
        onClick={onClose}
        aria-label="Close menu"
        tabIndex={-1}
      />
      <div
        ref={panelRef}
        className="drawer-enter absolute inset-y-0 left-0 flex w-[88%] max-w-[380px] flex-col bg-cream"
      >
        <div className="flex items-center justify-between border-b border-line px-5 py-4">
          <Logo />
          <button
            type="button"
            onClick={onClose}
            className="grid size-10 place-items-center rounded-full border border-line text-ink transition-colors hover:bg-sage"
            aria-label="Close menu"
            autoFocus
          >
            <X className="size-5" aria-hidden="true" />
          </button>
        </div>

        <nav className="flex-1 overflow-y-auto px-5 py-7" aria-label="Mobile">
          <ul className="space-y-1">
            {links.map((link, index) => (
              <li key={link.label}>
                <Link
                  to={link.href}
                  onClick={onClose}
                  className="group flex items-baseline gap-4 border-b border-line/70 py-4"
                >
                  <span className="tnum text-[10px] font-bold text-clay-dark">
                    {String(index + 1).padStart(2, '0')}
                  </span>
                  <span className="text-[1.55rem] font-semibold tracking-[-0.04em] transition-colors group-hover:text-forest">
                    {link.label}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </nav>

        <div className="border-t border-line px-5 py-5">
          <Link
            to={user ? '/account' : '/login'}
            onClick={onClose}
            className="btn btn-primary w-full"
          >
            {user ? `View ${user.name.split(' ')[0]}’s account` : 'Sign in to your account'}
          </Link>
          <p className="mt-4 text-center text-xs text-muted">
            Pharmacist advice line · <a href="tel:+14155550134" className="font-semibold text-ink">(415) 555-0134</a>
          </p>
        </div>
      </div>
    </div>
  );
}
