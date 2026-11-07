import { useState } from 'react';
import { ArrowUpRight, Menu, Search, ShoppingBag, Truck, UserRound } from 'lucide-react';
import { Link, NavLink } from 'react-router-dom';
import { Logo } from '@/components/ui/Logo';
import { MobileNav } from '@/components/layout/MobileNav';
import { useCartCount, useCartStore } from '@/store/cart';
import { useUiStore } from '@/store/ui';

const navItems = [
  { label: 'Shop', href: '/shop' },
  { label: 'Prescriptions', href: '/prescriptions' },
  { label: 'Pharmacists', href: '/pharmacists' },
  { label: 'Health goals', href: '/#goals' },
  { label: 'Journal', href: '/journal' },
];

export function SiteHeader() {
  const [menuOpen, setMenuOpen] = useState(false);
  const cartCount = useCartCount();
  const openCart = useCartStore((state) => state.open);
  const openSearch = useUiStore((state) => state.openSearch);
  const user = useUiStore((state) => state.user);

  return (
    <>
      <div className="bg-forest text-cream">
        <div className="container-page flex h-9 items-center justify-between gap-4 text-[11px] font-medium">
          <p className="flex items-center gap-1.5">
            <Truck className="size-3.5 text-sage" strokeWidth={1.8} aria-hidden="true" />
            Free delivery over $35
          </p>
          <p className="hidden text-cream/70 md:block">
            Same-day pickup before 4pm · Licensed pharmacists, seven days a week
          </p>
          <Link
            to="/prescriptions"
            className="inline-flex items-center gap-1 text-sage transition-colors hover:text-cream"
          >
            Upload a prescription
            <ArrowUpRight className="size-3" aria-hidden="true" />
          </Link>
        </div>
      </div>

      <header className="sticky top-0 z-40 border-b border-line/80 bg-cream/95 backdrop-blur-md">
        <div className="container-page flex h-[74px] items-center justify-between gap-3 sm:gap-6">
          <Logo withDescriptor descriptorClassName="hidden sm:block" />

          <nav className="hidden items-center gap-7 lg:flex" aria-label="Primary">
            {navItems.map((item) =>
              item.href.includes('#') ? (
                <Link key={item.label} to={item.href} className="nav-link">
                  {item.label}
                </Link>
              ) : (
                <NavLink key={item.label} to={item.href} className="nav-link">
                  {item.label}
                </NavLink>
              ),
            )}
          </nav>

          <div className="flex items-center gap-1.5 sm:gap-2">
            <button
              type="button"
              onClick={openSearch}
              className="hidden h-10 items-center gap-2 rounded-full border border-line bg-paper/70 pl-3.5 pr-4 text-[13px] font-medium text-muted transition-colors hover:border-line-strong hover:text-ink sm:inline-flex"
              aria-label="Search products"
            >
              <Search className="size-4" strokeWidth={1.8} aria-hidden="true" />
              Search
            </button>
            <button
              type="button"
              onClick={openSearch}
              className="grid size-10 place-items-center rounded-full text-ink transition-colors hover:bg-sage sm:hidden"
              aria-label="Search products"
            >
              <Search className="size-[19px]" strokeWidth={1.7} aria-hidden="true" />
            </button>

            <Link
              to="/account"
              className="hidden size-10 place-items-center rounded-full text-ink transition-colors hover:bg-sage sm:grid"
              aria-label={user ? `ME — ${user.name}’s account` : 'Sign in'}
            >
              {user ? (
                <span className="grid size-7 place-items-center rounded-full bg-sage text-[11px] font-bold text-forest">
                  {user.name
                    .split(' ')
                    .map((part) => part[0])
                    .join('')
                    .slice(0, 2)}
                </span>
              ) : (
                <UserRound className="size-[19px]" strokeWidth={1.7} aria-hidden="true" />
              )}
            </Link>

            <button
              type="button"
              onClick={openCart}
              className="relative grid size-10 place-items-center rounded-full text-ink transition-colors hover:bg-sage"
              aria-label={`Open bag, ${cartCount} ${cartCount === 1 ? 'item' : 'items'}`}
            >
              <ShoppingBag className="size-[19px]" strokeWidth={1.7} aria-hidden="true" />
              {cartCount > 0 ? (
                <span className="tnum absolute right-0.5 top-0.5 grid min-w-[17px] place-items-center rounded-full bg-clay px-1 text-[10px] font-bold leading-[17px] text-ink">
                  {cartCount > 9 ? '9+' : cartCount}
                </span>
              ) : null}
            </button>

            <button
              type="button"
              onClick={() => setMenuOpen(true)}
              className="grid size-10 place-items-center rounded-full text-ink transition-colors hover:bg-sage lg:hidden"
              aria-label="Open menu"
            >
              <Menu className="size-5" strokeWidth={1.7} aria-hidden="true" />
            </button>
          </div>
        </div>
      </header>

      <MobileNav open={menuOpen} onClose={() => setMenuOpen(false)} />
    </>
  );
}
