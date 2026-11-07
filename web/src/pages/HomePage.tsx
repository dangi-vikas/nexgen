import {
  ArrowRight,
  ArrowUpRight,
  Baby,
  Clock,
  Droplets,
  HeartPulse,
  Leaf,
  Moon,
  PackageCheck,
  Pill,
  Search,
  ShieldCheck,
  ShieldPlus,
  Star,
  Sun,
  Truck,
  Upload,
} from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import { Link } from 'react-router-dom';
import { PageMeta } from '@/components/ui/PageMeta';
import { Reveal } from '@/components/ui/Reveal';
import { SectionHeading } from '@/components/ui/SectionHeading';
import { Newsletter } from '@/components/ui/Newsletter';
import { ProductCard } from '@/components/product/ProductCard';
import { articles, categories } from '@/lib/mock';
import { useProducts } from '@/lib/queries';
import { useUiStore } from '@/store/ui';
import type { Category } from '@/lib/types';

const iconMap: Record<Category['icon'], LucideIcon> = {
  droplets: Droplets,
  pill: Pill,
  leaf: Leaf,
  sun: Sun,
  baby: Baby,
  moon: Moon,
  heart: HeartPulse,
  shield: ShieldPlus,
};

const assurances = [
  {
    icon: ShieldCheck,
    title: 'Licensed pharmacists',
    copy: 'Registered in every state we serve.',
  },
  {
    icon: Truck,
    title: 'Free delivery over $35',
    copy: 'Carbon-neutral, letterbox-friendly.',
  },
  {
    icon: PackageCheck,
    title: 'Discreet packaging',
    copy: 'Plain outer box, always.',
  },
  {
    icon: Clock,
    title: 'Same-day pickup',
    copy: 'Order before 4pm, collect today.',
  },
];

const featureSlugs = [
  'barrier-repair-face-oil',
  'everyday-allergy-relief',
  'calm-magnesium-glycinate',
  'spf-50-daily-fluid',
];

const articleIds = ['a-2', 'a-3'];

