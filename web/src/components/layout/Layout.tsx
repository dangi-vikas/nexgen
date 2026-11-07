import { Suspense, useEffect } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { SiteHeader } from '@/components/layout/SiteHeader';
import { SiteFooter } from '@/components/layout/SiteFooter';
import { CartDrawer } from '@/components/layout/CartDrawer';
import { SearchOverlay } from '@/components/layout/SearchOverlay';
import { Toast } from '@/components/layout/Toast';

function ScrollManager() {
  const { pathname, hash } = useLocation();

  useEffect(() => {
    if (hash) {
      const id = decodeURIComponent(hash.slice(1));
      const frame = window.requestAnimationFrame(() => {
        const target = document.getElementById(id);
        if (target) {
          target.scrollIntoView({ behavior: 'smooth', block: 'start' });
          return;
        }
        window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
      });
      return () => window.cancelAnimationFrame(frame);
    }
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
    return undefined;
  }, [pathname, hash]);

  return null;
}

function PageLoader() {
  return (
    <div className="container-page py-16" aria-busy="true" aria-live="polite">
      <div className="h-3 w-28 animate-pulse rounded-full bg-sage" />
      <div className="mt-5 h-12 w-2/3 max-w-md animate-pulse rounded-2xl bg-sage/80" />
      <div className="mt-3 h-3 w-1/2 max-w-sm animate-pulse rounded-full bg-sage/70" />
      <div className="mt-10 grid grid-cols-2 gap-5 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, index) => (
          <div key={index} className="space-y-3">
            <div className="aspect-[4/5] animate-pulse rounded-[26px] bg-sage/60" />
            <div className="h-3 w-2/3 animate-pulse rounded-full bg-sage/70" />
            <div className="h-3 w-1/2 animate-pulse rounded-full bg-sage/50" />
          </div>
        ))}
      </div>
    </div>
  );
}

export function Layout() {
  return (
    <div className="flex min-h-screen flex-col">
      <a
        href="#main"
        className="sr-only z-[70] focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:inline-flex focus:items-center focus:rounded-full focus:bg-forest focus:px-5 focus:py-3 focus:text-sm focus:font-bold focus:text-cream"
      >
        Skip to content
      </a>
      <ScrollManager />
      <SiteHeader />
      <main id="main" className="flex-1">
        <Suspense fallback={<PageLoader />}>
          <Outlet />
        </Suspense>
      </main>
      <SiteFooter />
      <CartDrawer />
      <SearchOverlay />
      <Toast />
    </div>
  );
}
