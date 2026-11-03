"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { storefrontApi, type Cart } from "../../lib/api";
import { ensureCart, readGuestCartAccessToken } from "../../lib/session";

const DEFAULT_TAX_CODE = "GST5";

export default function CartPage() {
  const [cart, setCart] = useState<Cart | null>(null);
  const [couponText, setCouponText] = useState("");
  const [statusText, setStatusText] = useState("Loading your cart...");
  const [busyAction, setBusyAction] = useState("");

  useEffect(() => {
    void loadCart();
  }, []);

  async function loadCart() {
    try {
      const currentCart = await ensureCart();
      setCart(currentCart);
      setStatusText("Cart is ready.");
    } catch (error) {
      setStatusText(normalizeError(error));
    }
  }

  async function updateQuantity(itemId: number, quantity: number) {
    if (!cart) {
      return;
    }
    setBusyAction(`update:${itemId}`);
    try {
      const nextQuantity = Math.max(1, quantity);
      const updated = await storefrontApi.updateItem(
        cart.id,
        itemId,
        { quantity: nextQuantity },
        readGuestCartAccessToken()
      );
      setCart(updated);
      setStatusText("Item updated.");
    } catch (error) {
      setStatusText(normalizeError(error));
    } finally {
      setBusyAction("");
    }
  }

  async function removeItem(itemId: number) {
    if (!cart) {
      return;
    }
    setBusyAction(`remove:${itemId}`);
    try {
      const updated = await storefrontApi.removeItem(cart.id, itemId, readGuestCartAccessToken());
      setCart(updated);
      setStatusText("Item removed.");
    } catch (error) {
      setStatusText(normalizeError(error));
    } finally {
      setBusyAction("");
    }
  }

  async function refreshPricing() {
    if (!cart) {
      return;
    }
    setBusyAction("pricing");
    try {
      const couponCodes = couponText
        .split(",")
        .map((code) => code.trim())
        .filter(Boolean);

      const updated = await storefrontApi.refreshPricing(
        cart.id,
        DEFAULT_TAX_CODE,
        couponCodes,
        readGuestCartAccessToken()
      );
      setCart(updated);
      setStatusText("Cart totals updated.");
    } catch (error) {
      setStatusText(normalizeError(error));
    } finally {
      setBusyAction("");
    }
  }

  return (
    <main className="page">
      <section className="pageHeading">
        <p className="eyebrow">Your Cart</p>
        <h1>Shopping cart</h1>
        <p className="subtitle">Check quantities and totals before checkout.</p>
      </section>

      {statusText && !cart ? <p className="status" role="status">{statusText}</p> : null}

      {cart?.items.length ? (
        <div className="cartLayout">
          <section className="contentPanel cartItems">
            <h2>{cart.items.length} {cart.items.length === 1 ? "item" : "items"}</h2>
            <div className="stackList">
              {cart.items.map((item) => (
                <article key={item.id} className="rowCard">
                  <div>
                    <h3>{item.productName}</h3>
                    <p>{item.sku} · INR {Number(item.unitPrice).toFixed(2)} each</p>
                    <strong>INR {Number(item.lineTotal).toFixed(2)}</strong>
                  </div>
                  <div className="inlineActions quantityActions">
                    <button
                      type="button"
                      className="ghost"
                      aria-label={`Decrease ${item.productName} quantity`}
                      onClick={() => void updateQuantity(item.id, item.quantity - 1)}
                      disabled={busyAction === `update:${item.id}` || item.quantity <= 1}
                    >
                      −
                    </button>
                    <span className="qtyBadge" aria-live="polite">{item.quantity}</span>
                    <button
                      type="button"
                      className="ghost"
                      aria-label={`Increase ${item.productName} quantity`}
                      onClick={() => void updateQuantity(item.id, item.quantity + 1)}
                      disabled={busyAction === `update:${item.id}`}
                    >
                      +
                    </button>
                    <button
                      type="button"
                      className="textButton"
                      onClick={() => void removeItem(item.id)}
                      disabled={busyAction === `remove:${item.id}`}
                    >
                      Remove
                    </button>
                  </div>
                </article>
              ))}
            </div>
          </section>

          <aside className="contentPanel cartSummaryPanel">
            <h2>Order summary</h2>
            <p>Subtotal <strong>INR {Number(cart.subtotalAmount).toFixed(2)}</strong></p>
            <p>Tax <strong>INR {Number(cart.taxAmount).toFixed(2)}</strong></p>
            <p>Discount <strong>INR {Number(cart.discountAmount).toFixed(2)}</strong></p>
            <p className="orderTotal">Total <strong>INR {Number(cart.totalAmount).toFixed(2)}</strong></p>
            <label htmlFor="coupon-code">Coupon code</label>
            <input
              id="coupon-code"
              value={couponText}
              onChange={(event) => setCouponText(event.target.value)}
              placeholder="Enter code"
            />
            <button type="button" className="ghost" onClick={refreshPricing} disabled={busyAction === "pricing"}>
              {busyAction === "pricing" ? "Applying..." : "Apply coupon"}
            </button>
            {statusText ? <p className="privacyNote" role="status">{statusText}</p> : null}
            <Link href="/checkout" className="buttonLink">Continue to checkout</Link>
          </aside>
        </div>
      ) : cart ? (
        <section className="emptyState">
          <h2>Your cart is empty</h2>
          <p>Browse the catalog to find something for your pantry.</p>
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
  return "Unexpected error";
}
