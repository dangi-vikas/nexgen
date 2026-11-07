import { ArrowLeft, ArrowRight, Clock } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { PageMeta } from '@/components/ui/PageMeta';
import { Breadcrumbs } from '@/components/ui/Breadcrumbs';
import { articles } from '@/lib/mock';
import { formatDate } from '@/lib/utils';

export function ArticlePage() {
  const { slug } = useParams<{ slug: string }>();
  const article = articles.find((item) => item.slug === slug);

  if (!article) {
    return (
      <div className="container-page py-16">
        <div className="mx-auto max-w-md text-center">
          <p className="eyebrow text-clay-dark">The journal</p>
          <h1 className="display mt-3 text-[2.4rem]">That page has been <span className="serif text-forest">filed away.</span></h1>
          <p className="mt-3 text-sm leading-relaxed text-muted">
            We couldn’t find that piece. The rest of the journal is still on the shelf.
          </p>
          <Link to="/journal" className="btn btn-primary mt-6">
            <ArrowLeft className="size-4" aria-hidden="true" />
            Back to the journal
          </Link>
        </div>
      </div>
    );
  }

  const related = articles.filter((item) => item.id !== article.id).slice(0, 2);

  return (
    <>
      <PageMeta
        title={`${article.title} — NexGen Journal`}
        description={article.excerpt}
      />

      <article>
        <header className="border-b border-line bg-sage/25 py-9 sm:py-12">
          <div className="container-page">
            <Breadcrumbs
              items={[
                { label: 'Journal', href: '/journal' },
                { label: article.title },
              ]}
            />
            <div className="mt-6 max-w-3xl">
              <div className="flex flex-wrap items-center gap-2 text-[10px] font-bold uppercase tracking-[0.14em] text-forest/85">
                <span>{article.category}</span>
                <span aria-hidden="true">·</span>
                <span>{article.readTime}</span>
                <span aria-hidden="true">·</span>
                <span>{formatDate(article.date)}</span>
              </div>
              <h1 className="display mt-4 text-[2.5rem] leading-[1.01] sm:text-[3.4rem]">
                {article.title}
              </h1>
              <p className="mt-4 max-w-2xl text-[15px] leading-relaxed text-muted">
                {article.excerpt}
              </p>
            </div>
          </div>
        </header>

        <div className="container-page py-8 sm:py-10">
          <div className="aspect-[16/8.5] overflow-hidden rounded-[32px] bg-sage sm:aspect-[16/7]">
            <img
              src={article.image}
              alt={article.title}
              width={1600}
              height={900}
              className="size-full object-cover"
            />
          </div>

          <div className="mx-auto mt-8 max-w-2xl sm:mt-10">
            {article.body.map((paragraph, index) => (
              <p
                key={paragraph}
                className={
                  index === 0
                    ? 'text-[17px] leading-[1.75] text-ink/85 sm:text-[18px]'
                    : 'mt-5 text-[15px] leading-[1.8] text-ink/75'
                }
              >
                {paragraph}
              </p>
            ))}

            <div className="mt-9 flex items-start gap-3 rounded-2xl border border-line bg-paper p-5">
              <span className="grid size-10 shrink-0 place-items-center rounded-full bg-sage text-forest">
                <Clock className="size-4" aria-hidden="true" />
              </span>
              <div>
                <p className="text-[13px] font-semibold">A note on this piece</p>
                <p className="mt-1 text-[12.5px] leading-relaxed text-muted">
                  Journal articles are general information, not a diagnosis or a prescription. If
                  something is worrying you, book time with a pharmacist or speak to your doctor.
                </p>
              </div>
            </div>

            <div className="mt-8 flex flex-col gap-4 border-t border-line pt-6 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <p className="text-[13px] font-semibold">Written by the NexGen pharmacy team</p>
                <p className="mt-0.5 text-[12px] text-muted">Reviewed by Amara Osei, lead pharmacist.</p>
              </div>
              <Link to="/pharmacists" className="link-quiet shrink-0">
                Meet the team
                <ArrowRight className="size-4" aria-hidden="true" />
              </Link>
            </div>
          </div>
        </div>
      </article>

      <section className="border-t border-line py-12 sm:py-16">
        <div className="container-page">
          <div className="flex items-end justify-between gap-4">
            <div>
              <p className="eyebrow text-clay-dark">Keep reading</p>
              <h2 className="display mt-2 text-[1.9rem] sm:text-[2.4rem]">
                More from the <span className="serif text-forest">journal.</span>
              </h2>
            </div>
            <Link to="/journal" className="link-quiet hidden sm:inline-flex">
              All articles
              <ArrowRight className="size-4" aria-hidden="true" />
            </Link>
          </div>
          <div className="mt-7 grid gap-6 sm:grid-cols-2">
            {related.map((item) => (
              <Link
                key={item.id}
                to={`/journal/${item.slug}`}
                className="group grid gap-5 rounded-[26px] border border-line bg-paper p-4 sm:grid-cols-[0.8fr_1.2fr] sm:items-center"
              >
                <div className="aspect-[4/3] overflow-hidden rounded-[18px] bg-sage">
                  <img
                    src={item.image}
                    alt={item.title}
                    width={600}
                    height={450}
                    loading="lazy"
                    className="size-full object-cover transition-transform duration-700 ease-[cubic-bezier(.22,1,.36,1)] group-hover:scale-[1.03]"
                  />
                </div>
                <div className="px-1 pb-1">
                  <p className="text-[10px] font-bold uppercase tracking-[0.14em] text-forest/85">
                    {item.category} · {item.readTime}
                  </p>
                  <h3 className="mt-1.5 text-[1.1rem] font-semibold leading-snug tracking-[-0.025em] transition-colors group-hover:text-forest">
                    {item.title}
                  </h3>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </section>
    </>
  );
}
