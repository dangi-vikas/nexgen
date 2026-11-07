import { useState, type FormEvent } from 'react';
import {
  ArrowRight,
  CalendarDays,
  Check,
  Clock,
  HeartPulse,
  Leaf,
  Moon,
  Phone,
  Pill,
  Plane,
  ShieldCheck,
  Sparkles,
} from 'lucide-react';
import { PageMeta } from '@/components/ui/PageMeta';
import { Breadcrumbs } from '@/components/ui/Breadcrumbs';
import { pharmacists } from '@/lib/mock';

const services = [
  {
    icon: Pill,
    title: 'Medication review',
    copy: 'A full look at what you take, why, and whether it still fits.',
    meta: '30 minutes · free',
  },
  {
    icon: Sparkles,
    title: 'Skin consultation',
    copy: 'Barrier, breakouts, pigment. A routine you can actually keep up.',
    meta: '20 minutes · $25',
  },
  {
    icon: Moon,
    title: 'Sleep & stress',
    copy: 'Practical, evidence-led support before reaching for a tablet.',
    meta: '20 minutes · free',
  },
  {
    icon: Plane,
    title: 'Travel health',
    copy: 'Vaccines, malaria advice and a sensible kit for where you’re going.',
    meta: '30 minutes · from $40',
  },
];

