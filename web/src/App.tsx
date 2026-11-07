import { lazy } from 'react';
import { Route, Routes } from 'react-router-dom';
import { Layout } from '@/components/layout/Layout';
import { HomePage } from '@/pages/HomePage';

const ShopPage = lazy(() => import('@/pages/ShopPage').then((module) => ({ default: module.ShopPage })));
const ProductPage = lazy(() =>
  import('@/pages/ProductPage').then((module) => ({ default: module.ProductPage })),
);
const CartPage = lazy(() => import('@/pages/CartPage').then((module) => ({ default: module.CartPage })));
const CheckoutPage = lazy(() =>
  import('@/pages/CheckoutPage').then((module) => ({ default: module.CheckoutPage })),
);
const AccountPage = lazy(() =>
  import('@/pages/AccountPage').then((module) => ({ default: module.AccountPage })),
);
const AuthPage = lazy(() => import('@/pages/AuthPage').then((module) => ({ default: module.AuthPage })));
const PrescriptionsPage = lazy(() =>
  import('@/pages/PrescriptionsPage').then((module) => ({ default: module.PrescriptionsPage })),
);
const PharmacistsPage = lazy(() =>
  import('@/pages/PharmacistsPage').then((module) => ({ default: module.PharmacistsPage })),
);
const JournalPage = lazy(() =>
  import('@/pages/JournalPage').then((module) => ({ default: module.JournalPage })),
);
const ArticlePage = lazy(() =>
  import('@/pages/ArticlePage').then((module) => ({ default: module.ArticlePage })),
);
const HelpPage = lazy(() => import('@/pages/HelpPage').then((module) => ({ default: module.HelpPage })));
const NotFoundPage = lazy(() =>
  import('@/pages/NotFoundPage').then((module) => ({ default: module.NotFoundPage })),
);

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route path="shop" element={<ShopPage />} />
        <Route path="shop/:slug" element={<ProductPage />} />
        <Route path="cart" element={<CartPage />} />
        <Route path="checkout" element={<CheckoutPage />} />
        <Route path="account" element={<AccountPage />} />
        <Route path="account/orders" element={<AccountPage />} />
        <Route path="account/prescriptions" element={<AccountPage />} />
        <Route path="account/details" element={<AccountPage />} />
        <Route path="login" element={<AuthPage />} />
        <Route path="register" element={<AuthPage />} />
        <Route path="prescriptions" element={<PrescriptionsPage />} />
        <Route path="pharmacists" element={<PharmacistsPage />} />
        <Route path="journal" element={<JournalPage />} />
        <Route path="journal/:slug" element={<ArticlePage />} />
        <Route path="help" element={<HelpPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
