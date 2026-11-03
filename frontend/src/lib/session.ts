"use client";

import { ApiRequestError, storefrontApi, type Cart } from "./api";

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

function canUseStorage(): boolean {
  return typeof window !== "undefined";
}

export function clearLegacySessionData(): void {
  if (!canUseStorage()) {
    return;
  }
  try {
    LEGACY_PRIVATE_KEYS.forEach((key) => window.localStorage.removeItem(key));
    window.localStorage.removeItem(CART_ID_KEY);
  } catch {
    // Storage may be disabled by the browser.
  }
}

export function readCartId(): number | null {
  if (!canUseStorage()) {
    return null;
  }
  try {
    const raw = window.sessionStorage.getItem(CART_ID_KEY);
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
    window.sessionStorage.setItem(CART_ID_KEY, String(cartId));
  } catch (error) {
    throw new Error("Cart access could not be saved in this browser session.", { cause: error });
  }
}

function clearCartId(): void {
  if (!canUseStorage()) {
    return;
  }
  try {
    window.sessionStorage.removeItem(CART_ID_KEY);
  } catch {
    return;
  }
}

export function readGuestCartAccessToken(): string | null {
  if (!canUseStorage()) {
    return null;
  }
  try {
    return window.sessionStorage.getItem(CART_ACCESS_TOKEN_KEY);
  } catch {
    return null;
  }
}

export function saveGuestCartAccessToken(token: string): void {
  if (!canUseStorage()) {
    throw new Error("Guest cart access cannot be saved in this browser session.");
  }
  try {
    window.sessionStorage.setItem(CART_ACCESS_TOKEN_KEY, token);
  } catch (error) {
    throw new Error("Guest cart access could not be saved in this browser session.", { cause: error });
  }
}

export function clearGuestCartAccessToken(): void {
  if (!canUseStorage()) {
    return;
  }
  try {
    window.sessionStorage.removeItem(CART_ACCESS_TOKEN_KEY);
  } catch {
    return;
  }
}

export function saveGuestOrderAccess(orderId: number, token: string): void {
  if (!canUseStorage()) {
    return;
  }
  try {
    window.sessionStorage.setItem(`riceoil.guest-order.${orderId}`, token);
  } catch {
    return;
  }
}

export function readGuestOrderAccess(orderId: number): string | null {
  if (!canUseStorage()) {
    return null;
  }
  try {
    return window.sessionStorage.getItem(`riceoil.guest-order.${orderId}`);
  } catch {
    return null;
  }
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
    window.sessionStorage.removeItem(CART_ID_KEY);
    window.sessionStorage.removeItem(CART_ACCESS_TOKEN_KEY);
    window.localStorage.removeItem(AUTH_TOKEN_KEY);
    window.localStorage.removeItem(AUTH_NAME_KEY);
    window.localStorage.removeItem("riceoil.auth.email");
  } catch {
    return;
  }
}

export async function ensureCart(): Promise<Cart> {
  if (readAccessToken()) {
    return ensureCustomerCart();
  }

  const existing = readCartId();
  if (existing) {
    try {
      const cart = await storefrontApi.getCart(existing, readGuestCartAccessToken());
      writeCartId(cart.id);
      return cart;
    } catch (error) {
      if (!(error instanceof ApiRequestError) || error.status !== 404) {
        throw error;
      }
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