export function PharmacistsPage() {
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({
    name: '',
    email: '',
    phone: '',
    topic: 'Medication review',
    preference: 'In person',
    notes: '',
  });

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!form.name || !form.email || !form.phone) {
      setError('Please add your name, email and phone so we can confirm.');
      return;
    }
    setError('');
    setSubmitted(true);
  }

  return (
    <>
      <PageMeta
        title="Pharmacists — NexGen Pharmacy"
        description="Book time with a licensed NexGen pharmacist for medication reviews, skin, sleep and travel health."
      />

      <section className="noise border-b border-line py-10 sm:py-14">
        <div className="container-page">
          <Breadcrumbs items={[{ label: 'Pharmacists' }]} />
          <div className="mt-6 grid items-center gap-10 lg:grid-cols-[1.05fr_0.95fr] lg:gap-16">
            <div>
              <p className="eyebrow text-clay-dark">
                <span className="inline-block h-px w-7 bg-clay-dark/70" aria-hidden="true" />
                Pharmacist care
              </p>
              <h1 className="display mt-4 text-[2.7rem] leading-[1] sm:text-[3.8rem]">
                A pharmacist
                <br />
                in your <span className="serif text-forest">corner.</span>
              </h1>
              <p className="mt-5 max-w-lg text-[15px] leading-relaxed text-muted">
                Not a chatbot, not a leaflet. A registered pharmacist who knows your history and has
                time to talk. Book a call, a video chat or a quiet corner of the shop.
              </p>
              <div className="mt-6 flex flex-wrap gap-3">
                <a href="#book" className="btn btn-primary">
                  Book a consultation
                  <ArrowRight className="size-4" aria-hidden="true" />
                </a>
                <a href="tel:+14155550134" className="btn btn-outline">
                  <Phone className="size-4" aria-hidden="true" />
                  (415) 555-0134
                </a>
              </div>
              <div className="mt-7 flex flex-wrap items-center gap-x-5 gap-y-2 text-[12px] text-muted">
                <span className="flex items-center gap-1.5">
                  <ShieldCheck className="size-4 text-forest" aria-hidden="true" />
                  Registered pharmacists
                </span>
                <span className="flex items-center gap-1.5">
                  <Clock className="size-4 text-forest" aria-hidden="true" />
                  Seven days a week
                </span>
              </div>
            </div>

            <div className="relative mx-auto w-full max-w-[520px]">
              <div className="organic-mask absolute inset-0 translate-x-4 translate-y-4 bg-sage-deep/40" aria-hidden="true" />
              <div className="organic-mask relative aspect-[4/4.2] overflow-hidden bg-sage">
                <img
                  src="/images/pharmacy-consult.jpg"
                  alt="A NexGen pharmacist consulting with a patient"
                  width={1600}
                  height={1067}
                  fetchPriority="high"
                  className="size-full object-cover object-[62%_48%]"
                />
                <div className="grain-overlay absolute inset-0" aria-hidden="true" />
                <div className="absolute inset-x-6 bottom-6 text-cream">
                  <p className="serif text-[1.4rem] leading-tight">“Bring the whole list. We’ll go through it.”</p>
                  <p className="mt-2 text-[9px] font-bold uppercase tracking-[0.18em] text-cream/65">
                    Amara Osei · Lead pharmacist
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="py-14 sm:py-16">
        <div className="container-page">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <p className="eyebrow text-clay-dark">The team</p>
              <h2 className="display mt-2 text-[2rem] sm:text-[2.7rem]">
                People, not a <span className="serif text-forest">hotline.</span>
              </h2>
            </div>
            <p className="max-w-sm text-[13px] leading-relaxed text-muted">
              Between them, our pharmacists have spent more than thirty years across community,
              hospital and herbal practice.
            </p>
          </div>

          <div className="mt-9 grid gap-6 sm:grid-cols-3">
            {pharmacists.map((person) => (
              <article key={person.id} className="group">
                <div className="aspect-[4/4.6] overflow-hidden rounded-[26px] bg-sage">
                  <img
                    src={person.image}
                    alt={`${person.name}, ${person.role}`}
                    width={900}
                    height={1035}
                    loading="lazy"
                    className="size-full object-cover object-[50%_28%] transition-transform duration-700 group-hover:scale-[1.03]"
                  />
                </div>
                <p className="mt-4 text-[10px] font-bold uppercase tracking-[0.15em] text-clay-dark">
                  {person.role} · {person.years} years
                </p>
                <h3 className="mt-1.5 text-[1.15rem] font-semibold tracking-[-0.025em]">{person.name}</h3>
                <p className="mt-0.5 text-[12px] font-semibold text-forest">{person.focus}</p>
                <p className="mt-2 text-[12.5px] leading-relaxed text-muted">{person.bio}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="border-y border-line bg-sage/35 py-14 sm:py-16">
        <div className="container-page">
          <p className="eyebrow text-clay-dark">What we can help with</p>
          <h2 className="display mt-2 text-[2rem] sm:text-[2.7rem]">
            Time for the <span className="serif text-forest">whole picture.</span>
          </h2>
          <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {services.map(({ icon: Icon, title, copy, meta }) => (
              <div key={title} className="rounded-[24px] border border-line bg-paper p-5">
                <span className="grid size-10 place-items-center rounded-full bg-sage text-forest">
                  <Icon className="size-5" strokeWidth={1.6} aria-hidden="true" />
                </span>
                <h3 className="mt-4 text-[14px] font-semibold tracking-[-0.01em]">{title}</h3>
                <p className="mt-1.5 text-[12px] leading-relaxed text-muted">{copy}</p>
                <p className="mt-4 text-[10px] font-bold uppercase tracking-[0.12em] text-clay-dark">{meta}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section id="book" className="scroll-mt-28 py-14 sm:py-18">
        <div className="container-page grid items-start gap-10 lg:grid-cols-[0.9fr_1.1fr] lg:gap-16">
          <div>
            <p className="eyebrow text-clay-dark">Book a time</p>
            <h2 className="display mt-3 text-[2.2rem] leading-[1.02] sm:text-[3rem]">
              Twenty minutes
              <br />
              that change the <span className="serif text-forest">week.</span>
            </h2>
            <p className="mt-4 max-w-sm text-[14px] leading-relaxed text-muted">
              Choose what you’d like to talk about and we’ll match you with the right pharmacist. No
              scripts, no rush.
            </p>
            <div className="mt-7 space-y-4">
              {[
                { icon: CalendarDays, title: 'Same-week appointments', copy: 'In person, phone or video.' },
                { icon: HeartPulse, title: 'Whole-person review', copy: 'Medicines, lifestyle and what matters to you.' },
                { icon: Leaf, title: 'Free follow-up', copy: 'A short check-in after your appointment.' },
              ].map(({ icon: Icon, title, copy }) => (
                <div key={title} className="flex items-start gap-3">
                  <span className="grid size-9 shrink-0 place-items-center rounded-full bg-sage text-forest">
                    <Icon className="size-4" strokeWidth={1.6} aria-hidden="true" />
                  </span>
                  <div>
                    <p className="text-[13px] font-semibold">{title}</p>
                    <p className="mt-0.5 text-[11.5px] text-muted">{copy}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="rounded-[30px] border border-line bg-paper p-6 sm:p-8">
            {submitted ? (
              <div className="py-10 text-center">
                <span className="mx-auto grid size-14 place-items-center rounded-full bg-sage text-forest">
                  <Check className="size-6" aria-hidden="true" />
                </span>
                <h3 className="display mt-5 text-[1.8rem]">
                  Request <span className="serif text-forest">received.</span>
                </h3>
                <p className="mx-auto mt-3 max-w-sm text-[13px] leading-relaxed text-muted">
                  We’ll call {form.phone} within one working day to confirm a time. If it’s urgent,
                  ring the pharmacy directly.
                </p>
                <button
                  type="button"
                  onClick={() => {
                    setSubmitted(false);
                    setForm({ name: '', email: '', phone: '', topic: 'Medication review', preference: 'In person', notes: '' });
                  }}
                  className="btn btn-outline mt-6"
                >
                  Book another
                </button>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="grid gap-4 sm:grid-cols-2">
                <div>
                  <label htmlFor="book-name" className="mb-1.5 block text-[11px] font-bold uppercase tracking-[0.12em] text-muted">
                    Full name
                  </label>
                  <input
                    id="book-name"
                    value={form.name}
                    onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))}
                    className="field"
                  />
                </div>
                <div>
                  <label htmlFor="book-phone" className="mb-1.5 block text-[11px] font-bold uppercase tracking-[0.12em] text-muted">
                    Phone
                  </label>
                  <input
                    id="book-phone"
                    type="tel"
                    value={form.phone}
                    onChange={(event) => setForm((current) => ({ ...current, phone: event.target.value }))}
                    className="field"
                  />
                </div>
                <div className="sm:col-span-2">
                  <label htmlFor="book-email" className="mb-1.5 block text-[11px] font-bold uppercase tracking-[0.12em] text-muted">
                    Email
                  </label>
                  <input
                    id="book-email"
                    type="email"
                    value={form.email}
                    onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
                    className="field"
                  />
                </div>
                <div>
                  <label htmlFor="book-topic" className="mb-1.5 block text-[11px] font-bold uppercase tracking-[0.12em] text-muted">
                    What’s it about?
                  </label>
                  <select
                    id="book-topic"
                    value={form.topic}
                    onChange={(event) => setForm((current) => ({ ...current, topic: event.target.value }))}
                    className="field appearance-none"
                  >
                    {['Medication review', 'Skin', 'Sleep & stress', 'Travel health', 'Something else'].map((topic) => (
                      <option key={topic}>{topic}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label htmlFor="book-preference" className="mb-1.5 block text-[11px] font-bold uppercase tracking-[0.12em] text-muted">
                    Preference
                  </label>
                  <select
                    id="book-preference"
                    value={form.preference}
                    onChange={(event) => setForm((current) => ({ ...current, preference: event.target.value }))}
                    className="field appearance-none"
                  >
                    {['In person', 'Phone call', 'Video call'].map((option) => (
                      <option key={option}>{option}</option>
                    ))}
                  </select>
                </div>
                <div className="sm:col-span-2">
                  <label htmlFor="book-notes" className="mb-1.5 block text-[11px] font-bold uppercase tracking-[0.12em] text-muted">
                    Anything we should know? <span className="font-medium normal-case tracking-normal">(optional)</span>
                  </label>
                  <textarea
                    id="book-notes"
                    rows={3}
                    value={form.notes}
                    onChange={(event) => setForm((current) => ({ ...current, notes: event.target.value }))}
                    className="field resize-none"
                    placeholder="Medicines you’re taking, or what you’d like to cover."
                  />
                </div>
                {error ? (
                  <p className="text-[12.5px] font-medium text-clay-dark sm:col-span-2" role="alert">
                    {error}
                  </p>
                ) : null}
                <div className="sm:col-span-2">
                  <button type="submit" className="btn btn-primary w-full sm:w-auto">
                    Request appointment
                    <ArrowRight className="size-4" aria-hidden="true" />
                  </button>
                  <p className="mt-3 text-[10.5px] leading-relaxed text-muted">
                    We’ll only use these details to arrange your appointment.
                  </p>
                </div>
              </form>
            )}
          </div>
        </div>
      </section>
    </>
  );
}
