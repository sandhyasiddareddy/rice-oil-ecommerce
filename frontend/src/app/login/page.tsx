"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { storefrontApi } from "../../lib/api";
import { readAccessToken, readCustomerName, saveCustomerSession } from "../../lib/session";

export default function LoginPage() {
  const router = useRouter();
  const [registering, setRegistering] = useState(false);
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [statusText, setStatusText] = useState("");
  const [busy, setBusy] = useState(false);
  const [accountChecked, setAccountChecked] = useState(false);
  const [customerName, setCustomerName] = useState("");

  useEffect(() => {
    if (readAccessToken()) {
      setCustomerName(readCustomerName() ?? "");
    }
    setAccountChecked(true);
  }, []);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setStatusText("");
    try {
      const session = registering
        ? await storefrontApi.register(fullName, email.trim(), password)
        : await storefrontApi.login(email.trim(), password);
      saveCustomerSession(session.token, session.fullName);
      router.push("/cart");
    } catch (error) {
      setStatusText(error instanceof Error ? error.message : "Account access failed. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  if (!accountChecked) {
    return (
      <main className="page">
        <p className="status" role="status">Checking account...</p>
      </main>
    );
  }

  if (readAccessToken()) {
    const givenName = customerName.trim().split(/\s+/)[0];
    return (
      <main className="page">
        <section className="pageHeading">
          <p className="eyebrow">Account</p>
          <h1>{givenName ? `Welcome, ${givenName}` : "You're signed in"}</h1>
          <p className="subtitle">Your account is ready. Continue shopping or review your orders.</p>
        </section>
        <section className="contentPanel">
          <Link href="/cart" className="buttonLink">View cart</Link>
          <Link href="/orders" className="inlineLink">View order history</Link>
        </section>
      </main>
    );
  }

  return (
    <main className="page">
      <section className="pageHeading">
        <p className="eyebrow">Account</p>
        <h1>{registering ? "Create your account" : "Welcome back"}</h1>
        <p className="subtitle">Sign in to keep your cart and order history with your account.</p>
      </section>

      <form className="contentPanel checkoutForm" onSubmit={(event) => void submit(event)}>
        {registering ? (
          <>
            <label htmlFor="account-name">Full name</label>
            <input id="account-name" autoComplete="name" required value={fullName} onChange={(event) => setFullName(event.target.value)} />
          </>
        ) : null}
        <label htmlFor="account-email">Email</label>
        <input id="account-email" type="email" autoComplete="email" required value={email} onChange={(event) => setEmail(event.target.value)} />
        <label htmlFor="account-password">Password</label>
        <input id="account-password" type="password" autoComplete={registering ? "new-password" : "current-password"} minLength={8} required value={password} onChange={(event) => setPassword(event.target.value)} />
        {statusText ? <p className="status" role="alert">{statusText}</p> : null}
        <button type="submit" disabled={busy}>
          {busy ? "Please wait..." : registering ? "Create account" : "Sign in"}
        </button>
        <button type="button" className="textButton" onClick={() => { setRegistering((value) => !value); setStatusText(""); }}>
          {registering ? "Already have an account? Sign in" : "New to Farmora? Create an account"}
        </button>
      </form>

      <p className="privacyNote">You can also <Link href="/catalog">continue shopping as a guest</Link>.</p>
    </main>
  );
}
