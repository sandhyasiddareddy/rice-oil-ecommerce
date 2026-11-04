"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { storefrontApi, type CustomerOrder } from "../../lib/api";
import { startOrderPayment } from "../../lib/razorpay";
import { readAccessToken, readGuestOrderAccess, readGuestOrderIds } from "../../lib/session";
import { OrderItems } from "../order-items";
import { ShipmentProgress } from "../shipment-progress";

type OrderRow = Pick<CustomerOrder, "id" | "orderNumber" | "status"> & {
  currency?: string;
  totalAmount?: number;
  shippingAmount?: number;
  items?: CustomerOrder["items"];
  shipments?: CustomerOrder["shipments"];
  customerEmail?: string | null;
  canRequestCancellation?: boolean;
  cancellationStatus?: string | null;
  refundStatus?: string | null;
  refundAmount?: number | null;
  guestToken?: string;
  guest?: boolean;
};

const UNPAID_STATUSES = ["CREATED", "PAYMENT_PENDING"];

function aftercareNote(order: OrderRow): string | null {
  const currency = order.currency ?? "INR";
  const amount = order.refundAmount != null ? `${currency} ${Number(order.refundAmount).toFixed(2)}` : "";
  switch (order.refundStatus) {
    case "PROCESSED":
      return `Refund of ${amount} issued. It usually reaches your account in 5-7 working days.`;
    case "REQUESTED":
    case "PENDING":
    case "PROCESSING":
    case "APPROVED":
      return `Refund of ${amount} is being processed.`;
    case "FAILED":
      return "Refund could not be completed. Our team will contact you.";
    default:
  }
  switch (order.cancellationStatus) {
    case "REQUESTED":
    case "PROCESSING":
      return "Cancellation requested. Waiting for approval.";
    case "REJECTED":
      return "Your cancellation request was declined. The order will continue to be processed.";
    default:
      return null;
  }
}

function statusTone(status: string): string {
  if (["CANCELLED", "FAILED", "REFUNDED"].includes(status)) return "isDanger";
  if (UNPAID_STATUSES.includes(status)) return "isWarn";
  return "isGood";
}

