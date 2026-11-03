import Link from "next/link";

export default function AdminPage() {
  return (
    <main className="page">
      <section className="pageHeading">
        <p className="eyebrow">Admin</p>
        <h1>Staff access is not enabled</h1>
        <p className="subtitle">Admin tools require verified staff authentication. A browser setting cannot grant access.</p>
      </section>
      <section className="contentPanel">
        <p>For customer shopping, continue to the catalog.</p>
        <Link href="/catalog" className="buttonLink">Browse products</Link>
      </section>
    </main>
  );
}