function Hero() {
  const openSearch = useUiStore((state) => state.openSearch);

  return (
    <section className="noise relative overflow-hidden pb-10 pt-8 sm:pb-14 sm:pt-12 lg:pb-16 lg:pt-14">
      <div className="container-page">
        <div className="grid items-center gap-10 lg:grid-cols-[1.02fr_0.98fr] lg:gap-16">
          <div className="relative z-10">
            <p className="eyebrow text-clay-dark">
              <span className="inline-block h-px w-7 bg-clay-dark/70" aria-hidden="true" />
              Pharmacy, reimagined
            </p>
            <h1 className="display mt-5 text-[clamp(3.15rem,7vw,5.5rem)]">
              Feel better,
              <br />
              <span className="serif text-forest">every day.</span>
            </h1>
            <p className="mt-6 max-w-md text-[15.5px] leading-relaxed text-muted sm:text-base">
              Thoughtful pharmacy care, delivered. Prescriptions, considered essentials and honest
              advice from pharmacists who know your name.
            </p>

            <div className="mt-7 flex flex-wrap items-center gap-3">
              <Link to="/shop" className="btn btn-primary">
                Shop essentials
                <ArrowRight className="size-4" aria-hidden="true" />
              </Link>
              <Link to="/prescriptions" className="btn btn-outline">
                <Upload className="size-4" aria-hidden="true" />
                Upload a prescription
              </Link>
            </div>

            <div className="mt-8 flex flex-wrap items-center gap-x-5 gap-y-2 text-[12.5px] text-muted">
              <span className="flex items-center gap-1.5">
                <span className="flex" aria-hidden="true">
                  {Array.from({ length: 5 }).map((_, index) => (
                    <Star key={index} className="size-3.5 fill-clay text-clay" strokeWidth={0} />
                  ))}
                </span>
                <span className="font-semibold text-ink">4.9</span>
                from 12,000+ patients
              </span>
              <span className="hidden h-3 w-px bg-line-strong sm:block" aria-hidden="true" />
              <span>Independent since 1998</span>
            </div>
          </div>

          <div className="relative mx-auto w-full max-w-[560px] lg:max-w-none">
            <div
              className="organic-mask absolute inset-0 translate-x-4 translate-y-5 bg-sage-deep/45"
              aria-hidden="true"
            />
            <div className="organic-mask relative aspect-[4/4.3] overflow-hidden bg-sage">
              <img
                src="/images/pharmacy-consult.jpg"
                alt="A Northleaf pharmacist talking a customer through her options at the counter"
                width={1600}
                height={1067}
                fetchPriority="high"
                className="size-full object-cover object-[58%_48%]"
              />
              <div className="grain-overlay absolute inset-0" aria-hidden="true" />
              <div className="absolute inset-x-6 bottom-5 flex items-end justify-between gap-4 text-cream sm:inset-x-8 sm:bottom-7">
                <div>
                  <p className="text-[10px] font-bold uppercase tracking-[0.18em] text-cream/70">
                    At the counter
                  </p>
                  <p className="mt-1 text-sm font-semibold tracking-[-0.01em]">
                    Real people, seven days a week
                  </p>
                </div>
                <span className="hidden rounded-full border border-cream/30 bg-ink/20 px-3 py-1.5 text-[10px] font-semibold uppercase tracking-[0.12em] text-cream/85 backdrop-blur-sm sm:inline-block">
                  Est. 1998
                </span>
              </div>
            </div>
            <div
              className="absolute -right-3 top-8 size-20 rounded-full border border-forest/15 sm:size-28"
              aria-hidden="true"
            />
            <span
              className="absolute -right-1 top-16 size-3 rounded-full bg-clay sm:top-24"
              aria-hidden="true"
            />
            <p
              className="absolute -left-10 bottom-32 hidden -rotate-90 text-[9px] font-bold uppercase tracking-[0.32em] text-muted xl:block"
              aria-hidden="true"
            >
              San Francisco · California
            </p>
          </div>
        </div>

        <div className="mt-10 rounded-[26px] border border-line bg-paper p-2 shadow-[0_18px_50px_-24px_rgba(16,35,28,0.25)] sm:mt-12">
          <div className="grid gap-1 md:grid-cols-[1.5fr_1fr_1fr_auto] md:items-stretch">
            <button
              type="button"
              onClick={openSearch}
              className="flex items-center gap-3 rounded-[20px] px-4 py-3 text-left transition-colors hover:bg-sage/35"
            >
              <span className="grid size-10 shrink-0 place-items-center rounded-full bg-sage text-forest">
                <Search className="size-[18px]" strokeWidth={1.8} aria-hidden="true" />
              </span>
              <span>
                <span className="block text-[9.5px] font-bold uppercase tracking-[0.16em] text-muted">
                  Search
                </span>
                <span className="mt-0.5 block text-[13.5px] font-semibold">
                  What can we help with?
                </span>
              </span>
            </button>

            <Link
              to="/help#delivery"
              className="flex items-center gap-3 rounded-[20px] px-4 py-3 transition-colors hover:bg-sage/35 md:border-l md:border-line"
            >
              <Truck className="size-4 shrink-0 text-forest" strokeWidth={1.7} aria-hidden="true" />
              <span>
                <span className="block text-[9.5px] font-bold uppercase tracking-[0.16em] text-muted">
                  Delivery
                </span>
                <span className="mt-0.5 block text-[13.5px] font-semibold">Free over $35</span>
              </span>
            </Link>

            <Link
              to="/pharmacists"
              className="flex items-center gap-3 rounded-[20px] px-4 py-3 transition-colors hover:bg-sage/35 md:border-l md:border-line"
            >
              <HeartPulse
                className="size-4 shrink-0 text-forest"
                strokeWidth={1.7}
                aria-hidden="true"
              />
              <span>
                <span className="block text-[9.5px] font-bold uppercase tracking-[0.16em] text-muted">
                  Consultation
                </span>
                <span className="mt-0.5 block text-[13.5px] font-semibold">Book a pharmacist</span>
              </span>
            </Link>

            <button
              type="button"
              onClick={openSearch}
              className="btn btn-primary m-1 w-[calc(100%-0.5rem)] md:w-auto"
            >
              Find care
              <ArrowRight className="size-4" aria-hidden="true" />
            </button>
          </div>
        </div>

        <div className="mt-4 flex flex-wrap items-center gap-x-3 gap-y-1.5 px-1 text-[12px] text-muted">
          <span className="font-semibold text-ink">Popular</span>
          {['Allergy relief', 'Sleep support', 'Vitamin D', 'Skin barrier'].map((term) => (
            <Link key={term} to="/shop" className="transition-colors hover:text-clay-dark">
              {term}
            </Link>
          ))}
        </div>
      </div>
    </section>
  );
}

