"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { storefrontApi, type CatalogCategory } from "../lib/api";
import { clearCustomerSession, clearLegacySessionData, readAccessToken, readCustomerName } from "../lib/session";

export default function MainNav() {
  const pathname = usePathname();
  const router = useRouter();
  const [categories, setCategories] = useState<CatalogCategory[]>([]);
  const [menuOpen, setMenuOpen] = useState(false);
  const [shopOpen, setShopOpen] = useState(false);
  const [signedIn, setSignedIn] = useState(false);
  const [customerName, setCustomerName] = useState("");

  useEffect(() => {
    clearLegacySessionData();
    const token = readAccessToken();
    setSignedIn(Boolean(token));
    setCustomerName(token ? readCustomerName() ?? "" : "");
    let active = true;
    void storefrontApi.listCategories().then((rows) => {
      if (active) {
        setCategories(rows);
      }
    }).catch(() => {
      if (active) {
        setCategories([]);
      }
    });

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    setMenuOpen(false);
    setShopOpen(false);
    const token = readAccessToken();
    setSignedIn(Boolean(token));
    setCustomerName(token ? readCustomerName() ?? "" : "");
  }, [pathname]);

  const isShopActive = pathname.startsWith("/catalog") || pathname.startsWith("/products");

  function signOut() {
    clearCustomerSession();
    setSignedIn(false);
    setCustomerName("");
    setMenuOpen(false);
    router.push("/");
  }

  return (
    <div className="navGroup">
      <button
        type="button"
        className="navToggle"
        aria-expanded={menuOpen}
        aria-controls="primary-navigation"
        onClick={() => setMenuOpen((open) => !open)}
      >
        {menuOpen ? "Close" : "Menu"}
      </button>
      <nav id="primary-navigation" className={`mainNav${menuOpen ? " isOpen" : ""}`} aria-label="Primary navigation">
        <Link className={pathname === "/" ? "active" : ""} aria-current={pathname === "/" ? "page" : undefined} href="/">Home</Link>
        <div className={`navDropdown${shopOpen ? " isOpen" : ""}`}>
          <button
            type="button"
            className={`navShopToggle${isShopActive ? " active" : ""}`}
            aria-expanded={shopOpen}
            aria-controls="shop-category-menu"
            onClick={() => setShopOpen((open) => !open)}
          >
            Shop <span className="navChevron" aria-hidden="true" />
          </button>
          {shopOpen ? <div id="shop-category-menu" className="navMenu">
            <Link href="/catalog" onClick={() => setMenuOpen(false)}>All products</Link>
            {categories.map((category) => (
              <Link key={category.id} href={`/catalog?category=${encodeURIComponent(category.slug)}`} onClick={() => setMenuOpen(false)}>
                {category.name}
              </Link>
            ))}
            {categories.length === 0 ? <span className="navMenuEmpty">Categories coming soon</span> : null}
          </div> : null}
        </div>
        <Link className={pathname.startsWith("/cart") ? "active" : ""} aria-current={pathname.startsWith("/cart") ? "page" : undefined} href="/cart">Cart</Link>
        <Link className={pathname.startsWith("/orders") ? "active" : ""} aria-current={pathname.startsWith("/orders") ? "page" : undefined} href="/orders">Orders</Link>
        <Link className={pathname.startsWith("/login") ? "active" : ""} aria-current={pathname.startsWith("/login") ? "page" : undefined} href="/login">
          {signedIn && customerName ? `Hi, ${customerName.trim().split(/\s+/)[0]}` : "Account"}
        </Link>
        {signedIn ? <button type="button" className="navSignOut" onClick={signOut}>Sign out</button> : null}
      </nav>
    </div>
  );
}
