import { ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import { PageMeta } from '@/components/ui/PageMeta';

export function NotFoundPage() {
  return (
    <>
      <PageMeta title="Page not found — NexGen Pharmacy" />
      <div className="container-page flex min-h-[60vh] flex-col items-center justify-center py-16 text-center">
        <p className="eyebrow text-clay-dark">404</p>
        <h1 className="display mt-3 text-[2.8rem] sm:text-[4rem]">
          This shelf is <span className="serif text-forest">empty.</span>
        </h1>
        <p className="mt-4 max-w-md text-[14.5px] leading-relaxed text-muted">
          The page you were looking for has moved, or never existed. The rest of the pharmacy is
          right where you left it.
        </p>
        <div className="mt-7 flex flex-wrap justify-center gap-3">
          <Link to="/" className="btn btn-primary">
            Back to home
            <ArrowRight className="size-4" aria-hidden="true" />
          </Link>
          <Link to="/shop" className="btn btn-outline">
            Shop all products
          </Link>
        </div>
      </div>
    </>
  );
}
