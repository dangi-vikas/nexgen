# NexGen storefront

The customer-facing pharmacy storefront for NexGen, built around a simple
promise: **care that feels human**. The UI is an editorial, cream-and-forest
storefront rather than a generic dashboard.

## Stack

- Vite + React 19 + TypeScript (strict)
- Tailwind CSS v4 with design tokens in `src/index.css`
- React Router with lazy-loaded routes
- TanStack Query for server data with a seeded mock fallback
- Zustand + `localStorage` for cart, promo code and browser-placed orders
- Manrope + Fraunces variable fonts, Lucide icons

## Run locally

```bash
cd nexgen-ui
npm install
npm run dev
```

The dev server runs on `http://localhost:5173` and proxies `/api` to Kong on
`http://localhost:8000`.

Other scripts:

```bash
npm run typecheck
npm run build
npm run preview
```

## Data and environment

Copy `nexgen-ui/.env.example` to `nexgen-ui/.env` and adjust:

| Variable | Purpose |
| --- | --- |
| `VITE_API_BASE_URL` | Gateway origin, for example `http://localhost:8000`. Leave unset to use the seeded catalogue (`VITE_USE_MOCK=false` plus an empty value uses the Vite `/api` proxy). |
| `VITE_USE_MOCK` | `true` skips network calls entirely. `false` reads the catalogue from the gateway. |

When the API is configured, product **prices, names, stock and descriptions come
from the product service**. The adapter (`src/lib/api.ts`) normalises the NexGen
payload (`skuCode`, `price`, `quantity`) into the storefront `Product` shape.
If the gateway is unreachable, the adapter falls back to the seeded catalogue so
the UI remains demoable.

Relevant endpoints:

- `GET /api/v1/products?size=48` — catalogue
- `POST /api/v1/auth/login`, `POST /api/v1/auth/register`, `POST /api/v1/auth/forgot-password`
- `GET /api/v1/orders/me` — account orders (when available)

The product detail route uses the catalogue list rather than assuming the
backend exposes a slug lookup, so live prices remain consistent between the
shelf and product page.

## Routes

| Route | Page |
| --- | --- |
| `/` | Home |
| `/shop` | Catalogue with category, concern, price and stock filters |
| `/shop/:slug` | Product detail |
| `/cart`, `/checkout` | Bag and three-step checkout |
| `/login`, `/register` | Auth |
| `/account`, `/account/orders`, `/account/prescriptions`, `/account/details` | Account area |
| `/prescriptions` | Prescription upload and status |
| `/pharmacists` | Pharmacist team and consultation |
| `/help` | Help centre |
| `/journal`, `/journal/:slug` | Editorial journal |
| `*` | 404 |

## Demo behaviour

- Mock catalogue and editorial content live in `src/lib/mock.ts`.
- `WELCOME10` is the demo promo code and persists across cart, checkout and review.
- Orders placed in the browser are stored under `nexgen-placed-orders` and
  merged into the account order history, so the confirmation flow is visible
  end to end without a running order service.
- Cart contents and the applied promo code are persisted locally.

The frontend is a static SPA (`dist/` after `npm run build`) and can be served by
any static host. Configure the host to rewrite unknown routes to `index.html`.

## Quality checks

- Lighthouse accessibility and best-practices: **1.00** on Home, Shop, Product,
  Help and Journal.
- Keyboard focus traps and focus restoration are implemented for mobile nav,
  cart drawer and search overlay.
- Layout verified at 320, 390, 900 and 1440 CSS pixels.
- Typecheck and production build pass.

## Photography

The demo photographs are sourced from Pexels and Unsplash under their free
licences. Replace them with commissioned or brand-licensed photography before a
commercial launch.
