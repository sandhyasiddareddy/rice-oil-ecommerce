"use client";

import { storefrontApi } from "./api";

type RazorpayCheckoutResult = {
  razorpay_payment_id: string;
  razorpay_order_id: string;
  razorpay_signature: string;
};

type RazorpayCheckoutOptions = {
  key: string;
  order_id: string;
  amount: number;
  currency: string;
  name: string;
  description: string;
  prefill?: { email: string };
  notes: { orderNumber: string };
  handler: (result: RazorpayCheckoutResult) => void;
  modal: { ondismiss: () => void };
  theme: { color: string };
};

declare global {
  interface Window {
    Razorpay?: new (options: RazorpayCheckoutOptions) => { open: () => void };
  }
}

export type PayableOrder = {
  id: number;
  orderNumber: string;
  customerEmail?: string | null;
};

function loadRazorpayCheckout(): Promise<void> {
  if (window.Razorpay) {
    return Promise.resolve();
  }
  return new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = "https://checkout.razorpay.com/v1/checkout.js";
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("Razorpay Checkout could not be loaded."));
    document.body.appendChild(script);
  });
}

/**
 * Opens Razorpay Checkout for an existing unpaid order. The backend returns the same
 * Razorpay order on repeated calls, so this is safe to use for retries and resumed payments.
 * Resolves once the checkout has been opened; callbacks report the outcome.
 */
export async function startOrderPayment(
  order: PayableOrder,
  callbacks: {
    onPaid: () => void;
    onError: (message: string) => void;
    onSettled: () => void;
  }
): Promise<void> {
  const paymentOrder = await storefrontApi.createPaymentOrder(order.id);
  if (!paymentOrder.keyId) {
    throw new Error("Online payment is not configured yet. Please contact support.");
  }
  await loadRazorpayCheckout();
  if (!window.Razorpay) {
    throw new Error("Razorpay Checkout could not be loaded. Please try again.");
  }

  const checkout = new window.Razorpay({
    key: paymentOrder.keyId,
    order_id: paymentOrder.providerOrderId,
    amount: paymentOrder.amount,
    currency: paymentOrder.currency,
    name: "Farmora",
    description: `Order ${order.orderNumber}`,
    prefill: order.customerEmail ? { email: order.customerEmail } : undefined,
    notes: { orderNumber: order.orderNumber },
    handler: (result) => {
      void verify(result);
    },
    modal: { ondismiss: callbacks.onSettled },
    theme: { color: "#464feb" }
  });

  async function verify(result: RazorpayCheckoutResult) {
    if (result.razorpay_order_id !== paymentOrder.providerOrderId) {
      callbacks.onError("Payment could not be verified for this order.");
      callbacks.onSettled();
      return;
    }
    try {
      const verified = await storefrontApi.verifyPayment(
        order.id,
        result.razorpay_payment_id,
        result.razorpay_signature
      );
      if (verified.status === "CANCELLED") {
        throw new Error("Your order expired before the payment completed, so it was cancelled. Your payment is being refunded automatically and should reach you within 5-7 business days.");
      }
      if (!verified.verified || verified.status !== "PAID") {
        throw new Error("Payment has not been confirmed. Check your payment status before retrying.");
      }
      callbacks.onPaid();
    } catch (error) {
      callbacks.onError(error instanceof Error ? error.message : "Payment could not be verified.");
    } finally {
      callbacks.onSettled();
    }
  }

  checkout.open();
}
