import { ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import { PageMeta } from '@/components/ui/PageMeta';
import { Breadcrumbs } from '@/components/ui/Breadcrumbs';
import { articles } from '@/lib/mock';
import { formatDate } from '@/lib/utils';

export function JournalPage() {
  const [featured, ...rest] = articles;

  return (
    <>
      <PageMeta
        title="The NexGen journal — Notes on feeling well"
        description="Plain-language writing from behind the pharmacy counter: skin, sleep, supplements and everyday health."
      />

      <section className="border-b border-line bg-sage/30 py-10 sm:py-14">
        <div className="container-page">
          <Breadcrumbs items={[{ label: 'Journal' }]} />
          <div className="mt-6 grid gap-8 lg:grid-cols-[1.25fr_0.75fr] lg:items-end">
            <div>
              <p className="eyebrow text-clay-dark">
                <span className="inline-block h-px w-7 bg-clay-dark/70" aria-hidden="true" />
                The NexGen journal
              </p>
              <h1 className="display mt-4 max-w-2xl text-[2.7rem] leading-[1.01] sm:text-[3.6rem]">
                Notes on feeling well,
                <br />
                not just <span className="serif text-forest">not-sick.</span>
              </h1>
            </div>
            <p className="max-w-sm text-[14.5px] leading-relaxed text-muted">
              Plain-language writing from behind the counter. Evidence over noise, and no advice
              that begins with buying something.
            </p>
          </div>
        </div>
      </section>

      <section className="py-10 sm:py-14">
        <div className="container-page">
          {featured ? (
            <Link
              to={`/journal/${featured.slug}`}
              className="group grid gap-7 rounded-[34px] border border-line bg-paper p-4 sm:p-6 lg:grid-cols-[1.05fr_0.95fr] lg:items-center lg:gap-12 lg:p-7"
            >
              <div className="aspect-[4/3] overflow-hidden rounded-[26px] bg-sage">
                <img
                  src={featured.image}
                  alt={featured.title}
                  width={900}
                  height={675}
                  className="size-full object-cover transition-transform duration-700 ease-[cubic-bezier(.22,1,.36,1)] group-hover:scale-[1.03]"
                />
              </div>
              <div className="px-1 pb-2 lg:px-2 lg:py-4">
                <div className="flex flex-wrap items-center gap-2 text-[10px] font-bold uppercase tracking-[0.14em] text-forest/85">
                  <span>{featured.category}</span>
                  <span aria-hidden="true">·</span>
                  <span>{featured.readTime}</span>
                  <span aria-hidden="true">·</span>
                  <span>{formatDate(featured.date)}</span>
                </div>
                <h2 className="display mt-3 text-[2rem] leading-[1.03] sm:text-[2.6rem]">
                  {featured.title}
                </h2>
                <p className="mt-4 max-w-lg text-[14.5px] leading-relaxed text-muted">
                  {featured.excerpt}
                </p>
                <span className="link-quiet mt-6">
                  Read the piece
                  <ArrowRight className="size-4" aria-hidden="true" />
                </span>
              </div>
            </Link>
          ) : null}

          <div className="mt-8 grid gap-6 sm:grid-cols-2">
            {rest.map((article) => (
              <Link
                key={article.id}
                to={`/journal/${article.slug}`}
                className="group flex flex-col rounded-[30px] border border-line bg-paper p-4 sm:p-5"
              >
                <div className="aspect-[16/10] overflow-hidden rounded-[22px] bg-sage">
                  <img
                    src={article.image}
                    alt={article.title}
                    width={800}
                    height={500}
                    loading="lazy"
                    className="size-full object-cover transition-transform duration-700 ease-[cubic-bezier(.22,1,.36,1)] group-hover:scale-[1.03]"
                  />
                </div>
                <div className="flex flex-1 flex-col px-1 pb-1 pt-4">
                  <div className="flex flex-wrap items-center gap-2 text-[10px] font-bold uppercase tracking-[0.14em] text-forest/85">
                    <span>{article.category}</span>
                    <span aria-hidden="true">·</span>
                    <span>{article.readTime}</span>
                  </div>
                  <h2 className="mt-2 text-[1.35rem] font-semibold leading-snug tracking-[-0.03em] transition-colors group-hover:text-forest">
                    {article.title}
                  </h2>
                  <p className="mt-2 line-clamp-2 text-[13px] leading-relaxed text-muted">
                    {article.excerpt}
                  </p>
                  <span className="link-quiet mt-4 text-[12.5px]">
                    Read
                    <ArrowRight className="size-3.5" aria-hidden="true" />
                  </span>
                </div>
              </Link>
            ))}
          </div>

          <div className="mt-10 flex flex-col items-start justify-between gap-4 rounded-[26px] bg-sage/55 p-5 sm:flex-row sm:items-center sm:p-6">
            <div>
              <p className="text-[15px] font-semibold tracking-[-0.02em]">A question the article didn’t answer?</p>
              <p className="mt-1 text-[12.5px] text-muted">
                A pharmacist can talk it through with you, free of charge.
              </p>
            </div>
            <Link to="/pharmacists" className="btn btn-primary shrink-0">
              Ask a pharmacist
              <ArrowRight className="size-4" aria-hidden="true" />
            </Link>
          </div>
        </div>
      </section>
    </>
  );
}
