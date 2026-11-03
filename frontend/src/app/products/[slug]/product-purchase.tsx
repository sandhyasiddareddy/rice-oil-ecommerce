"use client";

import Link from "next/link";
import { useState } from "react";
import { storefrontApi, type CatalogProductDetail } from "../../../lib/api";
import { ensureCart, readGuestCartAccessToken } from "../../../lib/session";

export default function ProductPurchase({ product }: { product: CatalogProductDetail }) {
  const [selectedVariantId, setSelectedVariantId] = useState(product.variants[0]?.id ?? null);
  const [busy, setBusy] = useState(false);
  const [statusText, setStatusText] = useState("");
  const selectedVariant = product.variants.find((variant) => variant.id === selectedVariantId);

  async function addToCart() {
    if (!selectedVariant) {
      return;
    }

    setBusy(true);
    try {
      const cart = await ensureCart();
      await storefrontApi.addItem(cart.id, {
        sku: selectedVariant.sku,
        productName: product.name,
        unitPrice: selectedVariant.regularPrice,
        quantity: 1
      }, readGuestCartAccessToken());
      setStatusText(`${product.name} added to cart.`);
    } catch (error) {
      setStatusText(error instanceof Error ? error.message : "Could not add this item to your cart.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="productPurchase">
      {product.variants.length > 0 ? (
        <fieldset className="variantPicker">
          <legend>Choose a size</legend>
          {product.variants.map((variant) => (
            <label key={variant.id} className="variantOption">
              <input
                type="radio"
                name="product-variant"
                checked={selectedVariantId === variant.id}
                onChange={() => setSelectedVariantId(variant.id)}
              />
              <span>
                <strong>{variant.packSize}{variant.unit}</strong>
                <small>{variant.weightGrams} g</small>
              </span>
              <strong>{variant.currency} {Number(variant.regularPrice).toFixed(2)}</strong>
            </label>
          ))}
        </fieldset>
      ) : (
        <p className="status">This product is currently unavailable.</p>
      )}

      <div className="inlineActions">
        <button type="button" onClick={() => void addToCart()} disabled={!selectedVariant || busy}>
          {busy ? "Adding..." : "Add to cart"}
        </button>
        <Link href="/cart" className="inlineLink">View cart</Link>
      </div>
      {statusText ? <p className="privacyNote" role="status" aria-live="polite">{statusText}</p> : null}
    </section>
  );
}