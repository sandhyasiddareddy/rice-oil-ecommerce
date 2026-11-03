"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { storefrontApi, type GuestOrderTracking } from "../../lib/api";
import { readGuestOrderAccess } from "../../lib/session";

export default function TrackOrderPage() {
  const [orderId, setOrderId] = useState("");
  const [accessToken, setAccessToken] = useState("");
  const [tracking, setTracking] = useState<GuestOrderTracking | null>(null);
  const [statusText, setStatusText] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const queryOrderId = new URLSearchParams(window.location.search).get("orderId") ?? "";
    setOrderId(queryOrderId);
    if (queryOrderId && /^\d+$/.test(queryOrderId)) {
      const savedToken = readGuestOrderAccess(Number(queryOrderId));
      if (savedToken) {
        setAccessToken(savedToken);
        void loadTracking(Number(queryOrderId), savedToken);
      }
    }
  }, []);

  async function loadTracking(id: number, token: string) {
    setBusy(true);
    setStatusText("");
    try {
      setTracking(await storefrontApi.getGuestOrderTracking(id, token));
    } catch (error) {
      setTracking(null);
      setStatusText(error instanceof Error ? error.message : "Could not load tracking details.");
    } finally {
      setBusy(false);
    }
  }

  function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const id = Number(orderId);
    if (!Number.isSafeInteger(id) || id <= 0 || !accessToken.trim()) {
      setStatusText("Enter a valid order ID and guest tracking token.");
      return;
    }
    void loadTracking(id, accessToken.trim());
  }

  return (
    <main className="page">
      <section className="pageHeading">
        <p className="eyebrow">Guest order tracking</p>
        <h1>Track your order</h1>
        <p className="subtitle">Use the order ID and private tracking token shown after guest checkout.</p>
      </section>

      <form className="contentPanel checkoutForm" onSubmit={submit}>
        <label htmlFor="guest-order-id">Order ID</label>
        <input id="guest-order-id" inputMode="numeric" required value={orderId} onChange={(event) => setOrderId(event.target.value)} />
        <label htmlFor="guest-access-token">Guest tracking token</label>
        <input id="guest-access-token" autoComplete="off" required value={accessToken} onChange={(event) => setAccessToken(event.target.value)} />
        <button type="submit" disabled={busy}>{busy ? "Loading..." : "View tracking"}</button>
      </form>

      {statusText ? <p className="status" role="status">{statusText}</p> : null}

      {tracking ? (
        <section className="contentPanel">
          <h2>{tracking.orderNumber}</h2>
          <p>Order status: <strong>{tracking.status.replaceAll("_", " ")}</strong></p>
          {tracking.shipments.length ? tracking.shipments.map((shipment) => (
            <article key={shipment.id} className="rowCard">
              <div>
                <h3>{shipment.provider} · {shipment.status.replaceAll("_", " ")}</h3>
                {shipment.trackingNumber ? <p>Tracking number: <strong>{shipment.trackingNumber}</strong></p> : null}
                {validTrackingUrl(shipment.trackingUrl) ? (
                  <p><a href={shipment.trackingUrl} target="_blank" rel="noreferrer">Open carrier tracking</a></p>
                ) : null}
                {shipment.events.length ? (
                  <ol className="stackList">
                    {shipment.events.map((event, index) => (
                      <li key={`${event.occurredAt}-${index}`}>
                        <strong>{event.status.replaceAll("_", " ")}</strong>
                        {event.description ? ` — ${event.description}` : ""}
                        <span className="privacyNote"> · {new Date(event.occurredAt).toLocaleString()}</span>
                      </li>
                    ))}
                  </ol>
                ) : null}
              </div>
            </article>
          )) : (
            <p className="privacyNote">A shipment has not been created for this order yet.</p>
          )}
        </section>
      ) : null}

      <p className="privacyNote"><Link href="/catalog">Continue shopping</Link></p>
    </main>
  );
}

function validTrackingUrl(value: string | null): value is string {
  if (!value) {
    return false;
  }
  try {
    return new URL(value).protocol === "https:";
  } catch {
    return false;
  }
}
