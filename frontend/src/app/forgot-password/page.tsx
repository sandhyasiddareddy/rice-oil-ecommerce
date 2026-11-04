"use client";

import Link from "next/link";
import { useState } from "react";
import { storefrontApi } from "../../lib/api";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [statusText, setStatusText] = useState("");
  const [errorText, setErrorText] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setStatusText("");
    setErrorText("");
    try {
      const result = await storefrontApi.requestPasswordReset(email.trim());
      setStatusText(result.message);
    } catch (error) {
      setErrorText(error instanceof Error ? error.message : "We couldn't send that request. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="page">
      <section className="pageHeading">
        <p className="eyebrow">Account recovery</p>
        <h1>Forgot your password?</h1>
        <p className="subtitle">Enter the email address on your account and we will send a reset link if it exists.</p>
      </section>
      <form className="contentPanel checkoutForm" onSubmit={(event) => void submit(event)}>
        <label htmlFor="reset-email">Email</label>
        <input id="reset-email" type="email" autoComplete="email" required value={email}
          onChange={(event) => setEmail(event.target.value)} />
        {statusText ? <p className="status" role="status">{statusText}</p> : null}
        {errorText ? <p className="status" role="alert">{errorText}</p> : null}
        <button type="submit" disabled={busy}>{busy ? "Please wait..." : "Send reset link"}</button>
        <Link href="/login" className="inlineLink">Back to sign in</Link>
      </form>
    </main>
  );
}
