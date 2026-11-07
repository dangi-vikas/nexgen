export type ProductCategory =
  | 'Skin'
  | 'Supplements'
  | 'Herbal'
  | 'Everyday'
  | 'Family'
  | 'Sleep';

export interface Product {
  id: string;
  sku: string;
  slug: string;
  name: string;
  subtitle: string;
  category: ProductCategory;
  concerns: string[];
  price: number;
  compareAt?: number;
  rating: number;
  reviews: number;
  stock: number;
  image: string;
  gallery: string[];
  size: string;
  description: string;
  dosage?: string;
  ingredients: string[];
  badge?: string;
  prescription?: boolean;
}

export interface Category {
  id: string;
  label: string;
  blurb: string;
  icon:
    | 'droplets'
    | 'pill'
    | 'leaf'
    | 'sun'
    | 'baby'
    | 'moon'
    | 'heart'
    | 'shield';
}

export interface Article {
  id: string;
  slug: string;
  title: string;
  excerpt: string;
  category: string;
  readTime: string;
  image: string;
  date: string;
  body: string[];
}

export interface Pharmacist {
  id: string;
  name: string;
  role: string;
  focus: string;
  bio: string;
  image: string;
  years: number;
}

export interface OrderLine {
  productId: string;
  name: string;
  size: string;
  price: number;
  quantity: number;
  image: string;
}

export interface Order {
  id: string;
  number: string;
  placedAt: string;
  status: 'Processing' | 'Packed' | 'Out for delivery' | 'Delivered';
  total: number;
  lines: OrderLine[];
  tracking?: string;
}

export interface Prescription {
  id: string;
  medication: string;
  prescriber: string;
  submittedAt: string;
  status: 'Under review' | 'Ready for pickup' | 'Out for delivery' | 'Completed';
  refillsLeft: number;
}

export interface CartLine {
  product: Product;
  quantity: number;
}

export interface User {
  id: string;
  name: string;
  email: string;
  phone?: string;
}
