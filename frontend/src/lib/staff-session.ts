"use client";

import type { AdminCredentials } from "./admin-api";

const STAFF_SESSION_KEY = "farmora.staff.session";
const STAFF_SESSION_EVENT = "farmora:staff-session-updated";

// Kept in sessionStorage so the staff session survives navigation but ends when the tab closes.
export function readStaffSession(): AdminCredentials | null {
  if (typeof window === "undefined") {
    return null;
  }
  try {
    const raw = window.sessionStorage.getItem(STAFF_SESSION_KEY);
    if (!raw) {
      return null;
    }
    const parsed: unknown = JSON.parse(raw);
    if (
      parsed && typeof parsed === "object"
      && typeof (parsed as AdminCredentials).username === "string"
      && typeof (parsed as AdminCredentials).password === "string"
    ) {
      return parsed as AdminCredentials;
    }
    return null;
  } catch {
    return null;
  }
}

export function saveStaffSession(credentials: AdminCredentials): void {
  if (typeof window === "undefined") {
    return;
  }
  try {
    window.sessionStorage.setItem(STAFF_SESSION_KEY, JSON.stringify(credentials));
    window.dispatchEvent(new Event(STAFF_SESSION_EVENT));
  } catch {
    return;
  }
}

export function clearStaffSession(): void {
  if (typeof window === "undefined") {
    return;
  }
  try {
    window.sessionStorage.removeItem(STAFF_SESSION_KEY);
    window.dispatchEvent(new Event(STAFF_SESSION_EVENT));
  } catch {
    return;
  }
}

export const staffSessionEvent = STAFF_SESSION_EVENT;
