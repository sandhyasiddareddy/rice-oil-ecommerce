"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { storefrontApi, type CustomerOrder } from "../../lib/api";
import { readAccessToken } from "../../lib/session";

export default function OrdersPage() {
  const [orders, setOrders] = useState<CustomerOrder[]>([]);
  const [statusText, setStatusText] = useState("Loading your orders...");
  const [signedIn, setSignedIn] = useState(false);

  useEffect(() => {
    if (!readAccessToken()) {
      setSignedIn(false);
      setStatusText("");
      return;
    }

    setSignedIn(true);
    void storefrontApi.listMyOrders()
      .then((result) => {
        setOrders(result);
        setStatusText("");
      })
      .catch((error: unknown) => {
        setStatusText(error instanceof Error ? error.message : "Could not load your orders.");
      });
  }, []);

  return (
    <main className="page">
      <section className="pageHeading">
        <p className="eyebrow">Orders</p>
        <h1>Order history</h1>
        <p className="subtitle">Your past orders are linked to your signed-in account.</p>
      </section>

      {!signedIn ? (
        <section className="emptyState">
          <h2>Sign in to view your orders</h2>
          <p>Order history is private to your customer account.</p>
          <Link href="/login" className="buttonLink">Sign in</Link>
        </section>
      ) : statusText ? (
        <p className="status" role="status">{statusText}</p>
      ) : orders.length ? (
        <section className="stackList">
          {orders.map((order) => (
            <article key={order.id} className="rowCard">
              <div>
                <h2>{order.orderNumber}</h2>
                <p>{order.status.replaceAll("_", " ")} · {order.currency} {Number(order.totalAmount).toFixed(2)}</p>
              </div>
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
