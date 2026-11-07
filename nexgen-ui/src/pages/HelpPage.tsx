import { ChevronDown, Clock, Mail, MapPin, MessageCircle, Phone } from 'lucide-react';
import { Link } from 'react-router-dom';
import { PageMeta } from '@/components/ui/PageMeta';
import { Breadcrumbs } from '@/components/ui/Breadcrumbs';

const faqs = [
  {
    q: 'When will my order arrive?',
    a: 'Standard delivery takes 3–5 working days and is free over $35. Express orders placed before 2pm arrive the next working day. You’ll get tracking by email as soon as your parcel leaves the pharmacy.',
  },
  {
    q: 'Can I collect a prescription today?',
    a: 'Yes, if we receive it before 2pm. Upload it here or call the pharmacy and we’ll confirm when it will be ready. Collection is from 48 Alder Street until 8pm on weekdays.',
  },
  {
    q: 'How do I transfer from another pharmacy?',
    a: 'Upload a photo of your prescription or call us with the name of your current pharmacy. We’ll handle the transfer directly — you don’t need to chase anyone.',
  },
  {
    q: 'What is your returns policy?',
    a: 'Unopened products can be returned within 30 days for a full refund. For safety reasons, medicines cannot be returned once they have left the pharmacy. If something is wrong with your order, contact us and we’ll put it right.',
  },
  {
    q: 'Is my health information private?',
    a: 'Yes. Your prescription and account details are encrypted in transit and at rest, accessible only to the pharmacists involved in your care. We never sell personal data or share it for advertising.',
  },
  {
    q: 'Do you deliver outside California?',
    a: 'We currently deliver prescription and non-prescription items across the contiguous United States. Controlled medicines are subject to state-specific rules and may require collection in store.',
  },
];

