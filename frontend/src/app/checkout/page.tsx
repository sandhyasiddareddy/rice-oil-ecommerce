"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { storefrontApi, type Cart, type Order } from "../../lib/api";
import {
  ensureCart,
  readAccessToken,
  readGuestCartAccessToken,
  saveGuestOrderAccess
} from "../../lib/session";

export default function CheckoutPage() {
  const [cart, setCart] = useState<Cart | null>(null);
  const [customerEmail, setCustomerEmail] = useState("");
  const [signedIn, setSignedIn] = useState(false);
  const [order, setOrder] = useState<Order | null>(null);
  const [statusText, setStatusText] = useState("Loading your cart...");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    setSignedIn(Boolean(readAccessToken()));
    void loadCart();
  }, []);

  async function loadCart() {
    try {
      const currentCart = await ensureCart();
      setCart(currentCart);
      setStatusText("");
    } catch (error) {
      setStatusText(normalizeError(error));
    }
  }

  async function submitOrder(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!cart || cart.items.length === 0) {
      return;
    }

    setBusy(true);
    try {
      const nextOrder = await storefrontApi.placeOrder(
        `idem-${crypto.randomUUID()}`,
        cart.id,
        customerEmail.trim().toLowerCase(),
        readGuestCartAccessToken()
      );
      if (nextOrder.guestAccessToken) {
        saveGuestOrderAccess(nextOrder.id, nextOrder.guestAccessToken);
      }
      setOrder(nextOrder);
      setStatusText("");
    } catch (error) {
      setStatusText(normalizeError(error));
    } finally {
      setBusy(false);
    }
  }

  if (order) {
    return (
      <main className="page">
        <section className="pageHeading">
          <p className="eyebrow">Order request received</p>
          <h1>Thank you</h1>
          <p className="subtitle">Your order reference is {order.orderNumber}.</p>
        </section>
        <section className="contentPanel">
          <p>This preview does not collect payment. Your order is recorded as {order.status.toLowerCase()}.</p>
          {order.guestAccessToken ? (
            <p className="privacyNote">Guest tracking access is saved for this browser session and expires in 30 days.</p>
          ) : null}
          <div className="inlineActions">
            {order.guestAccessToken ? (
              <Link href={`/track-order?orderId=${order.id}`} className="buttonLink">Track this order</Link>
            ) : null}
          <Link href="/catalog" className="buttonLink">Continue shopping</Link>
          </div>
        </section>
      </main>
    );
  }

  return (
    <main className="page">
      <section className="pageHeading">
        <p className="eyebrow">Checkout · Step 1 of 1</p>
        <h1>Contact for this order</h1>
        <p className="subtitle">Review your total, then enter an email to associate with the order.</p>
      </section>

      {statusText ? <p className="status" role="status">{statusText}</p> : null}

      {cart?.items.length ? (
        <div className="checkoutLayout">
          <form className="contentPanel checkoutForm" onSubmit={(event) => void submitOrder(event)}>
            {signedIn ? (
              <p className="privacyNote">This order will be linked to your signed-in account.</p>
            ) : (
              <>
                <label htmlFor="checkout-email">Email for this order</label>
                <input
                  id="checkout-email"
                  type="email"
                  autoComplete="email"
                  required
                  value={customerEmail}
                  onChange={(event) => setCustomerEmail(event.target.value)}
                  placeholder="you@example.com"
                />
                <p className="privacyNote">Used only to associate this guest order. It is not saved in this browser.</p>
              </>
            )}
            <button type="submit" disabled={busy || (!signedIn && !customerEmail.trim())}>
              {busy ? "Submitting..." : "Submit order request"}
            </button>
          </form>

          <aside className="contentPanel orderSummary">
            <h2>Order summary</h2>
            <p>{cart.items.length} {cart.items.length === 1 ? "item" : "items"}</p>
            <p>Subtotal <strong>INR {Number(cart.subtotalAmount).toFixed(2)}</strong></p>
            <p>Tax <strong>INR {Number(cart.taxAmount).toFixed(2)}</strong></p>
            <p>Discount <strong>INR {Number(cart.discountAmount).toFixed(2)}</strong></p>
            <p className="orderTotal">Total <strong>INR {Number(cart.totalAmount).toFixed(2)}</strong></p>
            <p className="privacyNote">No payment is collected in this preview.</p>
          </aside>
        </div>
      ) : cart ? (
        <section className="emptyState">
          <h2>Your cart is empty</h2>
          <p>Add something to your cart before checking out.</p>
          <Link href="/catalog" className="buttonLink">Browse products</Link>
        </section>
      ) : null}
    </main>
  );
}

function normalizeError(error: unknown): string {
  if (error instanceof Error) {
    return error.message;
  }
  return "We could not load checkout. Please try again.";
}