"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { storefrontApi, type CatalogCategory, type CatalogProduct } from "../../lib/api";
import { ensureCart, readGuestCartAccessToken } from "../../lib/session";

export default function CatalogPage() {
  const [categories, setCategories] = useState<CatalogCategory[]>([]);
  const [products, setProducts] = useState<CatalogProduct[]>([]);
  const [selectedCategory, setSelectedCategory] = useState("");
  const [searchText, setSearchText] = useState("");
  const [sortBy, setSortBy] = useState("featured");
  const [busyAction, setBusyAction] = useState("");
  const [statusText, setStatusText] = useState("Loading catalog...");

  useEffect(() => {
    void loadInitial();
  }, []);

  async function loadInitial() {
    try {
      const requestedCategory = new URLSearchParams(window.location.search).get("category") ?? "";
      const [categoryRows, productRows] = await Promise.all([
        storefrontApi.listCategories(),
        storefrontApi.listProducts(requestedCategory || undefined)
      ]);
      setCategories(categoryRows);
      setSelectedCategory(requestedCategory);
      setProducts(productRows);
      setStatusText(productRows.length === 0 ? "No products match this selection." : `${productRows.length} products.`);
    } catch (error) {
      setStatusText(normalizeError(error));
    }
  }

  async function filterProducts(categorySlug: string) {
    setBusyAction("filter");
    try {
      const rows = await storefrontApi.listProducts(categorySlug || undefined);
      setSelectedCategory(categorySlug);
      setProducts(rows);
      setStatusText(rows.length === 0 ? "No products match this selection." : `${rows.length} products.`);
    } catch (error) {
      setStatusText(normalizeError(error));
    } finally {
      setBusyAction("");
    }
  }

  async function searchProducts() {
    setBusyAction("search");
    try {
      const rows = searchText.trim() ? await storefrontApi.searchProducts(searchText.trim()) : await storefrontApi.listProducts();
      setProducts(rows);
      setStatusText(rows.length === 0 ? "No products match your search." : `${rows.length} products.`);
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
        setStatusText("No active variant available for this product.");
        return;
      }
      await storefrontApi.addItem(cart.id, {
        sku: firstVariant.sku,
        productName: detail.name,
        unitPrice: firstVariant.regularPrice,
        quantity: 1
      }, readGuestCartAccessToken());
      setStatusText(`Added ${detail.name} to cart.`);
    } catch (error) {
      setStatusText(normalizeError(error));
    } finally {
      setBusyAction("");
    }
  }

  const visibleProducts = useMemo(() => {
    const rows = [...products];
    if (sortBy === "priceAsc") {
      rows.sort((a, b) => a.startingPrice - b.startingPrice);
    } else if (sortBy === "priceDesc") {
      rows.sort((a, b) => b.startingPrice - a.startingPrice);
    } else {
      rows.sort((a, b) => a.name.localeCompare(b.name));
    }
    return rows;
  }, [products, sortBy]);

  return (
    <main className="page">
      <section className="surface sectionHeader">
        <h1>Product Catalog</h1>
        <p className="subtitle">Find a product, compare pack sizes, and add it to your cart.</p>
      </section>

      <section className="controls surface">
        <label>
          Search
          <input aria-label="Search products" value={searchText} onChange={(event) => setSearchText(event.target.value)} placeholder="Try rice, oil, sesame..." />
        </label>
        <div className="inlineActions">
          <button onClick={searchProducts} disabled={busyAction === "search"}>Search</button>
          <button className="ghost" onClick={() => void filterProducts("")} disabled={busyAction === "filter"}>Reset</button>
          <select aria-label="Sort products" value={sortBy} onChange={(event) => setSortBy(event.target.value)}>
            <option value="featured">Name: A-Z</option>
            <option value="priceAsc">Price: Low to High</option>
            <option value="priceDesc">Price: High to Low</option>
          </select>
        </div>
      </section>

      {categories.length > 0 ? (
        <section className="categorySection">
          <h2>Filter by category</h2>
          <div className="chipRow">
            <button type="button" className={selectedCategory === "" ? "chip active" : "chip"} onClick={() => void filterProducts("")}>All</button>
            {categories.map((category) => (
              <button
                type="button"
                key={category.id}
                className={selectedCategory === category.slug ? "chip active" : "chip"}
                onClick={() => void filterProducts(category.slug)}
              >
                {category.name}
              </button>
            ))}
          </div>
        </section>
      ) : null}

      <section aria-live="polite" className="productSection">
        <p className="resultCount">{statusText}</p>
        {visibleProducts.length > 0 ? (
          <div className="grid">
            {visibleProducts.map((product) => (
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
          <div className="emptyState">
            <h2>No products found</h2>
            <p>{statusText || "Try another search or clear the category filter."}</p>
            <button type="button" className="ghost" onClick={() => void filterProducts("")}>Clear filters</button>
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