export default function OrdersPage() {
  const [orders, setOrders] = useState<OrderRow[]>([]);
  const [statusText, setStatusText] = useState("Loading your orders...");
  const [signedIn, setSignedIn] = useState(false);
  const [payingOrderId, setPayingOrderId] = useState<number | null>(null);
  const [paymentMessage, setPaymentMessage] = useState("");
  const [aftercareBusyOrderId, setAftercareBusyOrderId] = useState<number | null>(null);

  const loadOrders = useCallback(async () => {
    if (!readAccessToken()) {
      setSignedIn(false);
      const guestOrders = readGuestOrderIds().flatMap((id) => {
        const token = readGuestOrderAccess(id);
        return token ? [{ id, token }] : [];
      });
      const results = await Promise.allSettled(
        guestOrders.map(({ id, token }) => storefrontApi.getGuestOrderTracking(id, token))
      );
      setOrders(results.flatMap((result) => result.status === "fulfilled"
        ? [{
            id: result.value.orderId,
            orderNumber: result.value.orderNumber,
            status: result.value.status,
            currency: result.value.currency,
            totalAmount: result.value.totalAmount,
            shippingAmount: result.value.shippingAmount,
            items: result.value.items,
            shipments: result.value.shipments,
            canRequestCancellation: result.value.canRequestCancellation,
            cancellationStatus: result.value.cancellationStatus,
            refundStatus: result.value.refundStatus,
            refundAmount: result.value.refundAmount,
            guestToken: readGuestOrderAccess(result.value.orderId) ?? undefined,
            guest: true
          }]
        : []));
      setStatusText("");
      return;
    }

    setSignedIn(true);
    try {
      setOrders(await storefrontApi.listMyOrders());
      setStatusText("");
    } catch (error) {
      setStatusText(error instanceof Error ? error.message : "Could not load your orders.");
    }
  }, []);

  useEffect(() => {
    void loadOrders();
  }, [loadOrders]);

  async function payNow(order: OrderRow) {
    setPayingOrderId(order.id);
    setPaymentMessage("");
    try {
      await startOrderPayment(order, {
        onPaid: () => {
          setPaymentMessage(`Payment received for ${order.orderNumber}.`);
          void loadOrders();
        },
        onError: setPaymentMessage,
        onSettled: () => setPayingOrderId(null)
      });
    } catch (error) {
      setPaymentMessage(error instanceof Error ? error.message : "Could not start payment.");
      setPayingOrderId(null);
    }
  }

  async function requestCancellation(order: OrderRow) {
    if (!window.confirm(`Cancel ${order.orderNumber}? Once approved, the full amount you paid will be refunded.`)) {
      return;
    }
    setAftercareBusyOrderId(order.id);
    setPaymentMessage("");
    try {
      await storefrontApi.requestCancellation(order.id, null, order.guestToken);
      setPaymentMessage(`Cancellation request submitted for ${order.orderNumber}.`);
      await loadOrders();
    } catch (error) {
      setPaymentMessage(error instanceof Error ? error.message : "Could not submit the cancellation request.");
    } finally {
      setAftercareBusyOrderId(null);
    }
  }

  return (
    <main className="page">
      <section className="pageHeading">
        <h1>Orders</h1>
        {signedIn ? null : <p className="subtitle">Guest orders from this session. Sign in to keep your history.</p>}
      </section>

      {paymentMessage ? <p className="status" role="status">{paymentMessage}</p> : null}

      {!signedIn && orders.length === 0 && !statusText ? (
        <section className="emptyState">
          <h2>No orders in this browser</h2>
          <p>Sign in to see your order history, or place an order as a guest.</p>
          <Link href="/login" className="buttonLink">Sign in</Link>
        </section>
      ) : statusText ? (
        <p className="status" role="status">{statusText}</p>
      ) : orders.length ? (
        <section className="orderList">
          {orders.map((order) => (
            <article key={order.id} className="orderCard">
              <header className="orderCardHeader">
                <div>
                  <p className="orderLabel">Order</p>
                  <h2>{order.orderNumber}</h2>
                </div>
                <span className={`orderBadge ${statusTone(order.status)}`}>{order.status.replaceAll("_", " ")}</span>
                {order.totalAmount !== undefined ? (
                  <div className="orderTotal">
                    <p className="orderLabel">Total</p>
                    <strong>{order.currency} {Number(order.totalAmount).toFixed(2)}</strong>
                  </div>
                ) : null}
              </header>
              {aftercareNote(order) ? <p className="status" role="status">{aftercareNote(order)}</p> : null}
              <ShipmentProgress orderStatus={order.status} shipments={order.shipments} />
              <OrderItems items={order.items} shippingAmount={order.shippingAmount} currency={order.currency} />
              <footer className="orderCardActions">
                {order.guest && !UNPAID_STATUSES.includes(order.status) ? (
                  <Link className="buttonLink" href={`/track-order?orderId=${order.id}`}>Track order</Link>
                ) : null}
                {UNPAID_STATUSES.includes(order.status) ? (
                  <button type="button" onClick={() => void payNow(order)} disabled={payingOrderId !== null}>
                    {payingOrderId === order.id ? "Opening payment..." : "Pay now"}
                  </button>
                ) : null}
                {order.canRequestCancellation ? (
                  <button type="button" className="ghost" onClick={() => void requestCancellation(order)} disabled={aftercareBusyOrderId !== null}>
                    {aftercareBusyOrderId === order.id ? "Submitting..." : "Cancel order"}
                  </button>
                ) : null}
              </footer>
            </article>
          ))}
        </section>
      ) : (
        <section className="emptyState">
          <h2>No orders yet</h2>
          <p>Your completed and in-progress orders will appear here.</p>
          <Link href="/catalog" className="buttonLink">Browse products</Link>
        </section>
      )}
    </main>
  );
}
