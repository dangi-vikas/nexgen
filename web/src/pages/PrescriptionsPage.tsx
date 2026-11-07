import { useRef, useState, type DragEvent, type FormEvent } from 'react';
import {
  ArrowRight,
  Check,
  Clock,
  FileText,
  Phone,
  Pill,
  ShieldCheck,
  UploadCloud,
  X,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { PageMeta } from '@/components/ui/PageMeta';
import { Breadcrumbs } from '@/components/ui/Breadcrumbs';
import { usePrescriptions } from '@/lib/queries';
import { formatDate } from '@/lib/utils';

export function PrescriptionsPage() {
  const inputRef = useRef<HTMLInputElement | null>(null);
  const prescriptions = usePrescriptions();
  const [file, setFile] = useState<File | null>(null);
  const [dragging, setDragging] = useState(false);
  const [consent, setConsent] = useState(false);
  const [error, setError] = useState('');
  const [submitted, setSubmitted] = useState(false);

  function acceptFile(next: File | null) {
    if (!next) return;
    const allowed = ['image/jpeg', 'image/png', 'image/webp', 'application/pdf'];
    if (!allowed.includes(next.type)) {
      setError('Please upload a JPG, PNG, WebP or PDF.');
      return;
    }
    if (next.size > 10 * 1024 * 1024) {
      setError('That file is larger than 10 MB. Try a smaller photo.');
      return;
    }
    setError('');
    setFile(next);
  }

  function handleDrop(event: DragEvent<HTMLDivElement>) {
    event.preventDefault();
    setDragging(false);
    acceptFile(event.dataTransfer.files?.[0] ?? null);
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!file) {
      setError('Add a photo or PDF of your prescription first.');
      return;
    }
    if (!consent) {
      setError('Please confirm you’re happy for a pharmacist to review this.');
      return;
    }
    setError('');
    setSubmitted(true);
  }

  return (
    <>
      <PageMeta
        title="Prescriptions — Northleaf Pharmacy"
        description="Upload a prescription, transfer from another pharmacy or request a repeat. Reviewed by licensed pharmacists."
      />

      <section className="noise border-b border-line bg-sage/35 py-10 sm:py-14">
        <div className="container-page">
          <Breadcrumbs items={[{ label: 'Prescriptions' }]} />
          <div className="mt-6 grid gap-8 lg:grid-cols-[1.2fr_0.8fr] lg:items-end">
            <div>
              <p className="eyebrow text-clay-dark">
                <span className="inline-block h-px w-7 bg-clay-dark/70" aria-hidden="true" />
                Prescriptions
              </p>
              <h1 className="display mt-4 text-[2.6rem] leading-[1.01] sm:text-[3.6rem]">
                Prescriptions,
                <br />
                without the <span className="serif text-forest">runaround.</span>
              </h1>
              <p className="mt-4 max-w-lg text-[15px] leading-relaxed text-muted">
                Send us a photo, transfer from another pharmacy, or ask your prescriber to send it
                directly. A licensed pharmacist reviews every order.
              </p>
            </div>
            <div className="rounded-2xl border border-line bg-paper/80 p-4 text-[12.5px] leading-relaxed text-muted">
              <p className="flex items-center gap-2 font-semibold text-ink">
                <Clock className="size-4 text-forest" aria-hidden="true" />
                Need it today?
              </p>
              <p className="mt-1.5">
                Call the pharmacy on{' '}
                <a href="tel:+14155550134" className="font-semibold text-forest">
                  (415) 555-0134
                </a>{' '}
                before 2pm and we’ll do our best to have it ready by closing.
              </p>
            </div>
          </div>
        </div>
      </section>

      <section className="py-10 sm:py-14">
        <div className="container-page grid items-start gap-8 lg:grid-cols-[1.15fr_0.85fr] lg:gap-12">
          <div className="rounded-[30px] border border-line bg-paper p-6 sm:p-8">
            {submitted ? (
              <div className="py-8 text-center">
                <span className="mx-auto grid size-16 place-items-center rounded-full bg-sage text-forest">
                  <Check className="size-7" strokeWidth={2} aria-hidden="true" />
                </span>
                <p className="eyebrow mt-5 justify-center text-clay-dark">Received</p>
                <h2 className="display mt-3 text-[2rem] sm:text-[2.4rem]">
                  Thank you — we have <span className="serif text-forest">it.</span>
                </h2>
                <p className="mx-auto mt-3 max-w-md text-[13.5px] leading-relaxed text-muted">
                  A pharmacist will review your prescription today. We’ll text and email when it’s
                  ready for pickup or on its way.
                </p>
                <div className="mx-auto mt-6 max-w-sm rounded-xl bg-cream px-4 py-3 text-left text-[12px]">
                  <p className="flex items-center gap-2 font-semibold">
                    <FileText className="size-4 text-forest" aria-hidden="true" />
                    {file?.name}
                  </p>
                  <p className="mt-1 pl-6 text-muted">Reference RX-{Math.floor(10000 + Math.random() * 89999)}</p>
                </div>
                <div className="mt-6 flex flex-wrap justify-center gap-3">
                  <Link to="/account/prescriptions" className="btn btn-primary">
                    View prescriptions
                  </Link>
                  <button
                    type="button"
                    onClick={() => {
                      setSubmitted(false);
                      setFile(null);
                      setConsent(false);
                    }}
                    className="btn btn-outline"
                  >
                    Upload another
                  </button>
                </div>
              </div>
            ) : (
              <>
                <p className="eyebrow text-clay-dark">Upload</p>
                <h2 className="mt-2 text-xl font-semibold tracking-[-0.03em]">
                  Send us your prescription
                </h2>
                <p className="mt-1.5 text-[13px] text-muted">
                  A clear photo of the full page is usually enough. PDFs are welcome too.
                </p>

                <form onSubmit={handleSubmit} className="mt-5">
                  <div
                    onDragOver={(event) => {
                      event.preventDefault();
                      setDragging(true);
                    }}
                    onDragLeave={() => setDragging(false)}
                    onDrop={handleDrop}
                    className={`relative flex flex-col items-center justify-center rounded-2xl border border-dashed px-6 py-10 text-center transition-colors ${
                      dragging ? 'border-forest bg-sage/40' : 'border-line-strong bg-cream/50'
                    }`}
                  >
                    <input
                      ref={inputRef}
                      type="file"
                      accept="image/jpeg,image/png,image/webp,application/pdf"
                      className="sr-only"
                      onChange={(event) => acceptFile(event.target.files?.[0] ?? null)}
                      aria-label="Choose a prescription file"
                    />
                    {file ? (
                      <>
                        <FileText className="size-8 text-forest" strokeWidth={1.5} aria-hidden="true" />
                        <p className="mt-3 max-w-full truncate text-[13.5px] font-semibold">{file.name}</p>
                        <p className="mt-1 text-[11.5px] text-muted">
                          {(file.size / 1024 / 1024).toFixed(1)} MB · ready to review
                        </p>
                        <button
                          type="button"
                          onClick={() => setFile(null)}
                          className="mt-3 inline-flex items-center gap-1 text-[11.5px] font-bold text-clay-dark"
                        >
                          <X className="size-3.5" aria-hidden="true" />
                          Remove file
                        </button>
                      </>
                    ) : (
                      <>
                        <span className="grid size-12 place-items-center rounded-full bg-sage text-forest">
                          <UploadCloud className="size-5" strokeWidth={1.6} aria-hidden="true" />
                        </span>
                        <p className="mt-3 text-[13.5px] font-semibold">
                          Drag a file here, or{' '}
                          <button
                            type="button"
                            onClick={() => inputRef.current?.click()}
                            className="text-forest underline underline-offset-2"
                          >
                            browse
                          </button>
                        </p>
                        <p className="mt-1 text-[11.5px] text-muted">JPG, PNG, WebP or PDF · up to 10 MB</p>
                      </>
                    )}
                  </div>

                  <label className="mt-4 flex cursor-pointer items-start gap-2.5 text-[12px] leading-relaxed text-muted">
                    <input
                      type="checkbox"
                      checked={consent}
                      onChange={(event) => {
                        setConsent(event.target.checked);
                        setError('');
                      }}
                      className="mt-0.5 size-4 rounded accent-forest"
                    />
                    I confirm this prescription is valid and consent to a Northleaf pharmacist
                    reviewing it.
                  </label>

                  {error ? (
                    <p className="mt-3 text-[12.5px] font-medium text-clay-dark" role="alert">
                      {error}
                    </p>
                  ) : null}

                  <button type="submit" className="btn btn-primary mt-5 w-full sm:w-auto">
                    Send to pharmacy
                    <ArrowRight className="size-4" aria-hidden="true" />
                  </button>
                  <p className="mt-3 flex items-center gap-1.5 text-[10.5px] text-muted">
                    <ShieldCheck className="size-3.5" aria-hidden="true" />
                    Encrypted in transit. Reviewed only by registered pharmacists.
                  </p>
                </form>
              </>
            )}
          </div>

          <aside className="space-y-5">
            <div className="rounded-[26px] border border-line bg-paper p-5 sm:p-6">
              <h2 className="text-[15px] font-semibold tracking-[-0.02em]">How it works</h2>
              <ol className="mt-4 space-y-4">
                {[
                  {
                    title: 'Send it over',
                    copy: 'Upload a photo, or ask your prescriber to send it to Northleaf.',
                  },
                  {
                    title: 'We review',
                    copy: 'A pharmacist checks interactions and stock, usually within a few hours.',
                  },
                  {
                    title: 'Collect or deliver',
                    copy: 'Pick up after 4pm, or we’ll post it discreetly to your door.',
                  },
                ].map((step, index) => (
                  <li key={step.title} className="flex gap-3">
                    <span className="tnum grid size-6 shrink-0 place-items-center rounded-full bg-sage text-[10px] font-bold text-forest">
                      {index + 1}
                    </span>
                    <div>
                      <p className="text-[13px] font-semibold">{step.title}</p>
                      <p className="mt-0.5 text-[11.5px] leading-relaxed text-muted">{step.copy}</p>
                    </div>
                  </li>
                ))}
              </ol>
            </div>

            <div className="rounded-[26px] bg-forest p-5 text-cream sm:p-6">
              <Phone className="size-5 text-sage" strokeWidth={1.6} aria-hidden="true" />
              <p className="mt-3 text-[15px] font-semibold">Transferring from elsewhere?</p>
              <p className="mt-1 text-[12px] leading-relaxed text-cream/65">
                We’ll call your current pharmacy and handle the paperwork. No need to chase anyone.
              </p>
              <a href="tel:+14155550134" className="mt-4 inline-flex text-[12.5px] font-bold text-clay-soft hover:text-cream">
                (415) 555-0134
              </a>
            </div>

            <div className="rounded-[26px] border border-line bg-paper p-5 sm:p-6">
              <h2 className="text-[15px] font-semibold tracking-[-0.02em]">Your recent prescriptions</h2>
              <ul className="mt-4 space-y-3">
                {prescriptions.map((item) => (
                  <li key={item.id} className="flex items-start gap-3 border-b border-line pb-3 last:border-0 last:pb-0">
                    <Pill className="mt-0.5 size-4 shrink-0 text-forest" strokeWidth={1.7} aria-hidden="true" />
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-[12.5px] font-semibold">{item.medication}</p>
                      <p className="mt-0.5 text-[11px] text-muted">
                        {item.status} · {formatDate(item.submittedAt)}
                      </p>
                    </div>
                  </li>
                ))}
              </ul>
              <Link to="/account/prescriptions" className="link-quiet mt-4 text-[12px]">
                Manage prescriptions
                <ArrowRight className="size-3.5" aria-hidden="true" />
              </Link>
            </div>
          </aside>
        </div>
      </section>
    </>
  );
}
