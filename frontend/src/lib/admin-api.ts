export type AdminCredentials = {
  username: string;
  password: string;
};

export type AdminCategory = {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  active: boolean;
};

export type AdminProduct = {
  id: number;
  categoryId: number;
  name: string;
  slug: string;
  description: string | null;
  attributesJson: string | null;
  published: boolean;
};

export type AdminVariant = {
  id: number;
  productId: number;
  sku: string;
  packSize: number;
  unit: string;
  weightGrams: number;
  regularPrice: number;
  currency: string;
  active: boolean;
};

export type ImportPreview = {
  jobId: number;
  totalRows: number;
  validRows: number;
  invalidRows: number;
};

export type ImportResult = {
  jobId: number;
  appliedRows: number;
  createdCategories: number;
  createdProducts: number;
  createdVariants: number;
};

export type AdminAftercareRequest = {
  id: number;
  orderId: number;
  orderNumber: string | null;
  customerEmail: string | null;
  amount: number | null;
  status: string;
  reason: string | null;
  providerRefundId: string | null;
  providerStatus: string | null;
};

export type FulfilmentOrder = {
  orderId: number;
  orderNumber: string;
  customerEmail: string | null;
  totalAmount: number;
  shippingAddressJson: string | null;
  paidAt: string | null;
  fulfilmentStatus: string;
  provider: string | null;
  trackingNumber: string | null;
};

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

function authorization(credentials: AdminCredentials): string {
  const bytes = new TextEncoder().encode(`${credentials.username}:${credentials.password}`);
  let binary = "";
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });
  return `Basic ${btoa(binary)}`;
}

export class StaffAuthError extends Error {
  constructor() {
    super("Staff access could not be verified.");
    this.name = "StaffAuthError";
  }
}

async function request<T>(
  credentials: AdminCredentials,
  path: string,
  init?: RequestInit
): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: {
      ...(init?.body instanceof FormData ? {} : { "Content-Type": "application/json" }),
      Authorization: authorization(credentials),
      ...(init?.headers ?? {})
    }
  });

  if (!response.ok) {
    if (response.status === 401 || response.status === 403) {
      throw new StaffAuthError();
    }
    if (response.status === 400 || response.status === 409) {
      throw new Error("Please review the information and try again.");
    }
    throw new Error("The admin request could not be completed. Try again.");
  }

  if (response.status === 204) {
    return undefined as T;
  }
  if (response.headers.get("content-type")?.includes("json")) {
    return response.json() as Promise<T>;
  }
  return response.blob() as Promise<T>;
}

export const adminApi = {
  verifyCredentials: (credentials: AdminCredentials) =>
    request<Blob>(credentials, "/api/admin/imports/template"),
  createCategory: (credentials: AdminCredentials, input: Omit<AdminCategory, "id">) =>
    request<AdminCategory>(credentials, "/api/admin/catalog/categories", {
      method: "POST",
      body: JSON.stringify(input)
    }),
  createProduct: (credentials: AdminCredentials, input: {
    categoryId: number;
    name: string;
    slug: string;
    description: string;
    attributesJson: string;
    published: boolean;
  }) => request<AdminProduct>(credentials, "/api/admin/catalog/products", {
    method: "POST",
    body: JSON.stringify(input)
  }),
  createVariant: (credentials: AdminCredentials, input: {
    productId: number;
    sku: string;
    packSize: number;
    unit: "KG" | "G" | "L" | "ML";
    weightGrams: number;
    regularPrice: number;
    active: boolean;
  }) => request<AdminVariant>(credentials, "/api/admin/catalog/variants", {
    method: "POST",
    body: JSON.stringify(input)
  }),
  adjustInventory: (credentials: AdminCredentials, input: {
    variantId: number;
    deltaQty: number;
    reason: string;
    reference: string;
    createdBy: string;
  }) => request<{ variantId: number; availableQty: number; reservedQty: number }>(
    credentials,
    "/api/admin/inventory/adjustments",
    { method: "POST", body: JSON.stringify(input) }
  ),
  listAftercareRefunds: (credentials: AdminCredentials) =>
    request<AdminAftercareRequest[]>(credentials, "/api/admin/aftercare/refunds"),
  approveAftercareRefund: (credentials: AdminCredentials, requestId: number) =>
    request<void>(credentials, `/api/admin/aftercare/refunds/${requestId}/approve`, {
      method: "POST"
    }),
  rejectAftercareRefund: (credentials: AdminCredentials, requestId: number) =>
    request<void>(credentials, `/api/admin/aftercare/refunds/${requestId}/reject`, { method: "POST" }),
  listAftercareCancellations: (credentials: AdminCredentials) =>
    request<AdminAftercareRequest[]>(credentials, "/api/admin/aftercare/cancellations"),
  approveAftercareCancellation: (credentials: AdminCredentials, requestId: number) =>
    request<void>(credentials, `/api/admin/aftercare/cancellations/${requestId}/approve`, { method: "POST" }),
  rejectAftercareCancellation: (credentials: AdminCredentials, requestId: number) =>
    request<void>(credentials, `/api/admin/aftercare/cancellations/${requestId}/reject`, { method: "POST" }),
  listFulfilmentOrders: (credentials: AdminCredentials) =>
    request<FulfilmentOrder[]>(credentials, "/api/admin/shipments/orders"),
  updateShipment: (credentials: AdminCredentials, input: {
    orderNumber: string;
    status: string;
    provider?: string;
    trackingNumber?: string;
    trackingUrl?: string;
    note?: string;
  }) =>
    request<unknown>(credentials, "/api/admin/shipments/update", {
      method: "POST",
      body: JSON.stringify(input)
    }),
  createPromotion: (credentials: AdminCredentials, input: {
    code: string;
    discountType: "PERCENTAGE" | "FIXED_AMOUNT";
    discountValue: number;
    minOrderValue: number | null;
    stackable: boolean;
    exclusionGroup: string | null;
    maxTotalRedemptions: number | null;
    maxRedemptionsPerCustomer: number | null;
    active: boolean;
    startsAt: string | null;
    endsAt: string | null;
  }) => request<{ id: number; code: string; active: boolean }>(
    credentials,
    "/api/admin/promotions",
    { method: "POST", body: JSON.stringify(input) }
  ),
  uploadProductImage: (credentials: AdminCredentials, productId: number, file: File, altText: string) => {
    const form = new FormData();
    form.append("file", file);
    const query = altText.trim() ? `?altText=${encodeURIComponent(altText.trim())}` : "";
    return request<{ id: number; imageUrl: string; altText: string | null }>(
      credentials,
      `/api/admin/catalog/products/${productId}/images${query}`,
      { method: "POST", body: form }
    );
  },
  downloadTemplate: (credentials: AdminCredentials) =>
    request<Blob>(credentials, "/api/admin/imports/template"),
  previewImport: (credentials: AdminCredentials, file: File) => {
    const form = new FormData();
    form.append("file", file);
    form.append("initiatedBy", credentials.username);
    return request<ImportPreview>(credentials, "/api/admin/imports/preview", {
      method: "POST",
      headers: { "Idempotency-Key": `admin-import-${crypto.randomUUID()}` },
      body: form
    });
  },
  confirmImport: (credentials: AdminCredentials, jobId: number) =>
    request<ImportResult>(credentials, `/api/admin/imports/${jobId}/confirm`, {
      method: "POST",
      body: JSON.stringify({ actor: credentials.username })
    }),
  downloadImportErrors: (credentials: AdminCredentials, jobId: number) =>
    request<Blob>(credentials, `/api/admin/imports/${jobId}/errors`)
};