export function HelpPage() {
  return (
    <>
      <PageMeta
        title="Help & contact — NexGen Pharmacy"
        description="Delivery, returns, prescriptions and privacy. Reach a NexGen pharmacist by phone, email or in person."
      />

      <section className="border-b border-line bg-sage/30 py-10 sm:py-12">
        <div className="container-page">
          <Breadcrumbs items={[{ label: 'Help & contact' }]} />
          <div className="mt-6 max-w-2xl">
            <p className="eyebrow text-clay-dark">Help & contact</p>
            <h1 className="display mt-3 text-[2.6rem] sm:text-[3.4rem]">
              However we can <span className="serif text-forest">help.</span>
            </h1>
            <p className="mt-4 text-[15px] leading-relaxed text-muted">
              A real person answers our phone during opening hours. For everything else, the answers
              below cover most of it.
            </p>
          </div>
        </div>
      </section>

      <section className="py-10 sm:py-14">
        <div className="container-page grid items-start gap-10 lg:grid-cols-[1.2fr_0.8fr] lg:gap-16">
          <div>
            <h2 className="text-xl font-semibold tracking-[-0.03em]">Common questions</h2>
            <div className="mt-5 divide-y divide-line border-y border-line">
              {faqs.map((faq, index) => (
                <details key={faq.q} className="group" open={index === 0}>
                  <summary className="flex cursor-pointer list-none items-center justify-between gap-4 py-4 text-[13.5px] font-semibold marker:hidden">
                    {faq.q}
                    <ChevronDown
                      className="size-4 shrink-0 text-muted transition-transform group-open:rotate-180"
                      aria-hidden="true"
                    />
                  </summary>
                  <p className="pb-5 pr-8 text-[13px] leading-relaxed text-muted">{faq.a}</p>
                </details>
              ))}
            </div>

            <div id="delivery" className="scroll-mt-28 pt-12">
              <h2 className="text-xl font-semibold tracking-[-0.03em]">Delivery & returns</h2>
              <p className="mt-3 max-w-2xl text-[13.5px] leading-relaxed text-muted">
                Standard delivery is free over $35 and $4.95 below that. Express delivery is $9.95
                and arrives next working day when ordered before 2pm. Everything leaves the pharmacy
                in a plain, letterbox-friendly outer box. Returns are accepted within 30 days for
                unopened non-medicine products.
              </p>
            </div>

            <div id="privacy" className="scroll-mt-28 pt-10">
              <h2 className="text-xl font-semibold tracking-[-0.03em]">Privacy & your data</h2>
              <p className="mt-3 max-w-2xl text-[13.5px] leading-relaxed text-muted">
                We collect only what is needed to dispense prescriptions and deliver orders. Health
                data is encrypted, access-controlled and retained in line with California and
                federal pharmacy law. You can request a copy or deletion of your account data by
                emailing <a href="mailto:privacy@nexgen.example" className="font-semibold text-forest">privacy@nexgen.example</a>.
              </p>
            </div>

            <div id="terms" className="scroll-mt-28 pt-10">
              <h2 className="text-xl font-semibold tracking-[-0.03em]">Terms of sale</h2>
              <p className="mt-3 max-w-2xl text-[13.5px] leading-relaxed text-muted">
                This storefront is a demonstration. Products, prices and availability are
                illustrative and not a real offer of sale. In a production deployment, all orders
                and prices would be confirmed by the pharmacy before dispensing.
              </p>
            </div>

            <div id="accessibility" className="scroll-mt-28 pt-10">
              <h2 className="text-xl font-semibold tracking-[-0.03em]">Accessibility</h2>
              <p className="mt-3 max-w-2xl text-[13.5px] leading-relaxed text-muted">
                We aim to meet WCAG 2.2 AA across the storefront, including keyboard navigation,
                visible focus states and reduced-motion support. If something is difficult to use,
                call us and we’ll help directly.
              </p>
            </div>

            <div id="careers" className="scroll-mt-28 pt-10">
              <h2 className="text-xl font-semibold tracking-[-0.03em]">Careers</h2>
              <p className="mt-3 max-w-2xl text-[13.5px] leading-relaxed text-muted">
                We are always glad to meet pharmacists who believe care is a conversation. Write to{' '}
                <a href="mailto:careers@nexgen.example" className="font-semibold text-forest">careers@nexgen.example</a>{' '}
                with a short note.
              </p>
            </div>
          </div>

          <aside className="space-y-4 lg:sticky lg:top-28">
            <div className="rounded-[26px] border border-line bg-paper p-5 sm:p-6">
              <h2 className="text-[15px] font-semibold">Talk to a pharmacist</h2>
              <ul className="mt-4 space-y-4 text-[12.5px]">
                <li className="flex items-start gap-3">
                  <Phone className="mt-0.5 size-4 shrink-0 text-forest" strokeWidth={1.7} aria-hidden="true" />
                  <span>
                    <a href="tel:+14155550134" className="font-semibold hover:text-forest">
                      (415) 555-0134
                    </a>
                    <span className="mt-0.5 block text-muted">Mon–Sat 8am–8pm · Sun 10am–5pm</span>
                  </span>
                </li>
                <li className="flex items-start gap-3">
                  <Mail className="mt-0.5 size-4 shrink-0 text-forest" strokeWidth={1.7} aria-hidden="true" />
                  <span>
                    <a href="mailto:care@nexgen.example" className="font-semibold hover:text-forest">
                      care@nexgen.example
                    </a>
                    <span className="mt-0.5 block text-muted">Replies within one working day</span>
                  </span>
                </li>
                <li className="flex items-start gap-3">
                  <MapPin className="mt-0.5 size-4 shrink-0 text-forest" strokeWidth={1.7} aria-hidden="true" />
                  <span>
                    <span className="font-semibold">48 Alder Street, Suite 2</span>
                    <span className="mt-0.5 block text-muted">San Francisco, CA 94110</span>
                  </span>
                </li>
                <li className="flex items-start gap-3">
                  <Clock className="mt-0.5 size-4 shrink-0 text-forest" strokeWidth={1.7} aria-hidden="true" />
                  <span>
                    <span className="font-semibold">Open seven days</span>
                    <span className="mt-0.5 block text-muted">Same-day pickup before 4pm</span>
                  </span>
                </li>
              </ul>
            </div>

            <div className="rounded-[26px] bg-forest p-5 text-cream sm:p-6">
              <MessageCircle className="size-5 text-sage" strokeWidth={1.6} aria-hidden="true" />
              <p className="mt-3 text-[15px] font-semibold">Prefer to book a time?</p>
              <p className="mt-1 text-[12px] leading-relaxed text-cream/65">
                A free twenty-minute consultation with a pharmacist, in person or by phone.
              </p>
              <Link to="/pharmacists#book" className="btn btn-clay mt-4 w-full">
                Book a consultation
              </Link>
            </div>
          </aside>
        </div>
      </section>
    </>
  );
}
