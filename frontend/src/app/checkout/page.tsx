"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import {
  storefrontApi,
  type Cart,
  type CheckoutAddress,
  type CustomerAddress,
  type Order
} from "../../lib/api";
import { startOrderPayment } from "../../lib/razorpay";
import {
  ensureCart,
  readAccessToken,
  readGuestCartAccessToken,
  releaseCheckedOutCart,
  rememberGuestOrder,
  saveGuestOrderAccess
} from "../../lib/session";

export default function CheckoutPage() {
  const [cart, setCart] = useState<Cart | null>(null);
  const [customerEmail, setCustomerEmail] = useState("");
  const [signedIn, setSignedIn] = useState(false);
  const [savedAddresses, setSavedAddresses] = useState<CustomerAddress[]>([]);
  const [selectedAddressId, setSelectedAddressId] = useState("new");
  const [shippingAddress, setShippingAddress] = useState<AddressFormData>(emptyAddress);
  const [billingAddress, setBillingAddress] = useState<AddressFormData>(emptyAddress);
  const [sameBillingAddress, setSameBillingAddress] = useState(true);
  const [order, setOrder] = useState<Order | null>(null);
  const [shippingFee, setShippingFee] = useState<number | null>(null);
  const [statusText, setStatusText] = useState("Loading your cart...");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const authenticated = Boolean(readAccessToken());
    setSignedIn(authenticated);
    void loadCart();
    if (authenticated) {
      void loadAddresses();
    }
  }, []);

  async function loadAddresses() {
    try {
      const addresses = await storefrontApi.listMyAddresses();
      setSavedAddresses(addresses);
      const defaultAddress = addresses.find((address) => address.defaultAddress) ?? addresses[0];
      if (defaultAddress) {
        setSelectedAddressId(String(defaultAddress.id));
        setShippingAddress(toAddressForm(defaultAddress));
      }
    } catch (error) {
      setStatusText(normalizeError(error));
    }
  }

  async function loadCart() {
    try {
      const currentCart = await ensureCart();
      setCart(currentCart);
      try {
        const quote = await storefrontApi.createShippingQuote(currentCart.id, readGuestCartAccessToken());
        setShippingFee(Number(quote.shippingFee));
      } catch {
        setShippingFee(null);
      }
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
      let shipping = toCheckoutAddress(shippingAddress);
      if (signedIn && selectedAddressId !== "new") {
        const selected = savedAddresses.find((address) => address.id === Number(selectedAddressId));
        if (!selected) {
          throw new Error("Select a valid shipping address or enter a new one.");
        }
        shipping = toCheckoutAddress(selected);
      } else if (signedIn) {
        const saved = await storefrontApi.createMyAddress({
          label: "Checkout",
          ...shipping,
          defaultAddress: savedAddresses.length === 0,
          shippingAddress: true,
          billingAddress: sameBillingAddress
        });
        setSavedAddresses((existing) => [...existing, saved]);
        setSelectedAddressId(String(saved.id));
      }

      const billing = sameBillingAddress ? shipping : toCheckoutAddress(billingAddress);
      const nextOrder = await storefrontApi.placeOrder(
        `idem-${crypto.randomUUID()}`,
        cart.id,
        customerEmail.trim().toLowerCase(),
        shipping,
        billing,
        readGuestCartAccessToken()
      );
      if (nextOrder.guestAccessToken) {
        saveGuestOrderAccess(nextOrder.id, nextOrder.guestAccessToken);
        rememberGuestOrder(nextOrder.id);
      }
      releaseCheckedOutCart();
      setCart(null);
      setOrder(nextOrder);
      setStatusText("");
    } catch (error) {
      setStatusText(normalizeError(error));
    } finally {
      setBusy(false);
    }
  }

  async function startPayment() {
    if (!order) {
      return;
    }

    setBusy(true);
    setStatusText("");
    try {
      await startOrderPayment(order, {
        onPaid: () => {
          setOrder((current) => (current ? { ...current, status: "PAID" } : current));
          setStatusText("");
        },
        onError: handlePaymentError,
        onSettled: () => setBusy(false)
      });
    } catch (error) {
      handlePaymentError(normalizeError(error));
      setBusy(false);
    }
  }

  function handlePaymentError(message: string) {
    if (/expired or been cancelled/i.test(message)) {
      setOrder((current) => (current ? { ...current, status: "CANCELLED" } : current));
      setStatusText("");
      return;
    }
    setStatusText(message);
  }

  if (order) {
    const paymentConfirmed = order.status === "PAID";
    const orderExpired = order.status === "CANCELLED";

    return (
      <main className="page">
        <section className="pageHeading">
          <p className="eyebrow">{paymentConfirmed ? "Order confirmed" : orderExpired ? "Order expired" : "Payment pending"}</p>
          <h1>{paymentConfirmed ? "Thank you" : orderExpired ? "This order has expired" : "Complete your payment"}</h1>
          <p className="subtitle">
            {paymentConfirmed
              ? `Your order reference is ${order.orderNumber}.`
              : orderExpired
                ? `Order ${order.orderNumber} was not paid in time and has been cancelled. Please add your items to the cart again to place a new order.`
                : `Order reference ${order.orderNumber}. Your order is not confirmed until payment succeeds.`}
          </p>
        </section>
        <section className="contentPanel">
          {paymentConfirmed ? <p>Payment confirmed. Your order is being processed.</p> : orderExpired ? (
            <p><Link href="/catalog">Back to catalog</Link></p>
          ) : (
            <div className="checkoutForm">
              <p>Complete payment securely through Razorpay to confirm your order.</p>
              <p className="privacyNote">Your items are held in this order. If you leave, you can resume payment any time from <Link href="/orders">Orders</Link>.</p>
              <button type="button" onClick={() => void startPayment()} disabled={busy}>
                {busy ? "Connecting to Razorpay..." : "Pay securely"}
              </button>
            </div>
          )}
          {paymentConfirmed && order.guestAccessToken ? (
            <p className="privacyNote">Guest tracking access is saved for this browser session and expires in 30 days.</p>
          ) : null}
          {statusText ? <p className="status" role="status">{statusText}</p> : null}
          <div className="inlineActions">
            {paymentConfirmed && order.guestAccessToken ? (
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
            <h2>Shipping address</h2>
            {signedIn && savedAddresses.length > 0 ? (
              <label htmlFor="saved-address">Choose a saved address</label>
            ) : null}
            {signedIn && savedAddresses.length > 0 ? (
              <select
                id="saved-address"
                value={selectedAddressId}
                onChange={(event) => {
                  setSelectedAddressId(event.target.value);
                  const address = savedAddresses.find((item) => String(item.id) === event.target.value);
                  if (address) {
                    setShippingAddress(toAddressForm(address));
                  }
                }}
              >
                {savedAddresses.map((address) => (
                  <option key={address.id} value={address.id}>
                    {address.label}: {address.line1}, {address.city}
                  </option>
                ))}
                <option value="new">Enter a new address</option>
              </select>
            ) : null}
            {(!signedIn || selectedAddressId === "new") ? (
              <AddressFormFields prefix="shipping" value={shippingAddress} onChange={setShippingAddress} />
            ) : null}
            <label className="inlineActions">
              <input
                type="checkbox"
                checked={sameBillingAddress}
                onChange={(event) => setSameBillingAddress(event.target.checked)}
              />
              Billing address is the same as shipping
            </label>
            {!sameBillingAddress ? (
              <>
                <h2>Billing address</h2>
                <AddressFormFields prefix="billing" value={billingAddress} onChange={setBillingAddress} />
              </>
            ) : null}
            <button type="submit" disabled={busy || (!signedIn && !customerEmail.trim())}>
              {busy ? "Submitting..." : "Continue to payment"}
            </button>
          </form>

          <aside className="contentPanel orderSummary">
            <h2>Order summary</h2>
            <p>{cart.items.length} {cart.items.length === 1 ? "item" : "items"}</p>
            <p>Subtotal <strong>INR {Number(cart.subtotalAmount).toFixed(2)}</strong></p>
            <p>Tax <strong>INR {Number(cart.taxAmount).toFixed(2)}</strong></p>
            <p>Discount <strong>INR {Number(cart.discountAmount).toFixed(2)}</strong></p>
            <p>Shipping <strong>{shippingFee === null ? "Calculated at order" : shippingFee === 0 ? "Free" : `INR ${shippingFee.toFixed(2)}`}</strong></p>
            <p className="orderTotal">Total <strong>INR {(Number(cart.totalAmount) + (shippingFee ?? 0)).toFixed(2)}</strong></p>
            <p className="privacyNote">You will confirm payment with Razorpay after placing your order.</p>
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

type AddressFormData = Omit<CheckoutAddress, "line2"> & { line2: string };

const emptyAddress: AddressFormData = {
  fullName: "",
  phone: "",
  line1: "",
  line2: "",
  city: "",
  state: "",
  postalCode: "",
  country: "IN"
};

function toAddressForm(address: CustomerAddress): AddressFormData {
  return { ...address, line2: address.line2 ?? "" };
}

function toCheckoutAddress(address: AddressFormData | CustomerAddress): CheckoutAddress {
  return { ...address, line2: address.line2 || null };
}

function AddressFormFields({
  prefix,
  value,
  onChange
}: {
  prefix: string;
  value: AddressFormData;
  onChange: (address: AddressFormData) => void;
}) {
  const fields: Array<[keyof AddressFormData, string, string]> = [
    ["fullName", "Full name", "name"],
    ["phone", "Phone", "tel"],
    ["line1", "Address line 1", "street-address"],
    ["line2", "Address line 2 (optional)", "address-line2"],
    ["city", "City", "address-level2"],
    ["state", "State", "address-level1"],
    ["postalCode", "Postal code", "postal-code"],
    ["country", "Country code", "country"]
  ];
  return (
    <>
      {fields.map(([field, label, autocomplete]) => {
        const id = `${prefix}-${field}`;
        return (
          <label key={field} htmlFor={id}>
            {label}
            <input
              id={id}
              autoComplete={autocomplete}
              required={field !== "line2"}
              value={value[field]}
              onChange={(event) => onChange({ ...value, [field]: event.target.value })}
            />
          </label>
        );
      })}
    </>
  );
}

function normalizeError(error: unknown): string {
  if (error instanceof Error) {
    return error.message;
  }
  return "We could not load checkout. Please try again.";
}