export type CatalogCategory = {
  id: number;
  name: string;
  slug: string;
  description: string | null;
};

export type CatalogProduct = {
  id: number;
  name: string;
  slug: string;
  categorySlug: string;
  description: string | null;
  startingPrice: number;
  currency: string;
};

export type CatalogVariant = {
  id: number;
  sku: string;
  packSize: number;
  unit: string;
  weightGrams: number;
  regularPrice: number;
  currency: string;
};

export type CatalogProductDetail = {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  categorySlug: string;
  variants: CatalogVariant[];
};

export type CartItem = {
  id: number;
  sku: string;
  productName: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
};

export type Cart = {
  id: number;
  status: string;
  currency: string;
  taxCode: string | null;
  subtotalAmount: number;
  taxAmount: number;
  discountAmount: number;
  totalAmount: number;
  couponCodes: string[];
  items: CartItem[];
  guestAccessToken?: string | null;
};

export type CheckoutQuote = {
  baseAmount: number;
  taxAmount: number;
  preDiscountTotal: number;
  taxCode: string | null;
  taxInclusive: boolean;
  appliedCoupon: string | null;
  discountAmount: number;
  finalAmount: number;
  rejectedCoupons: string[];
};

export type Order = {
  id: number;
  orderNumber: string;
  cartId: number;
  status: string;
  currency: string;
  subtotalAmount: number;
  taxAmount: number;
  discountAmount: number;
  totalAmount: number;
  customerEmail: string | null;
  guestAccessToken?: string | null;
};

export type ShipmentEvent = {
  status: string;
  description: string | null;
  occurredAt: string;
};

export type Shipment = {
  id: number;
  orderId: number;
  shipmentCode: string;
  provider: string;
  trackingNumber: string | null;
  trackingUrl: string | null;
  status: string;
  events: ShipmentEvent[];
};

export type GuestOrderTracking = {
  orderId: number;
  orderNumber: string;
  status: string;
  shipments: Shipment[];
};

export type PaymentOrder = {
  orderId: number;
  providerOrderId: string;
  currency: string;
  amount: number;
  status: string;
};

export type VerifiedPayment = {
  orderId: number;
  paymentId: string;
  status: string;
  verified: boolean;
};

export type ShippingQuote = {
  quoteCode: string;
  cartId: number;
  status: string;
  shippingFee: number;
  currency: string;
  provider: string | null;
  serviceLevel: string | null;
  expiresAt: string;
};

export type CustomerOrder = {
  id: number;
  orderNumber: string;
  status: string;
  currency: string;
  totalAmount: number;
  customerEmail: string;
};

export type AuthSession = {
  token: string;
  email: string;
  fullName: string;
  role: string;
};

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export class ApiRequestError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message);
    this.name = "ApiRequestError";
  }
}

function guestOrderHeaders(orderId: number): Record<string, string> {
  if (typeof window === "undefined") {
    return {};
  }
  try {
    const token = window.sessionStorage.getItem(`riceoil.guest-order.${orderId}`);
    return token ? { "X-Guest-Order-Token": token } : {};
  } catch {
    return {};
  }
}

