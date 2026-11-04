"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { storefrontApi } from "../../lib/api";
import { adminApi } from "../../lib/admin-api";
import { clearCustomerSession, readAccessToken, readCustomerName, saveCustomerSession } from "../../lib/session";
import { clearStaffSession, readStaffSession, saveStaffSession } from "../../lib/staff-session";

export default function LoginPage() {
  const router = useRouter();
  const [role, setRole] = useState<"customer" | "staff">("customer");
  const [staffUsername, setStaffUsername] = useState("");
  const [staffPassword, setStaffPassword] = useState("");
  const [registering, setRegistering] = useState(false);
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [statusText, setStatusText] = useState("");
  const [busy, setBusy] = useState(false);
  const [accountChecked, setAccountChecked] = useState(false);
  const [customerName, setCustomerName] = useState("");

  useEffect(() => {
    if (readStaffSession()) {
      router.replace("/admin");
      return;
    }
    if (new URLSearchParams(window.location.search).get("as") === "staff") {
      setRole("staff");
    }
    if (readAccessToken()) {
      setCustomerName(readCustomerName() ?? "");
    }
    setAccountChecked(true);
  }, [router]);

  async function submitStaff(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setStatusText("");
    const credentials = { username: staffUsername.trim(), password: staffPassword };
    try {
      await adminApi.verifyCredentials(credentials);
      clearCustomerSession();
      saveStaffSession(credentials);
      router.push("/admin");
    } catch (error) {
      setStaffPassword("");
      setStatusText(error instanceof Error ? error.message : "Staff sign in failed. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setStatusText("");
    try {
      const session = registering
        ? await storefrontApi.register(fullName, email.trim(), password)
        : await storefrontApi.login(email.trim(), password);
      clearStaffSession();
      saveCustomerSession(session.token, session.fullName);
      router.push("/");
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
          <Link href="/addresses" className="inlineLink">Manage addresses</Link>
        </section>
      </main>
    );
  }

  return (
    <main className="page">
      <section className="pageHeading">
        <p className="eyebrow">{role === "staff" ? "Farmora staff" : "Account"}</p>
        <h1>{role === "staff" ? "Staff sign in" : registering ? "Create your account" : "Welcome back"}</h1>
        <p className="subtitle">
          {role === "staff"
            ? "Use your staff credentials to open the operations workspace."
            : "Sign in to keep your cart and order history with your account."}
        </p>
      </section>

      <div className="adminTabs" role="tablist" aria-label="Sign in as">
        <button type="button" role="tab" aria-selected={role === "customer"} className={role === "customer" ? "active" : ""} onClick={() => { setRole("customer"); setStatusText(""); }}>Customer</button>
        <button type="button" role="tab" aria-selected={role === "staff"} className={role === "staff" ? "active" : ""} onClick={() => { setRole("staff"); setStatusText(""); }}>Staff / Admin</button>
      </div>

      {role === "staff" ? (
        <form className="contentPanel checkoutForm" onSubmit={(event) => void submitStaff(event)}>
          <label htmlFor="staff-username">Staff username</label>
          <input id="staff-username" autoComplete="username" required value={staffUsername} onChange={(event) => setStaffUsername(event.target.value)} />
          <label htmlFor="staff-password">Password</label>
          <input id="staff-password" type="password" autoComplete="current-password" required value={staffPassword} onChange={(event) => setStaffPassword(event.target.value)} />
          {statusText ? <p className="status" role="alert">{statusText}</p> : null}
          <button type="submit" disabled={busy}>{busy ? "Verifying..." : "Sign in to operations"}</button>
        </form>
      ) : (
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
        {!registering ? <Link href="/forgot-password" className="inlineLink">Forgot your password?</Link> : null}
      </form>
      )}

      {role === "customer" ? <p className="privacyNote">You can also <Link href="/catalog">continue shopping as a guest</Link>.</p> : null}
    </main>
  );
}
