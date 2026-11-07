# Northleaf Pharmacy — Storefront UI Plan

> A production-grade storefront for the NexGen pharmacy platform, inspired by the editorial
> travel layout supplied as reference (organic hero, floating search, card grid, warm cream
> canvas, deep-green + orange palette) — reshaped for healthcare.

## 1. Brand

| Token | Value |
| --- | --- |
| Name | **Northleaf** |
| Descriptor | Pharmacy & wellness |
| Tagline | *Care that feels human.* |
| Voice | Calm, precise, warm. Never salesy. |

Wordmark: lowercase `northleaf` in a tight geometric sans with a hand-drawn leaf/crescent mark.
Avoid generic medical crosses, capsules-as-logo, and "AI gradient" clichés.

## 2. Design language

**Palette** (CSS custom properties, mapped to Tailwind theme)

- `--ink` `#10231C` — primary text and deep surfaces
- `--forest` `#1B4334` — brand primary
- `--forest-soft` `#2E5C4A`
- `--sage` `#DCE6DA` / `--sage-deep` `#B9CDBB`
- `--cream` `#F7F4EC` — page canvas
- `--paper` `#FFFDF8` — cards
- `--clay` `#E4823C` — accent (CTAs, prices, highlights)
- `--clay-dark` `#9C4514` — accessible accent text
- `--clay-soft` `#F3C9A6`
- `--muted` `#55685F`

**Type**

- Display + UI: **Manrope** (variable) — geometric, confident, tight tracking at large sizes.
- Editorial accent: **Fraunces** (italic, optical size) — used sparingly for one word in headlines,
  pull quotes, and the "since 1998" style details.
- Type scale: 12 / 14 / 16 / 18 / 22 / 28 / 36 / 48 / 64 / 80. Headline line-height 0.98–1.05.
- Numbers (prices, dosages) get `tabular-nums`.

**Shape & motion**

- Radii: 999 (pills), 28 (cards), 20 (controls), 12 (inputs).
- Organic hero mask: SVG blob / soft asymmetrical border-radius, echoing the reference.
- Motion: 180–320ms, `cubic-bezier(.22,1,.36,1)`; reveals are subtle translate+fade, disabled under
  `prefers-reduced-motion`. No parallax gimmicks, no cursor followers.
- Texture: 3% noise overlay on cream sections; 1px hairline dividers in `rgba(16,35,28,.10)`.

**Anti-"AI look" rules**

- Real photography, never 3D renders or gradient blobs as hero art.
- No emoji, no glassmorphism, no purple/blue gradients, no floating cards with fake UI.
- Asymmetry and editorial whitespace over centered-everything.
- One accent colour used with restraint; icons are a single stroke set (Lucide, 1.5px).
- Copy written like a pharmacy, not a SaaS landing page.

## 3. Information architecture

```
/                     Home
/shop                 Catalog (category, concern, price, availability filters)
/shop/:slug           Product detail (gallery, dosage, ingredients, stock, add to cart)
/cart                 Cart
/checkout             Checkout (delivery → payment → review)
/account              Account overview
/account/orders       Orders + order detail
/account/prescriptions Prescription upload & status
/login  /register     Auth
/pharmacists          Book a consultation (editorial page)
/help                 FAQ / contact
```

Admin UI is out of scope for this milestone.

## 4. Home page composition (reference structure → pharmacy)

1. **Header** — wordmark, primary nav (Shop, Health goals, Prescriptions, Pharmacists, Journal),
   search trigger, account, cart with count. Sticky with condensed state on scroll.
