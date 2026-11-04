import Link from "next/link";
import { storefrontApi, type CatalogProductDetail } from "../../../lib/api";
import ProductPurchase from "./product-purchase";

type ProductDetailPageProps = {
  params: Promise<{ slug: string }>;
};

export default async function ProductDetailPage({ params }: ProductDetailPageProps) {
  const { slug } = await params;

  let product: CatalogProductDetail | null = null;
  let statusText = "";

  try {
    product = await storefrontApi.getProduct(slug);
  } catch (error) {
    statusText = normalizeError(error);
  }

  return (
    <main className="page">
      <nav aria-label="Breadcrumb" className="breadcrumbs">
        <Link href="/">Home</Link>
        <span aria-hidden="true">/</span>
        <Link href="/catalog">Products</Link>
        <span aria-hidden="true">/</span>
        <span aria-current="page">{product?.name ?? "Product"}</span>
      </nav>

      <section className="pageHeading">
        <h1>{product?.name ?? "Product details"}</h1>
        <p className="subtitle">
          {product?.description ?? "This product could not be loaded."}
        </p>
      </section>

      {product ? (
        <ProductPurchase product={product} />
      ) : (
        <section className="emptyState">
          <h2>Product unavailable</h2>
          <p>{statusText}</p>
          <Link className="buttonLink" href="/catalog">Back to products</Link>
        </section>
      )}
    </main>
  );
}

function normalizeError(error: unknown): string {
  if (error instanceof Error) {
    return error.message;
  }
  return "Unexpected error";
}
