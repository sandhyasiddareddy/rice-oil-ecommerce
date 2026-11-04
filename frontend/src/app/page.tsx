"use client";

import Link from "next/link";
import Image from "next/image";
import { useEffect, useState } from "react";
import { storefrontApi, type CatalogProduct } from "../lib/api";
import { ensureCart, readGuestCartAccessToken } from "../lib/session";

export default function HomePage() {
  const [products, setProducts] = useState<CatalogProduct[]>([]);
  const [busyAction, setBusyAction] = useState("");
  const [statusText, setStatusText] = useState("Loading product catalog...");

  useEffect(() => {
    void loadCatalog();
  }, []);

  async function loadCatalog() {
    try {
      const productRows = await storefrontApi.listProducts();
      setProducts(productRows);
      setStatusText(productRows.length === 0 ? "The catalog is being prepared." : "Browse the current selection.");
    } catch (error) {
      setStatusText(normalizeError(error));
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
      <section className="productSection">
        <div className="sectionBar">
          <h2>Products</h2>
          {products.length > 0 ? <Link href="/catalog" className="inlineLink">View all products</Link> : null}
        </div>
        {featuredProducts.length > 0 ? (
          <div className="grid">
            {featuredProducts.map((product) => (
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
