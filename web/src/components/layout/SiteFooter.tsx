import { Clock, MapPin, Phone } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Logo } from '@/components/ui/Logo';

const columns = [
  {
    title: 'Shop',
    links: [
      { label: 'All products', href: '/shop' },
      { label: 'Skin', href: '/shop?category=skin' },
      { label: 'Supplements', href: '/shop?category=supplements' },
      { label: 'Sleep', href: '/shop?category=sleep' },
      { label: 'Herbal', href: '/shop?category=herbal' },
    ],
  },
  {
    title: 'Care',
    links: [
      { label: 'Prescriptions', href: '/prescriptions' },
      { label: 'Pharmacist advice', href: '/pharmacists' },
      { label: 'Book a consultation', href: '/pharmacists#book' },
      { label: 'Delivery & returns', href: '/help#delivery' },
      { label: 'Help & contact', href: '/help' },
    ],
  },
  {
    title: 'Company',
    links: [
      { label: 'Our story', href: '/#story' },
      { label: 'Journal', href: '/journal' },
      { label: 'Careers', href: '/help#careers' },
      { label: 'Accessibility', href: '/help#accessibility' },
      { label: 'Privacy', href: '/help#privacy' },
    ],
  },
];

export function SiteFooter() {
  return (
    <footer className="bg-ink text-cream">
      <div className="container-page pb-10 pt-16 lg:pt-20">
        <div className="grid grid-cols-2 gap-x-6 gap-y-10 sm:grid-cols-3 lg:grid-cols-[1.45fr_repeat(3,1fr)] lg:gap-8">
          <div className="col-span-2 max-w-sm sm:col-span-3 lg:col-span-1">
            <Logo tone="light" withDescriptor />
            <p className="mt-5 text-sm leading-relaxed text-cream/60">
              A modern pharmacy for everyday health. Prescriptions, considered essentials and honest
              advice from pharmacists who know your name.
            </p>
            <ul className="mt-7 space-y-3 text-[13px] text-cream/70">
              <li className="flex items-start gap-2.5">
                <MapPin className="mt-0.5 size-4 shrink-0 text-sage" strokeWidth={1.6} aria-hidden="true" />
                <span>
                  48 Alder Street, Suite 2<br />
                  San Francisco, CA 94110
                </span>
              </li>
              <li className="flex items-center gap-2.5">
                <Phone className="size-4 shrink-0 text-sage" strokeWidth={1.6} aria-hidden="true" />
                <a href="tel:+14155550134" className="transition-colors hover:text-cream">
                  (415) 555-0134
                </a>
              </li>
              <li className="flex items-center gap-2.5">
                <Clock className="size-4 shrink-0 text-sage" strokeWidth={1.6} aria-hidden="true" />
                <span>Mon–Sat 8am–8pm · Sun 10am–5pm</span>
              </li>
            </ul>
          </div>

          {columns.map((column, index) => (
            <div key={column.title} className={index === 2 ? 'col-span-2 sm:col-span-1' : undefined}>
              <h2 className="text-[11px] font-bold uppercase tracking-[0.18em] text-cream/60">
                {column.title}
              </h2>
              <ul className="mt-5 space-y-3">
                {column.links.map((link) => (
                  <li key={link.label}>
                    <Link
                      to={link.href}
                      className="text-[13.5px] text-cream/75 transition-colors hover:text-clay-soft"
                    >
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="mt-14 flex flex-col gap-5 border-t border-cream/10 pt-6 text-[11.5px] text-cream/60 lg:flex-row lg:items-center lg:justify-between">
          <p>© 2026 Northleaf Pharmacy. A demonstration storefront.</p>
          <div className="flex flex-wrap items-center gap-x-5 gap-y-2">
            <Link to="/help#privacy" className="transition-colors hover:text-cream">
              Privacy
            </Link>
            <Link to="/help#terms" className="transition-colors hover:text-cream">
              Terms
            </Link>
            <Link to="/help#accessibility" className="transition-colors hover:text-cream">
              Accessibility
            </Link>
            <span className="hidden h-3 w-px bg-cream/15 sm:block" aria-hidden="true" />
            <div className="flex items-center gap-1.5" aria-label="Accepted payment methods">
              {['Visa', 'Mastercard', 'Amex', 'Apple Pay'].map((method) => (
                <span
                  key={method}
                  className="rounded-[5px] border border-cream/15 px-2 py-0.5 text-[9px] font-bold uppercase tracking-wide text-cream/70"
                >
                  {method}
                </span>
              ))}
            </div>
          </div>
        </div>
      </div>
    </footer>
  );
}