2. **Hero** — full-bleed photograph of a pharmacist/wellness still-life, organic cream mask overlay.
   Eyebrow `PHARMACY, REIMAGINED`. Headline: "Feel better, *every day*." Subcopy. Primary CTA
   `Shop essentials`, secondary `Upload a prescription`.
   Floating **service bar** (reference's search bar): `Search treatments` + `Delivery` + `Consultation`
   + `In-store pickup` + dark-green `Search` button. Blurred paper surface, hairline dividers.
3. **Assurance strip** — 4 items with line icons: Licensed pharmacists · Free delivery over $35 ·
   Discreet packaging · Same-day pickup.
4. **"Shelf favourites"** — 4 product cards (image, rating chip, category, name, price, add button)
   mirroring the destination cards.
5. **Health goals** — circular icon pills (Everyday wellness, Skin, Sleep, Heart, Immunity, Family).
6. **Editorial band** — "Notes from the counter" with two article cards, sage panel, organic corner leaf.
7. **Offer banner** — full-width deep-green panel, photo on left, "Your first refill is on us." CTA.
8. **Trust stats** — 25 yrs, 40k patients, 4.9 rating, 24/7 advice.
9. **Footer** — newsletter, columns, legal, payment marks.

## 5. Stack & architecture

- **Vite + React 19 + TypeScript (strict)** — fast, no SSR requirement for an authenticated
  storefront; deployable as static assets behind Kong/NGINX.
- **Tailwind CSS v4** with design tokens in `src/styles/theme.css`.
- **React Router v7** data router; route-level code splitting.
- **Zustand** cart store (persisted to `localStorage`, server sync when authenticated).
- **TanStack Query** for API reads; thin `lib/api.ts` fetch wrapper that targets
  `VITE_API_BASE_URL` (defaults to Kong `http://localhost:8000`) and falls back to seeded mock data
  when the backend is unreachable, so the UI is always demoable.
- **Lucide React** icons; **@fontsource-variable** for self-hosted fonts (no Google Fonts request).
- **Vitest + Testing Library** for component/store tests; ESLint + Prettier; `tsc --noEmit` in CI.

### Backend mapping (NexGen)

| UI surface | Endpoint(s) via Kong |
| --- | --- |
| Catalog / product | `GET /api/v1/products` (normalised from `skuCode`, `price`, `quantity`) |
| Stock badge | `GET /api/v1/inventory/{sku}` |
| Cart | `/api/v1/cart/**` |
| Checkout | `POST /api/v1/orders` (authoritative pricing server-side) |
| Orders | `GET /api/v1/orders/me` |
| Auth | `/api/v1/auth/login`, `/register`, `/forgot-password` |

Prices render from the gateway catalogue whenever `VITE_API_BASE_URL` is configured and
`VITE_USE_MOCK=false`. The client never computes authoritative totals for submission; cart
totals are display-only. Network failures and an unset gateway fall back to the seeded catalogue
so the demo remains usable.

## 6. Component inventory

`SiteHeader`, `MobileNav`, `SearchOverlay`, `Hero`, `ServiceBar`, `AssuranceStrip`, `ProductCard`,
`RatingChip`, `GoalPill`, `EditorialCard`, `OfferBanner`, `StatRow`, `Newsletter`, `SiteFooter`,
`QuantityStepper`, `Price`, `StockBadge`, `EmptyState`, `Skeleton`, `Toast`, `Drawer`, `Tabs`,
`Breadcrumbs`, `FilterPanel`, `AuthCard`, `OrderSummary`, `StepIndicator`, `FileDropzone`.

## 7. Quality bar

- WCAG AA contrast, keyboard-complete, visible focus rings, semantic landmarks, alt text.
- Responsive from 320px to 1920px; no horizontal scroll; 44px minimum touch targets.
- Seeded initial data plus empty/error messaging for every async surface.
- Images: locally stored, `width/height` set, lazy-loaded below the fold, AVIF/WebP-friendly sizing.
- No layout shift (reserve media ratios), CLS < 0.1 target.
- `npm run build`, `npm run typecheck` and Lighthouse must pass before hand-off.

## 8. Milestones

1. **Foundation** — scaffold, tokens, fonts, assets, app shell, header/footer, routing. ✅
2. **Home** — hero, service bar, assurances, favourites, goals, editorial, offer, stats. ✅
3. **Commerce** — catalog + filters, product detail, cart store/drawer/page. ✅
4. **Checkout & account** — checkout steps, order confirmation, auth, orders, prescriptions. ✅
5. **Polish** — responsive passes, motion, a11y audit, tests, README + screenshots. ✅

## 9. Final verification

- Lighthouse accessibility + best practices: **1.00** on Home, Shop, Product, Help, Journal,
  Account, Cart, Login, Prescriptions and Pharmacists.
- Keyboard focus traps verified for mobile nav, search overlay and cart drawer, including focus
  restoration to the trigger.
- Layout verified at 320, 390, 900 and 1440 CSS pixels with no horizontal overflow.
- Live catalogue adapter verified against a Spring `Page` payload: server prices, names, stock and
  product detail all render from the API response.
- Checkout → confirmation → account order history verified, including persistence after reload.
- Promo code `WELCOME10` persists across cart, checkout and review.

