"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { storefrontApi, type CatalogCategory, type CatalogProduct } from "../lib/api";
import { ensureCart, readGuestCartAccessToken } from "../lib/session";

export default function HomePage() {
  const [categories, setCategories] = useState<CatalogCategory[]>([]);
  const [products, setProducts] = useState<CatalogProduct[]>([]);
  const [searchText, setSearchText] = useState("");
  const [busyAction, setBusyAction] = useState("");
  const [statusText, setStatusText] = useState("Loading product catalog...");

  useEffect(() => {
    void loadCatalog();
  }, []);

  async function loadCatalog() {
    try {
      const [categoryRows, productRows] = await Promise.all([
        storefrontApi.listCategories(),
        storefrontApi.listProducts()
      ]);
      setCategories(categoryRows);
      setProducts(productRows);
      setStatusText(productRows.length === 0 ? "The catalog is being prepared." : "Browse the current selection.");
    } catch (error) {
      setStatusText(normalizeError(error));
    }
  }

  async function searchProducts() {
    setBusyAction("search");
    try {
      const rows = searchText.trim()
        ? await storefrontApi.searchProducts(searchText.trim())
        : await storefrontApi.listProducts();
      setProducts(rows);
      setStatusText(rows.length === 0 ? "No products match your search." : `${rows.length} products found.`);
    } catch (error) {
      setStatusText(normalizeError(error));
    } finally {
      setBusyAction("");
    }
  }

  async function addFirstVariantToCart(productSlug: string) {
    setBusyAction(`add:${productSlug}`);
    try {
      const cart = await ensureCart();
      const detail = await storefrontApi.getProduct(productSlug);
      const firstVariant = detail.variants[0];
      if (!firstVariant) {
        setStatusText("This product is currently unavailable.");
        return;
      }
      await storefrontApi.addItem(cart.id, {
        sku: firstVariant.sku,
        productName: detail.name,
        unitPrice: firstVariant.regularPrice,
        quantity: 1
      }, readGuestCartAccessToken());
      setStatusText(`${detail.name} added to cart.`);
    } catch (error) {
      setStatusText(normalizeError(error));
    } finally {
      setBusyAction("");
    }
  }

  const featuredProducts = products.slice(0, 8);

  return (
    <main className="page">
      <section className="hero surface">
        <div className="heroContent">
          <p className="eyebrow">Farmora Pantry</p>
          <h1>Good food, simply found.</h1>
          <p className="subtitle">Browse pantry staples, compare pack sizes, and review your order before checkout.</p>
          <div className="searchRow">
            <input
              aria-label="Search products"
              value={searchText}
              onChange={(event) => setSearchText(event.target.value)}
              placeholder="Search rice, oils, dals..."
            />
            <button onClick={searchProducts} disabled={busyAction === "search"}>Search</button>
          </div>
          <div className="inlineActions">
            <Link href="/catalog" className="buttonLink">Browse all products</Link>
          </div>
        </div>
      </section>

      {categories.length > 0 ? (
        <section className="categorySection">
          <div className="sectionBar">
            <h2>Shop by category</h2>
            <Link href="/catalog" className="inlineLink">View all</Link>
          </div>
          <div className="chipRow">
            {categories.map((category) => (
              <Link key={category.id} href={`/catalog?category=${encodeURIComponent(category.slug)}`} className="chip">
                {category.name}
              </Link>
            ))}
          </div>
        </section>
      ) : null}

      <section className="productSection">
        <div className="sectionBar">
          <h2>{searchText ? "Search results" : "Featured products"}</h2>
          {products.length > 0 ? <Link href="/catalog" className="inlineLink">View all products</Link> : null}
        </div>
        {featuredProducts.length > 0 ? (
          <div className="grid">
            {featuredProducts.map((product) => (
              <article key={product.id} className="card surface">
                <h3>{product.name}</h3>
                <p>{product.description || "A pantry staple, ready to explore."}</p>
                <div className="meta">
                  <span>{product.currency} {product.startingPrice.toFixed(2)}</span>
                  <span>{product.categorySlug}</span>
                </div>
                <div className="inlineActions">
                  <Link href={`/products/${product.slug}`} className="inlineLink">View details</Link>
                  <button
                    onClick={() => void addFirstVariantToCart(product.slug)}
                    disabled={busyAction === `add:${product.slug}`}
                  >
                    Add to cart
                  </button>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <div className="emptyState" role="status">
            <h3>No products available yet</h3>
            <p>{statusText || "Please check back soon."}</p>
          </div>
        )}
      </section>
    </main>
  );
}

function normalizeError(error: unknown): string {
  if (error instanceof Error) {
    return error.message;
  }
  return "Unexpected error";
}
