"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { storefrontApi } from "../../lib/api";

export default function ResetPasswordPage() {
  const [token, setToken] = useState("");
  const [password, setPassword] = useState("");
  const [statusText, setStatusText] = useState("");
  const [errorText, setErrorText] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    setToken(new URLSearchParams(window.location.search).get("token") ?? "");
  }, []);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setStatusText("");
    setErrorText("");
    try {
      const result = await storefrontApi.resetPassword(token, password);
      setStatusText(result.message);
    } catch (error) {
      setErrorText(error instanceof Error ? error.message : "We couldn't reset your password. Request a new link.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="page">
      <section className="pageHeading">
        <p className="eyebrow">Account recovery</p>
        <h1>Choose a new password</h1>
        <p className="subtitle">Reset links expire after 30 minutes and can only be used once.</p>
      </section>
      <form className="contentPanel checkoutForm" onSubmit={(event) => void submit(event)}>
        <label htmlFor="new-password">New password</label>
        <input id="new-password" type="password" autoComplete="new-password" minLength={8} maxLength={72}
          required value={password} onChange={(event) => setPassword(event.target.value)} />
        {!token ? <p className="status" role="alert">This reset link is missing or invalid.</p> : null}
        {statusText ? <p className="status" role="status">{statusText}</p> : null}
        {errorText ? <p className="status" role="alert">{errorText}</p> : null}
        <button type="submit" disabled={busy || !token || Boolean(statusText)}>
          {busy ? "Please wait..." : "Reset password"}
        </button>
        {statusText ? <Link href="/login" className="inlineLink">Continue to sign in</Link> : null}
      </form>
    </main>
  );
}