function AssuranceStrip() {
  return (
    <section className="border-y border-line bg-paper/55">
      <div className="container-page grid grid-cols-1 gap-0 sm:grid-cols-2 lg:grid-cols-4">
        {assurances.map(({ icon: Icon, title, copy }, index) => (
          <div
            key={title}
            className={`flex items-start gap-3.5 py-6 sm:px-5 lg:px-7 ${
              index > 0 ? 'border-t border-line sm:border-t-0 sm:border-l' : ''
            } ${index === 2 ? 'sm:border-l-0 lg:border-l' : ''} ${index === 1 ? 'sm:pl-0 lg:pl-7' : ''}`}
          >
            <Icon className="mt-0.5 size-5 shrink-0 text-forest" strokeWidth={1.6} aria-hidden="true" />
            <div>
              <p className="text-[13.5px] font-semibold tracking-[-0.01em]">{title}</p>
              <p className="mt-0.5 text-[12px] leading-snug text-muted">{copy}</p>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}

function Goals() {
  return (
    <section id="goals" className="scroll-mt-28 py-16 sm:py-20 lg:py-24">
      <div className="container-page">
        <Reveal>
          <SectionHeading
            eyebrow="Shop by need"
            title={
              <>
                Start with how <span className="serif text-forest">you feel.</span>
              </>
            }
            description="Eight considered collections, edited by our pharmacists. No endless aisles."
            action={{ label: 'Browse all products', href: '/shop' }}
          />
        </Reveal>

        <Reveal delay={80}>
          <div className="mt-10 flex gap-4 overflow-x-auto pb-3 scrollbar-hide sm:grid sm:grid-cols-4 sm:gap-x-4 sm:gap-y-7 sm:overflow-visible lg:grid-cols-8">
            {categories.map((category) => {
              const Icon = iconMap[category.icon];
              return (
                <Link
                  key={category.id}
                  to={`/shop?category=${category.id}`}
                  className="group flex w-[92px] shrink-0 flex-col items-center text-center sm:w-auto"
                >
                  <span className="grid size-[76px] place-items-center rounded-full border border-line bg-paper text-forest transition-all duration-300 group-hover:-translate-y-1 group-hover:border-forest group-hover:bg-forest group-hover:text-cream group-hover:shadow-[0_12px_30px_-12px_rgba(27,67,52,0.55)] sm:size-[84px]">
                    <Icon className="size-6" strokeWidth={1.5} aria-hidden="true" />
                  </span>
                  <span className="mt-3 text-[13px] font-semibold tracking-[-0.01em]">
                    {category.label}
                  </span>
                  <span className="mt-0.5 text-[11px] text-muted">{category.blurb}</span>
                </Link>
              );
            })}
          </div>
        </Reveal>
      </div>
    </section>
  );
}

function ShelfFavourites() {
  const products = useProducts();
  const featured = featureSlugs
    .map((slug) => products.find((product) => product.slug === slug))
    .filter((product): product is NonNullable<typeof product> => Boolean(product));

  return (
    <section className="pb-16 sm:pb-20 lg:pb-24">
      <div className="container-page">
        <Reveal>
          <SectionHeading
            eyebrow="Shelf favourites"
            title={
              <>
                What people <span className="serif text-forest">come back for.</span>
              </>
            }
            description="The quiet staples our patients reorder without thinking. Tried, trusted, never pushed."
            action={{ label: 'Shop all products', href: '/shop' }}
          />
        </Reveal>
        <div className="mt-10 grid grid-cols-2 gap-x-4 gap-y-10 sm:gap-x-5 md:grid-cols-4">
          {featured.map((product, index) => (
            <Reveal key={product.id} delay={index * 70}>
              <ProductCard product={product} priority={index < 2} />
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}

function Story() {
  return (
    <section id="story" className="scroll-mt-28 border-t border-line py-16 sm:py-20 lg:py-24">
      <div className="container-page grid items-center gap-10 lg:grid-cols-[0.95fr_1.05fr] lg:gap-16">
        <Reveal>
          <div className="relative">
            <div
              className="absolute -bottom-4 -left-4 h-[85%] w-[85%] rounded-[36px] border border-forest/12"
              aria-hidden="true"
            />
            <div className="relative aspect-[5/4.4] overflow-hidden rounded-[36px] bg-sage">
              <img
                src="/images/hero-pharmacy.jpg"
                alt="A pharmacist at Northleaf reaching for a preparation on the wooden shelves"
                width={1800}
                height={1200}
                loading="lazy"
                className="size-full object-cover object-[62%_45%]"
              />
            </div>
            <div className="absolute -bottom-5 right-5 rounded-2xl border border-line bg-paper px-4 py-3 shadow-soft sm:right-8">
              <p className="text-[10px] font-bold uppercase tracking-[0.16em] text-muted">
                Independently owned
              </p>
              <p className="mt-1 text-[13px] font-semibold">Same family since 1998</p>
            </div>
          </div>
        </Reveal>

        <Reveal delay={100}>
          <p className="eyebrow text-clay-dark">
            <span className="inline-block h-px w-7 bg-clay-dark/70" aria-hidden="true" />
            Our story
          </p>
          <h2 className="display mt-4 text-[2rem] leading-[1.03] sm:text-[2.7rem] lg:text-[3rem]">
            A pharmacy that still
            <br className="hidden sm:block" /> knows <span className="serif text-forest">your name.</span>
          </h2>
          <p className="mt-5 max-w-lg text-[15px] leading-relaxed text-muted">
            Northleaf began behind a single wooden counter in 1998. Three decades later the shelves
            are deeper and the delivery van is electric, but the idea has not moved: good pharmacy
            is a conversation, not a transaction.
          </p>
          <p className="mt-3.5 max-w-lg text-[15px] leading-relaxed text-muted">
            Our pharmacists are on the floor, on the phone and on your doorstep. They will tell you
            when a supplement is worth it, and when a walk would do more.
          </p>
          <div className="mt-6 flex items-center gap-4">
            <div className="h-10 w-px bg-line-strong" aria-hidden="true" />
            <div>
              <p className="serif text-lg text-forest">Amara Osei</p>
              <p className="text-[11px] font-semibold uppercase tracking-[0.12em] text-muted">
                Lead pharmacist
              </p>
            </div>
          </div>
          <Link to="/pharmacists" className="link-quiet mt-6">
            Meet the team
            <ArrowRight className="size-4" aria-hidden="true" />
          </Link>
        </Reveal>
      </div>
    </section>
  );
}

function Journal() {
  return (
    <section id="journal" className="scroll-mt-28 pb-16 sm:pb-20 lg:pb-24">
      <div className="container-page">
        <Reveal>
          <div className="noise relative overflow-hidden rounded-[36px] bg-sage p-6 sm:p-9 lg:p-12">
            <svg
              className="pointer-events-none absolute -right-10 -top-10 size-[300px] text-forest/10"
              viewBox="0 0 200 200"
              fill="none"
              aria-hidden="true"
            >
              <path
                d="M168 22C95 28 52 67 47 130c-2 24 8 42 22 54 15 12 36 17 55 10 53-17 59-82 44-172Z"
                stroke="currentColor"
                strokeWidth="1.2"
              />
              <path d="M42 184C64 129 101 91 154 63" stroke="currentColor" strokeWidth="1" />
            </svg>

            <div className="relative grid gap-10 lg:grid-cols-[0.78fr_1.22fr] lg:gap-14">
              <div className="lg:pt-2">
                <p className="eyebrow text-forest/85">The Northleaf journal</p>
                <h2 className="display mt-4 text-[2rem] leading-[1.03] text-ink sm:text-[2.6rem]">
                  Notes on feeling well,
                  <br className="hidden sm:block" /> not just{' '}
                  <span className="serif text-forest">not-sick.</span>
                </h2>
                <p className="mt-4 max-w-sm text-[14.5px] leading-relaxed text-muted">
                  Plain-language writing from behind the counter. Evidence over noise, always.
                </p>
                <Link to="/journal" className="link-quiet mt-6 text-forest">
                  Read the journal
                  <ArrowRight className="size-4" aria-hidden="true" />
                </Link>
              </div>

              <div className="grid gap-6 sm:grid-cols-2">
                {articleIds.map((id) => {
                  const article = articles.find((item) => item.id === id);
                  if (!article) return null;
                  return (
                    <Link key={article.id} to={`/journal/${article.slug}`} className="group flex flex-col">
                      <div className="aspect-[4/3] overflow-hidden rounded-2xl bg-cream-deep">
                        <img
                          src={article.image}
                          alt={article.title}
                          width={800}
                          height={600}
                          loading="lazy"
                          className="size-full object-cover transition-transform duration-700 ease-[cubic-bezier(.22,1,.36,1)] group-hover:scale-[1.04]"
                        />
                      </div>
                      <div className="mt-3.5 flex items-center gap-2 text-[10px] font-bold uppercase tracking-[0.14em] text-forest/85">
                        <span>{article.category}</span>
                        <span aria-hidden="true">·</span>
                        <span>{article.readTime}</span>
                      </div>
                      <h3 className="mt-1.5 text-[1.05rem] font-semibold leading-snug tracking-[-0.025em] text-ink transition-colors group-hover:text-forest">
                        {article.title}
                      </h3>
                      <p className="mt-1.5 line-clamp-2 text-[12.5px] leading-relaxed text-muted">
                        {article.excerpt}
                      </p>
                    </Link>
                  );
                })}
              </div>
            </div>
          </div>
        </Reveal>
      </div>
    </section>
  );
}

function OfferBanner() {
  return (
    <section className="pb-16 sm:pb-20 lg:pb-24">
      <div className="container-page">
        <Reveal>
          <div className="relative overflow-hidden rounded-[36px] bg-forest">
            <img
              src="/images/herbs-dark.jpg"
              alt=""
              width={1800}
              height={1200}
              loading="lazy"
              className="absolute inset-0 size-full object-cover opacity-75"
            />
            <div
              className="absolute inset-0 bg-gradient-to-r from-forest via-forest/88 to-forest/15"
              aria-hidden="true"
            />
            <div className="relative max-w-xl px-7 py-12 text-cream sm:px-10 sm:py-16 lg:px-14 lg:py-20">
              <p className="eyebrow text-sage">
                <span className="inline-block h-px w-7 bg-sage/60" aria-hidden="true" />
                A small welcome
              </p>
              <h2 className="display mt-4 text-[2.2rem] leading-[1.02] sm:text-[3rem] lg:text-[3.4rem]">
                Your first refill
                <br />
                is <span className="serif text-clay-soft">on us.</span>
              </h2>
              <p className="mt-4 max-w-sm text-[14.5px] leading-relaxed text-cream/70">
                Transfer a prescription before the end of the month and we’ll waive the dispensing
                fee. One less thing to think about.
              </p>
              <div className="mt-6 flex flex-wrap items-center gap-3">
                <Link to="/prescriptions" className="btn btn-clay">
                  Upload a prescription
                  <ArrowUpRight className="size-4" aria-hidden="true" />
                </Link>
                <span className="rounded-full border border-cream/25 px-3.5 py-2 text-[11px] font-bold uppercase tracking-[0.16em] text-cream/70">
                  Code NEWLEAF
                </span>
              </div>
            </div>
          </div>
        </Reveal>
      </div>
    </section>
  );
}

function Stats() {
  const stats = [
    { value: '28', unit: 'years', label: 'On the same high street' },
    { value: '40k', unit: 'patients', label: 'Cared for, and counting' },
    { value: '4.9', unit: '/ 5', label: 'Average patient rating' },
    { value: '24/7', unit: 'advice', label: 'A pharmacist on the line' },
  ];

  return (
    <section className="border-y border-line bg-paper/60">
      <div className="container-page grid grid-cols-2 gap-0 py-10 sm:py-12 lg:grid-cols-4">
        {stats.map((stat, index) => (
          <div
            key={stat.label}
            className={`px-4 py-5 sm:px-6 lg:px-8 ${
              index % 2 === 1 ? 'border-l border-line' : ''
            } ${index > 1 ? 'border-t border-line lg:border-t-0' : ''} ${
              index === 2 ? 'lg:border-l' : ''
            }`}
          >
            <p className="flex items-baseline gap-1.5">
              <span className="display tnum text-[2.6rem] leading-none sm:text-[3.2rem]">
                {stat.value}
              </span>
              <span className="text-[10px] font-bold uppercase tracking-[0.14em] text-muted">
                {stat.unit}
              </span>
            </p>
            <p className="mt-2 text-[12.5px] text-muted">{stat.label}</p>
          </div>
        ))}
      </div>
    </section>
  );
}

function NewsletterSection() {
  return (
    <section className="py-16 sm:py-20 lg:py-24">
      <div className="container-page">
        <Reveal>
          <div className="mx-auto max-w-2xl text-center">
            <span className="mx-auto grid size-12 place-items-center rounded-full border border-line bg-paper text-forest">
              <Leaf className="size-5" strokeWidth={1.5} aria-hidden="true" />
            </span>
            <p className="eyebrow mt-5 justify-center text-clay-dark">The monthly letter</p>
            <h2 className="display mt-4 text-[2.2rem] leading-[1.02] sm:text-[3rem]">
              A little letter, <span className="serif text-forest">once a month.</span>
            </h2>
            <p className="mx-auto mt-4 max-w-md text-[14.5px] leading-relaxed text-muted">
              Seasonal notes, new arrivals and the occasional honest recommendation. No noise, no
              daily discounts.
            </p>
            <div className="mx-auto mt-7 max-w-md">
              <Newsletter compact />
            </div>
          </div>
        </Reveal>
      </div>
    </section>
  );
}

export function HomePage() {
  return (
    <>
      <PageMeta
        title="Northleaf — Care that feels human."
        description="A modern pharmacy for everyday health. Prescriptions, wellness essentials and honest advice from licensed pharmacists."
      />
      <Hero />
      <AssuranceStrip />
      <Goals />
      <ShelfFavourites />
      <Story />
      <Journal />
      <OfferBanner />
      <Stats />
      <NewsletterSection />
    </>
  );
}
