"use client";

// Guest state survives tab/browser restarts for this long, then is discarded.
export const GUEST_SESSION_TTL_MS = 24 * 60 * 60 * 1000;

type Entry = { value: string; expiresAt: number };

function parse(raw: string | null): Entry | null {
  if (!raw) {
    return null;
  }
  try {
    const entry: unknown = JSON.parse(raw);
    if (
      entry && typeof entry === "object"
      && typeof (entry as Entry).value === "string"
      && typeof (entry as Entry).expiresAt === "number"
    ) {
      return entry as Entry;
    }
  } catch {
    // Fall through: unreadable entries are treated as missing.
  }
  return null;
}

export function readGuestItem(key: string): string | null {
  if (typeof window === "undefined") {
    return null;
  }
  try {
    const entry = parse(window.localStorage.getItem(key));
    if (!entry || entry.expiresAt <= Date.now()) {
      window.localStorage.removeItem(key);
      return null;
    }
    return entry.value;
  } catch {
    return null;
  }
}

// Rewriting the same value keeps its original expiry, so the 24h window is not extended on every read/write cycle.
export function writeGuestItem(key: string, value: string): void {
  const existing = parse(window.localStorage.getItem(key));
  const expiresAt = existing && existing.value === value && existing.expiresAt > Date.now()
    ? existing.expiresAt
    : Date.now() + GUEST_SESSION_TTL_MS;
  window.localStorage.setItem(key, JSON.stringify({ value, expiresAt } satisfies Entry));
}

export function removeGuestItem(key: string): void {
  if (typeof window === "undefined") {
    return;
  }
  try {
    window.localStorage.removeItem(key);
  } catch {
    return;
  }
}

export function guestOrderTokenKey(orderId: number | string): string {
  return `riceoil.guest-order.${orderId}`;
}
