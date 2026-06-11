import { API_ORIGIN } from "@/lib/api";
const API_BASE = `${API_ORIGIN}/api/v1`;

export type Pricing = "FREE" | "PAID";
export type ItemType = "RESOURCE" | "BUNDLE";

export interface ResourceCard {
  slug: string;
  title: string;
  description: string;
  level: string | null;
  category: string | null;
  pricing: Pricing;
  priceCents: number | null;
  owned: boolean;
  locked: boolean;
  relatedResourceSlug: string | null;
}

export interface BundleCard {
  slug: string;
  title: string;
  description: string;
  priceCents: number;
  resourceSlugs: string[];
  owned: boolean;
}

export interface CatalogResponse {
  currency: string;
  freeResources: ResourceCard[];
  paidResources: ResourceCard[];
  bundles: BundleCard[];
}

export interface ResourceDetail {
  slug: string;
  title: string;
  description: string;
  level: string | null;
  category: string | null;
  pricing: Pricing;
  priceCents: number | null;
  currency: string;
  previewText: string | null;
  owned: boolean;
  locked: boolean;
  relatedResourceSlug: string | null;
}

export interface AssetItem {
  assetType: "FILE" | "EMBED";
  label: string;
  locator: string;
}

export interface ContentResponse {
  slug: string;
  assets: AssetItem[];
}

export interface PurchaseResponse {
  id: string;
  itemType: ItemType;
  slug: string;
  title: string;
  amountCents: number;
  currency: string;
  receiptReference: string;
  paymentProvider: string;
  grantedResourceSlugs: string[];
  purchasedAt: string;
}

export interface PurchaseSummary {
  id: string;
  itemType: ItemType;
  slug: string;
  title: string;
  amountCents: number;
  receiptReference: string;
  purchasedAt: string;
  grantedResourceSlugs: string[];
}

export interface MyPurchasesResponse {
  currency: string;
  purchases: PurchaseSummary[];
}

export interface ReceiptLineItem {
  title: string;
}

export interface ReceiptResponse {
  receiptReference: string;
  purchasedAt: string;
  buyerEmail: string;
  itemType: ItemType;
  itemTitle: string;
  lineItems: ReceiptLineItem[];
  amountCents: number;
  currency: string;
  seller: string;
}

export interface ApiError {
  error: string;
  message: string;
}

export function formatEur(cents: number): string {
  return new Intl.NumberFormat("es-ES", {
    style: "currency",
    currency: "EUR",
  }).format(cents / 100);
}

async function apiCall<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${endpoint}`, {
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    ...options,
  });

  if (res.status === 204) return undefined as T;

  const data = await res.json();

  if (!res.ok) {
    throw data as ApiError;
  }

  return data as T;
}

export const getCatalog = () => apiCall<CatalogResponse>("/resources");

export const getResource = (slug: string) =>
  apiCall<ResourceDetail>(`/resources/${slug}`);

export const getResourceContent = (slug: string) =>
  apiCall<ContentResponse>(`/resources/${slug}/content`);

export const purchase = (itemType: ItemType, slug: string) =>
  apiCall<PurchaseResponse>("/purchases", {
    method: "POST",
    body: JSON.stringify({ itemType, slug }),
  });

export const listPurchases = () => apiCall<MyPurchasesResponse>("/purchases");

export const getReceipt = (id: string) =>
  apiCall<ReceiptResponse>(`/purchases/${id}/receipt`);
