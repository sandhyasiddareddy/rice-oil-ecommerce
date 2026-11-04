"use client";

import Link from "next/link";
import Image from "next/image";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useMemo, useState } from "react";
import { storefrontApi, type CatalogCategory, type CatalogProduct } from "../../lib/api";
import { ensureCart, readGuestCartAccessToken } from "../../lib/session";

export default function CatalogPage() {
  return (
    <Suspense fallback={null}>
      <CatalogContent />
    </Suspense>
  );
}

function CatalogContent() {
  const searchParams = useSearchParams();
  const queryParam = searchParams.get("q") ?? "";
  const categoryParam = searchParams.get("category") ?? "";
  const [categories, setCategories] = useState<CatalogCategory[]>([]);
  const [products, setProducts] = useState<CatalogProduct[]>([]);
  const [selectedCategory, setSelectedCategory] = useState("");
  const [sortBy, setSortBy] = useState("featured");
  const [busyAction, setBusyAction] = useState("");
  const [statusText, setStatusText] = useState("Loading catalog...");

  useEffect(() => {
    void loadInitial(queryParam, categoryParam);
  }, [queryParam, categoryParam]);

  async function loadInitial(query: string, requestedCategory: string) {
    try {
      const [categoryRows, productRows] = await Promise.all([
        storefrontApi.listCategories(),
        query ? storefrontApi.searchProducts(query) : storefrontApi.listProducts(requestedCategory || undefined)
      ]);
      setCategories(categoryRows);
      setSelectedCategory(query ? "" : requestedCategory);
      setProducts(productRows);
      setStatusText(
        productRows.length === 0
          ? query ? "No products match your search." : "No products match this selection."
          : `${productRows.length} products.`
      );
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
      <section className="controls surface">
        <div className="inlineActions">
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
                {product.imageUrl ? (
                  <Image
                    className="productCardImage"
                    src={product.imageUrl}
                    alt={product.name}
                    width={640}
                    height={480}
                  />
                ) : null}
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