async function request<T>(
  path: string,
  init?: RequestInit,
  guestCartToken?: string | null
): Promise<T> {
  let token: string | null = null;
  if (typeof window !== "undefined") {
    try {
      token = window.sessionStorage.getItem("riceoil.auth.token");
    } catch {
      token = null;
    }
  }
  let response: Response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...init,
      headers: {
        "Content-Type": "application/json",
        ...(guestCartToken ? { "X-Guest-Cart-Token": guestCartToken } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(init?.headers ?? {})
      }
    });
  } catch {
    throw new Error("We couldn't connect to Farmora. Check your connection and try again.");
  }

  if (!response.ok) {
    throw new ApiRequestError(response.status, safeErrorMessage(path, response.status));
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

function safeErrorMessage(path: string, status: number): string {
  if (path === "/api/auth/login" && (status === 400 || status === 401)) {
    return "Email or password is incorrect.";
  }
  if (path === "/api/auth/register" && (status === 400 || status === 409)) {
    return "We couldn't create your account. Check your details or sign in instead.";
  }
  if (status === 400) {
    return "Please check your information and try again.";
  }
  if (status === 401) {
    return "Your session may have expired. Sign in and try again.";
  }
  if (status === 403) {
    return "You don't have permission to do that.";
  }
  if (status === 404) {
    return "We couldn't find what you requested.";
  }
  if (status === 409) {
    return "We couldn't complete that request. Review your details and try again.";
  }
  if (status === 429) {
    return "Too many attempts. Please wait a moment and try again.";
  }
  return "We couldn't complete that request. Please try again.";
}

export const storefrontApi = {
  register: (fullName: string, email: string, password: string) =>
    request<AuthSession>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({ fullName, email, password })
    }),
  login: (email: string, password: string) =>
    request<AuthSession>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password })
    }),
  listCategories: () => request<CatalogCategory[]>("/api/catalog/categories"),
  listProducts: (category?: string) =>
    request<CatalogProduct[]>(
      category ? `/api/catalog/products?category=${encodeURIComponent(category)}` : "/api/catalog/products"
    ),
  searchProducts: (query: string) =>
    request<CatalogProduct[]>(`/api/catalog/search?q=${encodeURIComponent(query)}`),
  getProduct: (slugOrSku: string) => request<CatalogProductDetail>(`/api/catalog/products/${slugOrSku}`),
  createCart: () => request<Cart>("/api/carts", { method: "POST", body: JSON.stringify({ currency: "INR" }) }),
  getOrCreateMyCart: (guestCartId: number | null, guestCartToken: string | null = null) =>
    request<Cart>("/api/customers/me/cart", {
      method: "POST",
      body: JSON.stringify({ guestCartId })
    }, guestCartToken),
  getCart: (cartId: number, guestCartToken: string | null = null) =>
    request<Cart>(`/api/carts/${cartId}`, undefined, guestCartToken),
  addItem: (
    cartId: number,
    input: { sku: string; productName: string; unitPrice: number; quantity: number },
    guestCartToken: string | null = null
  ) =>
    request<Cart>(`/api/carts/${cartId}/items`, { method: "POST", body: JSON.stringify(input) }, guestCartToken),
  updateItem: (
    cartId: number,
    itemId: number,
    input: { unitPrice?: number; quantity?: number },
    guestCartToken: string | null = null
  ) =>
    request<Cart>(`/api/carts/${cartId}/items/${itemId}`, { method: "PATCH", body: JSON.stringify(input) }, guestCartToken),
  removeItem: (cartId: number, itemId: number, guestCartToken: string | null = null) =>
    request<Cart>(`/api/carts/${cartId}/items/${itemId}`, { method: "DELETE" }, guestCartToken),
  refreshPricing: (cartId: number, taxCode: string, couponCodes: string[], guestCartToken: string | null = null) =>
    request<Cart>(`/api/carts/${cartId}/pricing/refresh`, {
      method: "POST",
      body: JSON.stringify({ taxCode, couponCodes })
    }, guestCartToken),
  quote: (subtotal: number, taxCode: string, couponCodes: string[], customerEmail: string) =>
    request<CheckoutQuote>("/api/checkout/quote", {
      method: "POST",
      body: JSON.stringify({ subtotal, taxCode, couponCodes, customerEmail })
    }),
  placeOrder: (idempotencyKey: string, cartId: number, customerEmail: string, guestCartToken: string | null = null) =>
    request<Order>("/api/checkout/orders", {
      method: "POST",
      headers: { "Idempotency-Key": idempotencyKey },
      body: JSON.stringify({ cartId, customerEmail })
    }, guestCartToken),
  createPaymentOrder: (orderId: number) =>
    request<PaymentOrder>("/api/payments/razorpay/create-order", {
      method: "POST",
      headers: guestOrderHeaders(orderId),
      body: JSON.stringify({ orderId })
    }),
  verifyPayment: (orderId: number, paymentId: string, signature: string) =>
    request<VerifiedPayment>("/api/payments/razorpay/verify", {
      method: "POST",
      headers: guestOrderHeaders(orderId),
      body: JSON.stringify({ orderId, paymentId, signature })
    }),
  createShippingQuote: (cartId: number, guestCartToken: string | null = null) =>
    request<ShippingQuote>("/api/shipping/quotes", {
      method: "POST",
      body: JSON.stringify({ cartId })
    }, guestCartToken),
  listMyOrders: () => request<CustomerOrder[]>("/api/customers/me/orders"),
  getMyOrder: (orderId: number) => request<CustomerOrder>(`/api/customers/me/orders/${orderId}`),
  getGuestOrderTracking: (orderId: number, accessToken: string) =>
    request<GuestOrderTracking>(`/api/guest/orders/${orderId}/tracking`, {
      headers: { "X-Guest-Token": accessToken }
    })
};
