"use client";

import { ApiRequestError, storefrontApi, type Cart } from "./api";
import { guestOrderTokenKey, readGuestItem, removeGuestItem, writeGuestItem } from "./guest-storage";

const CART_ID_KEY = "riceoil.cart.id";
const CART_ACCESS_TOKEN_KEY = "riceoil.cart.access-token";
const AUTH_TOKEN_KEY = "riceoil.auth.token";
const AUTH_NAME_KEY = "riceoil.auth.name";
const LEGACY_PRIVATE_KEYS = [
  "riceoil.last.order",
  "riceoil.customer.email",
  "riceoil.customer.role",
  AUTH_TOKEN_KEY,
  AUTH_NAME_KEY,
  "riceoil.auth.email"
];

let cartResolution: Promise<Cart> | null = null;

function canUseStorage(): boolean {
  return typeof window !== "undefined";
}

export function clearLegacySessionData(): void {
  if (!canUseStorage()) {
    return;
  }
  try {
    LEGACY_PRIVATE_KEYS.forEach((key) => window.localStorage.removeItem(key));
  } catch {
    // Storage may be disabled by the browser.
  }
}

export function readCartId(): number | null {
  if (!canUseStorage()) {
    return null;
  }
  try {
    const raw = readGuestItem(CART_ID_KEY);
    if (!raw) {
      return null;
    }
    const id = Number(raw);
    return Number.isSafeInteger(id) && id > 0 ? id : null;
  } catch {
    return null;
  }
}

export function writeCartId(cartId: number): void {
  if (!canUseStorage()) {
    throw new Error("Cart access cannot be saved in this browser session.");
  }
  try {
    writeGuestItem(CART_ID_KEY, String(cartId));
  } catch (error) {
    throw new Error("Cart access could not be saved in this browser session.", { cause: error });
  }
}

function clearCartId(): void {
  if (!canUseStorage()) {
    return;
  }
  removeGuestItem(CART_ID_KEY);
}

export function readGuestCartAccessToken(): string | null {
  return readGuestItem(CART_ACCESS_TOKEN_KEY);
}

export function saveGuestCartAccessToken(token: string): void {
  if (!canUseStorage()) {
    throw new Error("Guest cart access cannot be saved in this browser session.");
  }
  try {
    writeGuestItem(CART_ACCESS_TOKEN_KEY, token);
  } catch (error) {
    throw new Error("Guest cart access could not be saved in this browser session.", { cause: error });
  }
}

export function clearGuestCartAccessToken(): void {
  removeGuestItem(CART_ACCESS_TOKEN_KEY);
}

export function saveGuestOrderAccess(orderId: number, token: string): void {
  if (!canUseStorage()) {
    return;
  }
  try {
    writeGuestItem(guestOrderTokenKey(orderId), token);
  } catch {
    return;
  }
}

const GUEST_ORDERS_KEY = "riceoil.guest-orders";

export function readGuestOrderIds(): number[] {
  if (!canUseStorage()) {
    return [];
  }
  try {
    const parsed: unknown = JSON.parse(readGuestItem(GUEST_ORDERS_KEY) ?? "[]");
    return Array.isArray(parsed)
      ? parsed.filter((id): id is number => Number.isSafeInteger(id) && id > 0)
      : [];
  } catch {
    return [];
  }
}

export function rememberGuestOrder(orderId: number): void {
  if (!canUseStorage()) {
    return;
  }
  try {
    const ids = readGuestOrderIds().filter((id) => id !== orderId);
    writeGuestItem(GUEST_ORDERS_KEY, JSON.stringify([orderId, ...ids]));
  } catch {
    return;
  }
}

// The backend marks the cart CHECKED_OUT when the order is placed, so the browser must stop using it.
export function releaseCheckedOutCart(): void {
  clearCartId();
  clearGuestCartAccessToken();
  if (canUseStorage()) {
    window.dispatchEvent(new CustomEvent("farmora:cart-updated", { detail: { count: 0 } }));
  }
}

export function readGuestOrderAccess(orderId: number): string | null {
  if (!canUseStorage()) {
    return null;
  }
  return readGuestItem(guestOrderTokenKey(orderId));
}

export function readAccessToken(): string | null {
  if (!canUseStorage()) {
    return null;
  }
  try {
    return window.sessionStorage.getItem(AUTH_TOKEN_KEY);
  } catch {
    return null;
  }
}

export function saveCustomerSession(token: string, fullName: string): void {
  if (!canUseStorage()) {
    return;
  }
  try {
    window.sessionStorage.setItem(AUTH_TOKEN_KEY, token);
    window.sessionStorage.setItem(AUTH_NAME_KEY, fullName);
    window.localStorage.removeItem(AUTH_TOKEN_KEY);
    window.localStorage.removeItem(AUTH_NAME_KEY);
    window.localStorage.removeItem("riceoil.auth.email");
    window.dispatchEvent(new Event("farmora:customer-session-updated"));
  } catch {
    return;
  }
}

export function readCustomerName(): string | null {
  if (!canUseStorage()) {
    return null;
  }
  try {
    return window.sessionStorage.getItem(AUTH_NAME_KEY);
  } catch {
    return null;
  }
}

export function clearCustomerSession(): void {
  if (!canUseStorage()) {
    return;
  }
  try {
    window.sessionStorage.removeItem(AUTH_TOKEN_KEY);
    window.sessionStorage.removeItem(AUTH_NAME_KEY);
    window.sessionStorage.removeItem("riceoil.auth.email");
    removeGuestItem(CART_ID_KEY);
    removeGuestItem(CART_ACCESS_TOKEN_KEY);
    window.localStorage.removeItem(AUTH_TOKEN_KEY);
    window.localStorage.removeItem(AUTH_NAME_KEY);
    window.localStorage.removeItem("riceoil.auth.email");
  } catch {
    return;
  }
}

export function ensureCart(): Promise<Cart> {
  if (!cartResolution) {
    cartResolution = resolveCart().finally(() => {
      cartResolution = null;
    });
  }
  return cartResolution;
}

async function resolveCart(): Promise<Cart> {
  if (readAccessToken()) {
    return ensureCustomerCart();
  }

  const existing = readCartId();
  if (existing) {
    try {
      const cart = await storefrontApi.getCart(existing, readGuestCartAccessToken());
      if (cart.status === "ACTIVE") {
        writeCartId(cart.id);
        return cart;
      }
      clearCartId();
      clearGuestCartAccessToken();
    } catch (error) {
      if (!(error instanceof ApiRequestError) || ![403, 404].includes(error.status)) {
        throw error;
      }
      clearCartId();
      clearGuestCartAccessToken();
    }
  }

  const created = await storefrontApi.createCart();
  writeCartId(created.id);
  if (!created.guestAccessToken) {
    throw new Error("Guest cart access token was not returned by the server.");
  }
  saveGuestCartAccessToken(created.guestAccessToken);
  return created;
}

export async function ensureCustomerCart(): Promise<Cart> {
  const guestCartId = readCartId();
  let customerCart: Cart;
  try {
    customerCart = await storefrontApi.getOrCreateMyCart(guestCartId, readGuestCartAccessToken());
  } catch (error) {
    if (!(error instanceof ApiRequestError) || ![403, 404].includes(error.status) || guestCartId === null) {
      throw error;
    }

    clearCartId();
    clearGuestCartAccessToken();
    customerCart = await storefrontApi.getOrCreateMyCart(null);
  }

  clearGuestCartAccessToken();
  writeCartId(customerCart.id);
  return customerCart;
}
