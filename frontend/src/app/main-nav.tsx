"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { storefrontApi, type CatalogCategory } from "../lib/api";
import {
  clearCustomerSession,
  clearLegacySessionData,
  ensureCart,
  readAccessToken,
  readCartId,
  readCustomerName
} from "../lib/session";
import { clearStaffSession, readStaffSession, staffSessionEvent } from "../lib/staff-session";

export default function MainNav() {
  const pathname = usePathname();
  const router = useRouter();
  const [categories, setCategories] = useState<CatalogCategory[]>([]);
  const [menuOpen, setMenuOpen] = useState(false);
  const [searchText, setSearchText] = useState("");
  const [shopOpen, setShopOpen] = useState(false);
  const [accountOpen, setAccountOpen] = useState(false);
  const [signedIn, setSignedIn] = useState<boolean | null>(null);
  const [staffName, setStaffName] = useState<string | null>(null);
  const [customerName, setCustomerName] = useState("");
  const [cartItemCount, setCartItemCount] = useState<number | null>(null);

  useEffect(() => {
    clearLegacySessionData();
    const token = readAccessToken();
    setSignedIn(Boolean(token));
    setCustomerName(token ? readCustomerName() ?? "" : "");
    setStaffName(readStaffSession()?.username ?? null);
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
    setAccountOpen(false);
    const token = readAccessToken();
    setSignedIn(Boolean(token));
    setCustomerName(token ? readCustomerName() ?? "" : "");
    const staff = readStaffSession();
    setStaffName(staff?.username ?? null);
    if (staff) {
      if (!pathname.startsWith("/admin")) {
        router.replace("/admin");
      }
      return;
    }
    if (token || readCartId() !== null) {
      void ensureCart()
        .then((cart) => setCartItemCount(cart.items.reduce((total, item) => total + item.quantity, 0)))
        .catch((error: unknown) => console.error("Could not load cart item count.", error));
    } else {
      setCartItemCount(0);
    }
  }, [pathname, router]);

  useEffect(() => {
    function handleCartUpdated(event: Event) {
      const { count } = (event as CustomEvent<{ count: number }>).detail;
      if (Number.isSafeInteger(count) && count >= 0) {
        setCartItemCount(count);
      }
    }

    function handleCustomerSessionUpdated() {
      const token = readAccessToken();
      setSignedIn(Boolean(token));
      setCustomerName(token ? readCustomerName() ?? "" : "");
    }

    function handleStaffSessionUpdated() {
      setStaffName(readStaffSession()?.username ?? null);
    }

    window.addEventListener("farmora:cart-updated", handleCartUpdated);
    window.addEventListener("farmora:customer-session-updated", handleCustomerSessionUpdated);
    window.addEventListener(staffSessionEvent, handleStaffSessionUpdated);
    return () => {
      window.removeEventListener("farmora:cart-updated", handleCartUpdated);
      window.removeEventListener("farmora:customer-session-updated", handleCustomerSessionUpdated);
      window.removeEventListener(staffSessionEvent, handleStaffSessionUpdated);
    };
  }, []);

  const isShopActive = pathname.startsWith("/catalog") || pathname.startsWith("/products");

  function signOut() {
    // Fire-and-forget: the token is read synchronously before the session is cleared.
    void storefrontApi.logout().catch(() => undefined);
    clearCustomerSession();
    setSignedIn(false);
    setCustomerName("");
    setMenuOpen(false);
    setAccountOpen(false);
    router.push("/");
  }

  function staffSignOut() {
    clearStaffSession();
    setStaffName(null);
    setMenuOpen(false);
    router.push("/login?as=staff");
  }

  if (staffName !== null) {
    return (
      <div className="navGroup">
        <nav className="mainNav" aria-label="Staff navigation">
          <Link className={pathname.startsWith("/admin") ? "active" : ""} href="/admin">Operations</Link>
          <span className="navMenuEmpty">Signed in as {staffName}</span>
          <button type="button" className="navSignOut" onClick={staffSignOut}>Sign out</button>
        </nav>
      </div>
    );
  }

  function submitSearch(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const query = searchText.trim();
    router.push(query ? `/catalog?q=${encodeURIComponent(query)}` : "/catalog");
  }

  return (
    <div className="navGroup">
      <form className="navSearch" role="search" onSubmit={submitSearch}>
        <input
          type="search"
          aria-label="Search products"
          value={searchText}
          onChange={(event) => setSearchText(event.target.value)}
          placeholder="Search products"
        />
        <button type="submit" aria-label="Search">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.5-3.5" />
          </svg>
        </button>
      </form>
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
            Products <span className="navChevron" aria-hidden="true" />
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
        <Link
          className={`navIconLink${pathname.startsWith("/cart") ? " active" : ""}`}
          aria-current={pathname.startsWith("/cart") ? "page" : undefined}
          aria-label={`Cart${cartItemCount === null ? "" : `, ${cartItemCount} items`}`}
          href="/cart"
        >
          <NavIcon name="cart" />
          <span>Cart</span>
          {cartItemCount !== null && cartItemCount > 0 ? (
            <span className="navCountBadge" aria-hidden="true">{cartItemCount > 99 ? "99+" : cartItemCount}</span>
          ) : null}
        </Link>
        <Link
          className={`navIconLink${pathname.startsWith("/orders") ? " active" : ""}`}
          aria-current={pathname.startsWith("/orders") ? "page" : undefined}
          href="/orders"
        >
          <NavIcon name="orders" />
          <span>Orders</span>
        </Link>
        {signedIn ? (
          <div className={`navDropdown accountDropdown${accountOpen ? " isOpen" : ""}`}>
            <button
              type="button"
              className="navAccountToggle"
              aria-expanded={accountOpen}
              aria-controls="account-menu"
              onClick={() => setAccountOpen((open) => !open)}
            >
              <NavIcon name="account" />
              {customerName ? customerName.trim().split(/\s+/)[0] : "Account"}
              <span className="navChevron" aria-hidden="true" />
            </button>
            {accountOpen ? (
              <div id="account-menu" className="navMenu">
                <Link href="/login">My account</Link>
                <Link href="/addresses">Address book</Link>
                <button type="button" className="navSignOut" onClick={signOut}>Sign out</button>
              </div>
            ) : null}
          </div>
        ) : (
          <Link
            className={`navIconLink${pathname.startsWith("/login") ? " active" : ""}`}
            aria-current={pathname.startsWith("/login") ? "page" : undefined}
            href="/login"
          >
            <NavIcon name="account" />
            <span>Sign in</span>
          </Link>
        )}
      </nav>
    </div>
  );
}

function NavIcon({ name }: { name: "account" | "cart" | "orders" }) {
  if (name === "account") {
    return (
      <svg className="navIcon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
        <circle cx="12" cy="8" r="3.5" />
        <path d="M5 20c.7-3.5 3.2-5.3 7-5.3s6.3 1.8 7 5.3" strokeLinecap="round" />
      </svg>
    );
  }
  if (name === "cart") {
    return (
      <svg className="navIcon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
        <path d="M3 4h2l2.2 11.1a2 2 0 0 0 2 1.6h8.5a2 2 0 0 0 1.9-1.4L22 8H6" strokeLinecap="round" strokeLinejoin="round" />
        <circle cx="10" cy="20" r="1" />
        <circle cx="18" cy="20" r="1" />
      </svg>
    );
  }
  return (
    <svg className="navIcon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
      <path d="M4 7.5 12 3l8 4.5v9L12 21l-8-4.5z" strokeLinejoin="round" />
      <path d="m4.5 7.7 7.5 4.2 7.5-4.2M12 12v8.5M8 5.2l8 4.5" strokeLinejoin="round" />
    </svg>
  );
}
