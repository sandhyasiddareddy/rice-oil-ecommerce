"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { storefrontApi, type GuestOrderTracking } from "../../lib/api";
import { readGuestOrderAccess } from "../../lib/session";
import { OrderItems } from "../order-items";
import { ShipmentProgress } from "../shipment-progress";

export default function TrackOrderPage() {
  const [orderId, setOrderId] = useState("");
  const [accessToken, setAccessToken] = useState("");
  const [tracking, setTracking] = useState<GuestOrderTracking | null>(null);
  const [statusText, setStatusText] = useState("");
  const [busy, setBusy] = useState(false);
  const [aftercareMessage, setAftercareMessage] = useState("");

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const queryOrderId = params.get("orderId") ?? "";
    setOrderId(queryOrderId);
    if (queryOrderId && /^\d+$/.test(queryOrderId)) {
      const savedToken = params.get("token") || readGuestOrderAccess(Number(queryOrderId));
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

  async function requestCancellation() {
    if (!tracking || !window.confirm(`Submit a cancellation request for ${tracking.orderNumber}?`)) {
      return;
    }
    setBusy(true);
    setAftercareMessage("");
    try {
      await storefrontApi.requestCancellation(tracking.orderId, null, accessToken);
      setAftercareMessage("Your cancellation request was submitted for staff review.");
      await loadTracking(tracking.orderId, accessToken);
    } catch (error) {
      setAftercareMessage(error instanceof Error ? error.message : "Could not submit the cancellation request.");
    } finally {
      setBusy(false);
    }
  }

  async function requestRefund(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!tracking) {
      return;
    }
    const form = new FormData(event.currentTarget);
    setBusy(true);
    setAftercareMessage("");
    try {
      const result = await storefrontApi.requestRefund(
        tracking.orderId,
        Number(form.get("amount")),
        String(form.get("reason") ?? "").trim() || null,
        accessToken
      );
      setAftercareMessage(`Refund request ${result.id} was submitted for staff review.`);
    } catch (error) {
      setAftercareMessage(error instanceof Error ? error.message : "Could not submit the refund request.");
    } finally {
      setBusy(false);
    }
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
      {aftercareMessage ? <p className="status" role="status">{aftercareMessage}</p> : null}

      {tracking ? (
        <section className="contentPanel">
          <h2>{tracking.orderNumber}</h2>
          <p>Order status: <strong>{tracking.status.replaceAll("_", " ")}</strong></p>
          <p>Total: <strong>{tracking.currency} {Number(tracking.totalAmount).toFixed(2)}</strong></p>
          <OrderItems items={tracking.items} shippingAmount={tracking.shippingAmount} currency={tracking.currency} />
          <ShipmentProgress orderStatus={tracking.status} shipments={tracking.shipments} />
          {tracking.canRequestCancellation ? (
            <button type="button" onClick={() => void requestCancellation()} disabled={busy}>Cancel order</button>
          ) : null}
          {tracking.status === "PAID" ? (
            <form className="checkoutForm" onSubmit={(event) => void requestRefund(event)}>
              <h3>Request a refund</h3>
              <p>Refund requests are reviewed by staff before being sent to Razorpay.</p>
              <label htmlFor="guest-refund-amount">Amount ({tracking.currency})</label>
              <input
                id="guest-refund-amount"
                name="amount"
                type="number"
                min="0.01"
                max={tracking.totalAmount}
                step="0.01"
                defaultValue={tracking.totalAmount}
                required
              />
              <label htmlFor="guest-refund-reason">Reason (optional)</label>
              <input id="guest-refund-reason" name="reason" maxLength={300} />
              <button type="submit" disabled={busy}>Submit refund request</button>
            </form>
          ) : null}
          {tracking.shipments.length ? tracking.shipments.map((shipment) => (
            <article key={shipment.id} className="rowCard">
              <div>
                <h3>{shipment.provider} · {shipment.status.replaceAll("_", " ")}</h3>
                {shipment.status !== "DELIVERED" && shipment.status !== "CANCELLED" ? <p className="privacyNote">Estimated delivery: 5-7 working days after dispatch.</p> : null}
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
            <p className="privacyNote">Your order is confirmed and being prepared. Estimated delivery: 5-7 working days after dispatch.</p>
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
