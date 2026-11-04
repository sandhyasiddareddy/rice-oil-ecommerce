"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { storefrontApi, type AddressInput, type CustomerAddress } from "../../lib/api";
import { readAccessToken } from "../../lib/session";

type AddressForm = AddressInput;

const emptyAddress: AddressForm = {
  label: "Home",
  fullName: "",
  phone: "",
  line1: "",
  line2: "",
  city: "",
  state: "",
  postalCode: "",
  country: "IN",
  defaultAddress: false,
  shippingAddress: true,
  billingAddress: false
};

export default function AddressesPage() {
  const [addresses, setAddresses] = useState<CustomerAddress[]>([]);
  const [form, setForm] = useState<AddressForm>(emptyAddress);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [signedIn, setSignedIn] = useState(false);
  const [busy, setBusy] = useState(false);
  const [statusText, setStatusText] = useState("");

  useEffect(() => {
    const authenticated = Boolean(readAccessToken());
    setSignedIn(authenticated);
    if (authenticated) {
      void loadAddresses();
    }
  }, []);

  async function loadAddresses() {
    try {
      setAddresses(await storefrontApi.listMyAddresses());
    } catch (error) {
      setStatusText(error instanceof Error ? error.message : "Could not load your addresses.");
    }
  }

  function editAddress(address: CustomerAddress) {
    setEditingId(address.id);
    setForm({
      label: address.label,
      fullName: address.fullName,
      phone: address.phone,
      line1: address.line1,
      line2: address.line2 ?? "",
      city: address.city,
      state: address.state,
      postalCode: address.postalCode,
      country: address.country,
      defaultAddress: address.defaultAddress,
      shippingAddress: address.shippingAddress,
      billingAddress: address.billingAddress
    });
  }

  async function saveAddress(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setStatusText("");
    try {
      if (editingId === null) {
        await storefrontApi.createMyAddress(form);
      } else {
        await storefrontApi.updateMyAddress(editingId, form);
      }
      setForm(emptyAddress);
      setEditingId(null);
      await loadAddresses();
      setStatusText("Address saved.");
      window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (error) {
      setStatusText(error instanceof Error ? error.message : "Could not save this address.");
    } finally {
      setBusy(false);
    }
  }

  async function deleteAddress(addressId: number) {
    setBusy(true);
    setStatusText("");
    try {
      await storefrontApi.deleteMyAddress(addressId);
      if (editingId === addressId) {
        setEditingId(null);
        setForm(emptyAddress);
      }
      await loadAddresses();
      setStatusText("Address removed.");
    } catch (error) {
      setStatusText(error instanceof Error ? error.message : "Could not remove this address.");
    } finally {
      setBusy(false);
    }
  }

  if (!signedIn) {
    return (
      <main className="page">
        <section className="emptyState">
          <h1>Sign in to manage addresses</h1>
          <Link href="/login" className="buttonLink">Sign in</Link>
        </section>
      </main>
    );
  }

  return (
    <main className="page">
      <section className="pageHeading">
        <p className="eyebrow">Account</p>
        <h1>Address book</h1>
        <p className="subtitle">Manage the addresses used for shipping and billing.</p>
      </section>

      {statusText ? (
        <p className={`status ${/saved|removed/i.test(statusText) ? "success" : "error"}`} role="status">{statusText}</p>
      ) : null}
      {addresses.length ? (
        <section className="stackList">
          {addresses.map((address) => (
            <article key={address.id} className="rowCard">
              <div className="addressCard">
                <h2>{address.label}{address.defaultAddress ? " · Default" : ""}</h2>
                <p>{address.fullName}</p>
                <p className="addressPhone">Phone: {address.phone || "Not provided"}</p>
                <p>{address.line1}{address.line2 ? `, ${address.line2}` : ""}, {address.city}, {address.state} {address.postalCode}, {address.country}</p>
              </div>
              <div className="inlineActions">
                <button type="button" className="ghost" onClick={() => editAddress(address)} disabled={busy}>Edit</button>
                <button type="button" className="textButton" onClick={() => void deleteAddress(address.id)} disabled={busy}>Remove</button>
              </div>
            </article>
          ))}
        </section>
      ) : (
        <section className="emptyState"><p>No saved addresses yet.</p></section>
      )}

      <form className="contentPanel checkoutForm" onSubmit={(event) => void saveAddress(event)}>
        <h2>{editingId === null ? "Add an address" : "Edit address"}</h2>
        <label>
          Label
          <input required value={form.label} onChange={(event) => setForm({ ...form, label: event.target.value })} />
        </label>
        <AddressField label="Full name" value={form.fullName} onChange={(value) => setForm({ ...form, fullName: value })} />
        <AddressField label="Phone" type="tel" value={form.phone} onChange={(value) => setForm({ ...form, phone: value })} />
        <AddressField label="Address line 1" value={form.line1} onChange={(value) => setForm({ ...form, line1: value })} />
        <AddressField label="Address line 2 (optional)" value={form.line2 ?? ""} onChange={(value) => setForm({ ...form, line2: value })} required={false} />
        <AddressField label="City" value={form.city} onChange={(value) => setForm({ ...form, city: value })} />
        <AddressField label="State" value={form.state} onChange={(value) => setForm({ ...form, state: value })} />
        <AddressField label="Postal code" value={form.postalCode} onChange={(value) => setForm({ ...form, postalCode: value })} />
        <AddressField label="Country code" value={form.country} onChange={(value) => setForm({ ...form, country: value })} />
        <label>
          <input type="checkbox" checked={Boolean(form.defaultAddress)} onChange={(event) => setForm({ ...form, defaultAddress: event.target.checked })} />
          Make this the default address
        </label>
        <label>
          <input type="checkbox" checked={Boolean(form.shippingAddress)} onChange={(event) => setForm({ ...form, shippingAddress: event.target.checked })} />
          Use for shipping
        </label>
        <label>
          <input type="checkbox" checked={Boolean(form.billingAddress)} onChange={(event) => setForm({ ...form, billingAddress: event.target.checked })} />
          Use for billing
        </label>
        <div className="inlineActions">
          <button type="submit" disabled={busy}>{busy ? "Saving..." : "Save address"}</button>
          {editingId !== null ? (
            <button type="button" className="ghost" onClick={() => { setEditingId(null); setForm(emptyAddress); }}>Cancel</button>
          ) : null}
        </div>
      </form>
    </main>
  );
}

function AddressField({
  label,
  value,
  onChange,
  required = true,
  type = "text"
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  required?: boolean;
  type?: string;
}) {
  return (
    <label>
      {label}
      <input type={type} required={required} value={value} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}
