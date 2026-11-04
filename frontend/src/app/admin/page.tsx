"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { storefrontApi, type CatalogCategory, type CatalogProduct } from "../../lib/api";
import { clearStaffSession, readStaffSession } from "../../lib/staff-session";
import {
  adminApi,
  StaffAuthError,
  type AdminAftercareRequest,
  type AdminCategory,
  type AdminCredentials,
  type AdminProduct,
  type AdminVariant,
  type FulfilmentOrder,
  type ImportPreview,
  type ImportResult
} from "../../lib/admin-api";

type AdminTab = "catalog" | "inventory" | "promotions" | "imports" | "aftercare" | "fulfilment";
type Notice = { kind: "success" | "error"; text: string };

const nextStage: Record<string, { status: string; label: string }> = {
  READY_TO_PACK: { status: "PACKED", label: "Mark packed" },
  SHIPMENT_CREATED: { status: "PACKED", label: "Mark packed" },
  PACKED: { status: "SHIPPED", label: "Mark shipped" },
  SHIPPED: { status: "IN_TRANSIT", label: "Mark in transit" },
  PICKED_UP: { status: "IN_TRANSIT", label: "Mark in transit" },
  IN_TRANSIT: { status: "OUT_FOR_DELIVERY", label: "Mark out for delivery" },
  OUT_FOR_DELIVERY: { status: "DELIVERED", label: "Mark delivered" }
};

const tabs: { id: AdminTab; label: string }[] = [
  { id: "fulfilment", label: "Fulfilment" },
  { id: "catalog", label: "Catalog" },
  { id: "inventory", label: "Inventory" },
  { id: "promotions", label: "Promotions" },
  { id: "imports", label: "Imports" },
  { id: "aftercare", label: "Orders & refunds" }
];

export default function AdminPage() {
  const router = useRouter();
  const [loadError, setLoadError] = useState("");
  const [credentials, setCredentials] = useState<AdminCredentials>({ username: "", password: "" });
  const [authorized, setAuthorized] = useState(false);
  const [activeTab, setActiveTab] = useState<AdminTab>("fulfilment");
  const [fulfilmentOrders, setFulfilmentOrders] = useState<FulfilmentOrder[]>([]);
  const [fulfilmentFilter, setFulfilmentFilter] = useState("OPEN");
  const [trackingDrafts, setTrackingDrafts] = useState<Record<string, { provider: string; trackingNumber: string }>>({});
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<Notice | null>(null);
  const [categories, setCategories] = useState<CatalogCategory[]>([]);
  const [products, setProducts] = useState<CatalogProduct[]>([]);
  const [workingProductId, setWorkingProductId] = useState("");
  const [recentCategory, setRecentCategory] = useState<AdminCategory | null>(null);
  const [recentProduct, setRecentProduct] = useState<AdminProduct | null>(null);
  const [recentVariant, setRecentVariant] = useState<AdminVariant | null>(null);
  const [uploadedImageUrl, setUploadedImageUrl] = useState<string | null>(null);
  const [inventoryResult, setInventoryResult] = useState<{ variantId: number; availableQty: number; reservedQty: number } | null>(null);
  const [promotionResult, setPromotionResult] = useState<{ id: number; code: string; active: boolean } | null>(null);
  const [importFile, setImportFile] = useState<File | null>(null);
  const [importPreview, setImportPreview] = useState<ImportPreview | null>(null);
  const [importResult, setImportResult] = useState<ImportResult | null>(null);
  const [refundRequests, setRefundRequests] = useState<AdminAftercareRequest[]>([]);
  const [cancellationRequests, setCancellationRequests] = useState<AdminAftercareRequest[]>([]);

  async function loadAftercare() {
    const [refundRows, cancellationRows] = await Promise.all([
      adminApi.listAftercareRefunds(credentials),
      adminApi.listAftercareCancellations(credentials)
    ]);
    setRefundRequests(refundRows);
    setCancellationRequests(cancellationRows);
  }

  async function loadFulfilment() {
    setFulfilmentOrders(await adminApi.listFulfilmentOrders(credentials));
  }

  async function advanceOrder(order: FulfilmentOrder) {
    const next = nextStage[order.fulfilmentStatus];
    if (!next) {
      return;
    }
    const draft = trackingDrafts[order.orderNumber] ?? { provider: order.provider ?? "", trackingNumber: order.trackingNumber ?? "" };
    if (next.status === "SHIPPED" && !draft.trackingNumber.trim()) {
      setNotice({ kind: "error", text: "Enter the tracking number before marking the order shipped." });
      return;
    }
    await runAction(async () => {
      await adminApi.updateShipment(credentials, {
        orderNumber: order.orderNumber,
        status: next.status,
        provider: draft.provider.trim() || undefined,
        trackingNumber: draft.trackingNumber.trim() || undefined
      });
      await loadFulfilment();
      return `${order.orderNumber} is now ${next.status.replace(/_/g, " ").toLowerCase()}.`;
    });
  }

  async function loadWorkspace(creds: AdminCredentials) {
    await adminApi.verifyCredentials(creds);
    const [categoryRows, productRows, refundRows, cancellationRows, fulfilmentRows] = await Promise.all([
      storefrontApi.listCategories(),
      storefrontApi.listProducts(),
      adminApi.listAftercareRefunds(creds),
      adminApi.listAftercareCancellations(creds),
      adminApi.listFulfilmentOrders(creds)
    ]);
    setCredentials(creds);
    setCategories(categoryRows);
    setProducts(productRows);
    setRefundRequests(refundRows);
    setCancellationRequests(cancellationRows);
    setFulfilmentOrders(fulfilmentRows);
    setAuthorized(true);
  }

  useEffect(() => {
    const saved = readStaffSession();
    if (!saved) {
      router.replace("/login?as=staff");
      return;
    }
    loadWorkspace(saved).catch((error) => {
      if (error instanceof StaffAuthError) {
        clearStaffSession();
        router.replace("/login?as=staff");
        return;
      }
      setLoadError(messageFor(error));
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function signOut() {
    clearStaffSession();
    router.push("/login?as=staff");
  }

  async function createCategory(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    await runAction(async () => {
      const category = await adminApi.createCategory(credentials, {
        name: String(form.get("name")),
        slug: String(form.get("slug")),
        description: String(form.get("description") ?? ""),
        active: form.get("active") === "on"
      });
      setRecentCategory(category);
      setCategories(await storefrontApi.listCategories());
      return `Category “${category.name}” created.`;
    });
  }

  async function createProduct(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    await runAction(async () => {
      const product = await adminApi.createProduct(credentials, {
        categoryId: Number(form.get("categoryId")),
        name: String(form.get("name")),
        slug: String(form.get("slug")),
        description: String(form.get("description") ?? ""),
        attributesJson: "{}",
        published: form.get("published") === "on"
      });
      setRecentProduct(product);
      setWorkingProductId(String(product.id));
      setProducts(await storefrontApi.listProducts());
      return `Product “${product.name}” created.`;
    });
  }

  async function createVariant(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    await runAction(async () => {
      const variant = await adminApi.createVariant(credentials, {
        productId: Number(form.get("productId")),
        sku: String(form.get("sku")),
        packSize: Number(form.get("packSize")),
        unit: String(form.get("unit")) as "KG" | "G" | "L" | "ML",
        weightGrams: Number(form.get("weightGrams")),
        regularPrice: Number(form.get("regularPrice")),
        active: form.get("active") === "on"
      });
      setRecentVariant(variant);
      return `Variant ${variant.sku} created.`;
    });
  }

  async function uploadImageFor(productId: number, file: File, altText: string, onDone?: () => void) {
    await runAction(async () => {
      const result = await adminApi.uploadProductImage(credentials, productId, file, altText);
      setUploadedImageUrl(result.imageUrl);
      setProducts(await storefrontApi.listProducts());
      onDone?.();
      return "Product image uploaded.";
    });
  }

  async function uploadProductImage(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    const file = form.get("image");
    if (!(file instanceof File) || file.size === 0) {
      setNotice({ kind: "error", text: "Choose an image file to upload." });
      return;
    }
    await uploadImageFor(Number(form.get("productId")), file, String(form.get("altText") ?? ""), () => {
      formElement.reset();
      setWorkingProductId(String(form.get("productId")));
    });
  }

  async function adjustInventory(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    await runAction(async () => {
      const result = await adminApi.adjustInventory(credentials, {
        variantId: Number(form.get("variantId")),
        deltaQty: Number(form.get("deltaQty")),
        reason: String(form.get("reason")),
        reference: String(form.get("reference") ?? ""),
        createdBy: credentials.username
      });
      setInventoryResult(result);
      return `Inventory updated for variant ${result.variantId}.`;
    });
  }

  async function createPromotion(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    await runAction(async () => {
      const toIso = (value: FormDataEntryValue | null) => value ? new Date(String(value)).toISOString() : null;
      const promotion = await adminApi.createPromotion(credentials, {
        code: String(form.get("code")).trim().toUpperCase(),
        discountType: String(form.get("discountType")) as "PERCENTAGE" | "FIXED_AMOUNT",
        discountValue: Number(form.get("discountValue")),
        minOrderValue: form.has("minOrderValue") && String(form.get("minOrderValue")) ? Number(form.get("minOrderValue")) : null,
        stackable: form.get("stackable") === "on",
        exclusionGroup: String(form.get("exclusionGroup") ?? "") || null,
        maxTotalRedemptions: form.has("maxTotalRedemptions") && String(form.get("maxTotalRedemptions")) ? Number(form.get("maxTotalRedemptions")) : null,
        maxRedemptionsPerCustomer: form.has("maxRedemptionsPerCustomer") && String(form.get("maxRedemptionsPerCustomer")) ? Number(form.get("maxRedemptionsPerCustomer")) : null,
        active: form.get("active") === "on",
        startsAt: toIso(form.get("startsAt")),
        endsAt: toIso(form.get("endsAt"))
      });
      setPromotionResult(promotion);
      return `Promotion ${promotion.code} created.`;
    });
  }

  async function previewImport(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!importFile) {
      setNotice({ kind: "error", text: "Choose an Excel workbook first." });
      return;
    }
    await runAction(async () => {
      const preview = await adminApi.previewImport(credentials, importFile);
      setImportPreview(preview);
      setImportResult(null);
      return `Import ${preview.jobId} validated.`;
    });
  }

  async function confirmImport() {
    if (!importPreview) {
      return;
    }
    await runAction(async () => {
      const result = await adminApi.confirmImport(credentials, importPreview.jobId);
      setImportResult(result);
      return `Import ${result.jobId} applied.`;
    });
  }

  async function downloadTemplate() {
    await runAction(async () => {
      downloadBlob(await adminApi.downloadTemplate(credentials), "catalog-template.xlsx");
      return "Template downloaded.";
    });
  }

  async function downloadImportErrors() {
    if (!importPreview) {
      return;
    }
    await runAction(async () => {
      const file = await adminApi.downloadImportErrors(credentials, importPreview.jobId);
      downloadBlob(file, `import-errors-${importPreview.jobId}.csv`);
      return "Error report downloaded.";
    });
  }

  async function reviewAftercare(kind: "refund" | "cancellation", requestId: number, approve: boolean) {
    await runAction(async () => {
      if (kind === "refund") {
        if (approve) {
          await adminApi.approveAftercareRefund(credentials, requestId);
        } else {
          await adminApi.rejectAftercareRefund(credentials, requestId);
        }
      } else if (approve) {
        await adminApi.approveAftercareCancellation(credentials, requestId);
      } else {
        await adminApi.rejectAftercareCancellation(credentials, requestId);
      }
      await loadAftercare();
      return approve
        ? kind === "refund" ? `Refund request ${requestId} sent to Razorpay.` : `Cancellation request ${requestId} approved.`
        : `${kind === "refund" ? "Refund" : "Cancellation"} request ${requestId} rejected.`;
    });
  }

  async function runAction(action: () => Promise<string>) {
    setBusy(true);
    setNotice(null);
    try {
      setNotice({ kind: "success", text: await action() });
    } catch (error) {
      setNotice({ kind: "error", text: messageFor(error) });
    } finally {
      setBusy(false);
    }
  }

  if (!authorized) {
    return (
      <main className="page adminPage">
        {loadError ? (
          <>
            <p className="status error" role="alert">{loadError}</p>
            <button type="button" onClick={() => window.location.reload()}>Try again</button>
          </>
        ) : (
          <p className="status" role="status">Loading staff workspace...</p>
        )}
      </main>
    );
  }

  return (
    <main className="page adminPage">
      <section className="pageHeading adminHeading">
        <div>
          <p className="eyebrow">Farmora operations</p>
          <h1>Staff workspace</h1>
        </div>
        <button type="button" className="ghost" onClick={signOut}>Sign out</button>
      </section>

      <nav className="adminTabs" aria-label="Admin tools" role="tablist">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            type="button"
            role="tab"
            aria-selected={activeTab === tab.id}
            className={activeTab === tab.id ? "active" : ""}
            onClick={() => {
              setActiveTab(tab.id);
              setNotice(null);
              if (tab.id === "fulfilment") {
                void loadFulfilment().catch((error) => setNotice({ kind: "error", text: messageFor(error) }));
              }
              if (tab.id === "aftercare") {
                void loadAftercare().catch((error) => setNotice({ kind: "error", text: messageFor(error) }));
              }
            }}
          >
            {tab.label}
          </button>
        ))}
      </nav>

      {notice ? <p className={`status ${notice.kind}`} role={notice.kind === "error" ? "alert" : "status"}>{notice.text}</p> : null}

      {activeTab === "fulfilment" ? (
        <section className="adminGrid" aria-label="Order fulfilment">
          <div className="contentPanel adminForm">
            <h2>Paid orders</h2>
            <label htmlFor="fulfilment-filter">Show</label>
            <select id="fulfilment-filter" value={fulfilmentFilter} onChange={(event) => setFulfilmentFilter(event.target.value)}>
              <option value="OPEN">Not yet delivered</option>
              <option value="READY_TO_PACK">Ready to pack</option>
              <option value="PACKED">Packed</option>
              <option value="SHIPPED">Shipped / in transit</option>
              <option value="DELIVERED">Delivered</option>
              <option value="ALL">All</option>
            </select>
            <button type="button" className="ghost" disabled={busy} onClick={() => void runAction(async () => { await loadFulfilment(); return "Orders refreshed."; })}>Refresh</button>
            {fulfilmentOrders.filter((order) => matchesFilter(order.fulfilmentStatus, fulfilmentFilter)).map((order) => {
              const next = nextStage[order.fulfilmentStatus];
              const draft = trackingDrafts[order.orderNumber] ?? { provider: order.provider ?? "", trackingNumber: order.trackingNumber ?? "" };
              return (
                <article className="rowCard" key={order.orderId}>
                  <div>
                    <h3>{order.orderNumber} · {order.fulfilmentStatus.replace(/_/g, " ")}</h3>
                    <p>{order.customerEmail ?? "Guest customer"} · INR {Number(order.totalAmount ?? 0).toFixed(2)}</p>
                    <p>{formatAddress(order.shippingAddressJson)}</p>
                    {order.fulfilmentStatus === "PACKED" ? (
                      <>
                        <input
                          aria-label={`Courier for ${order.orderNumber}`}
                          placeholder="Courier (e.g. DTDC)"
                          value={draft.provider}
                          onChange={(event) => setTrackingDrafts((current) => ({ ...current, [order.orderNumber]: { ...draft, provider: event.target.value } }))}
                        />
                        <input
                          aria-label={`Tracking number for ${order.orderNumber}`}
                          placeholder="Tracking number"
                          value={draft.trackingNumber}
                          onChange={(event) => setTrackingDrafts((current) => ({ ...current, [order.orderNumber]: { ...draft, trackingNumber: event.target.value } }))}
                        />
                      </>
                    ) : order.trackingNumber ? <p>Tracking: {order.provider} {order.trackingNumber}</p> : null}
                    {next ? <button type="button" disabled={busy} onClick={() => void advanceOrder(order)}>{next.label}</button> : null}
                  </div>
                </article>
              );
            })}
            {!fulfilmentOrders.length ? <p className="privacyNote">No paid orders yet.</p> : null}
          </div>
        </section>
      ) : null}

      {activeTab === "catalog" ? (
        <section className="adminGrid" aria-label="Catalog management">
          <form className="contentPanel adminForm" onSubmit={(event) => void createCategory(event)}>
            <h2>Create category</h2>
            <label htmlFor="category-name">Name</label>
            <input id="category-name" name="name" required maxLength={120} />
            <label htmlFor="category-slug">Slug</label>
            <input id="category-slug" name="slug" required maxLength={140} />
            <label htmlFor="category-description">Description</label>
            <textarea id="category-description" name="description" rows={3} />
            <label className="checkField"><input type="checkbox" name="active" defaultChecked /> Active</label>
            <button type="submit" disabled={busy}>Create category</button>
            {recentCategory ? <p className="privacyNote">Created: {recentCategory.name} · ID {recentCategory.id}</p> : null}
          </form>

          <form className="contentPanel adminForm" onSubmit={(event) => void createProduct(event)}>
            <h2>Create product</h2>
            <label htmlFor="product-category">Category</label>
            <select id="product-category" name="categoryId" required defaultValue="">
              <option value="" disabled>Select category</option>
              {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
            </select>
            <label htmlFor="product-name">Name</label>
            <input id="product-name" name="name" required maxLength={160} />
            <label htmlFor="product-slug">Slug</label>
            <input id="product-slug" name="slug" required maxLength={180} />
            <label htmlFor="product-description">Description</label>
            <textarea id="product-description" name="description" rows={3} />
            <label className="checkField"><input type="checkbox" name="published" defaultChecked /> Published</label>
            <button type="submit" disabled={busy || categories.length === 0}>Create product</button>
            {categories.length === 0 ? <p className="privacyNote">Create a category before adding products.</p> : null}
            {recentProduct ? <p className="privacyNote">Created: {recentProduct.name} · ID {recentProduct.id}</p> : null}
          </form>

          <form className="contentPanel adminForm" onSubmit={(event) => void createVariant(event)}>
            <h2>Create variant</h2>
            <label htmlFor="variant-product">Product</label>
            <select id="variant-product" name="productId" required value={workingProductId} onChange={(event) => setWorkingProductId(event.target.value)}>
              <option value="" disabled>Select product</option>
              {products.map((product) => <option key={product.id} value={product.id}>{product.name}</option>)}
            </select>
            <label htmlFor="variant-sku">SKU</label>
            <input id="variant-sku" name="sku" required maxLength={80} />
            <div className="adminFieldPair">
              <div><label htmlFor="variant-pack-size">Pack size</label><input id="variant-pack-size" name="packSize" type="number" min="0.01" step="0.01" required /></div>
              <div><label htmlFor="variant-unit">Unit</label><select id="variant-unit" name="unit" defaultValue="KG"><option>KG</option><option>G</option><option>L</option><option>ML</option></select></div>
            </div>
            <div className="adminFieldPair">
              <div><label htmlFor="variant-weight">Weight (g)</label><input id="variant-weight" name="weightGrams" type="number" min="0" step="1" required /></div>
              <div><label htmlFor="variant-price">Price (INR)</label><input id="variant-price" name="regularPrice" type="number" min="0.01" step="0.01" required /></div>
            </div>
            <label className="checkField"><input type="checkbox" name="active" defaultChecked /> Active</label>
            <button type="submit" disabled={busy || products.length === 0}>Create variant</button>
            {recentVariant ? <p className="privacyNote">Created: {recentVariant.sku} · ID {recentVariant.id}</p> : null}
          </form>

          <form className="contentPanel adminForm" onSubmit={(event) => void uploadProductImage(event)}>
            <h2>Upload product image</h2>
            <label htmlFor="image-product">Product</label>
            <select id="image-product" name="productId" required value={workingProductId} onChange={(event) => setWorkingProductId(event.target.value)}>
              <option value="" disabled>Select product</option>
              {products.map((product) => <option key={product.id} value={product.id}>{product.name}</option>)}
            </select>
            <label htmlFor="image-file">Image (JPEG, PNG or WebP)</label>
            <input id="image-file" name="image" type="file" accept="image/jpeg,image/png,image/webp" required />
            <label htmlFor="image-alt">Alt text</label>
            <input id="image-alt" name="altText" maxLength={200} />
            <button type="submit" disabled={busy || products.length === 0}>Upload image</button>
            {uploadedImageUrl ? <p className="privacyNote">Uploaded: {uploadedImageUrl}</p> : null}
          </form>

          <section className="contentPanel adminForm" style={{ gridColumn: "1 / -1" }}>
            <h2>Products and images</h2>
            <p className="privacyNote">Each image is attached to a product by its product ID. Pick a file next to a product to set or replace its image.</p>
            {products.length === 0 ? <p className="privacyNote">No products yet. Create a category and product first.</p> : null}
            <div className="stackList">
              {products.map((product) => (
                <article key={product.id} className="rowCard">
                  <div style={{ display: "flex", gap: "1rem", alignItems: "center" }}>
                    {product.imageUrl ? (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img src={product.imageUrl} alt={product.name} width={64} height={64} style={{ objectFit: "cover", borderRadius: 8 }} />
                    ) : (
                      <span className="privacyNote" style={{ width: 64, textAlign: "center" }}>No image</span>
                    )}
                    <div>
                      <h3>{product.name} <span className="privacyNote">· ID {product.id}</span></h3>
                      <input
                        type="file"
                        accept="image/jpeg,image/png,image/webp"
                        disabled={busy}
                        aria-label={`Upload image for ${product.name}`}
                        onChange={(event) => {
                          const input = event.currentTarget;
                          const file = input.files?.[0];
                          if (file) {
                            void uploadImageFor(product.id, file, product.name, () => { input.value = ""; });
                          }
                        }}
                      />
                    </div>
                  </div>
                </article>
              ))}
            </div>
          </section>
        </section>
      ) : null}

      {activeTab === "inventory" ? (
        <section className="contentPanel adminForm adminSingleForm">
          <h2>Adjust stock</h2>
          <p>Use a positive quantity to receive stock or a negative quantity to record a reduction.</p>
          <form onSubmit={(event) => void adjustInventory(event)}>
            <label htmlFor="inventory-variant">Variant ID</label>
            <input id="inventory-variant" name="variantId" type="number" min="1" step="1" required />
            <label htmlFor="inventory-delta">Quantity change</label>
            <input id="inventory-delta" name="deltaQty" type="number" step="1" required />
            <label htmlFor="inventory-reason">Reason</label>
            <input id="inventory-reason" name="reason" required maxLength={120} placeholder="e.g. Stock received" />
            <label htmlFor="inventory-reference">Reference</label>
            <input id="inventory-reference" name="reference" maxLength={120} />
            <button type="submit" disabled={busy}>Apply adjustment</button>
          </form>
          {inventoryResult ? <p className="privacyNote">Variant {inventoryResult.variantId}: {inventoryResult.availableQty} available, {inventoryResult.reservedQty} reserved.</p> : null}
        </section>
      ) : null}

      {activeTab === "promotions" ? (
        <form className="contentPanel adminForm adminSingleForm" onSubmit={(event) => void createPromotion(event)}>
          <h2>Create promotion</h2>
          <label htmlFor="promotion-code">Code</label>
          <input id="promotion-code" name="code" required maxLength={64} />
          <div className="adminFieldPair">
            <div><label htmlFor="promotion-type">Discount type</label><select id="promotion-type" name="discountType"><option value="PERCENTAGE">Percentage</option><option value="FIXED_AMOUNT">Fixed amount</option></select></div>
            <div><label htmlFor="promotion-value">Discount value</label><input id="promotion-value" name="discountValue" type="number" min="0.01" step="0.01" required /></div>
          </div>
          <label htmlFor="promotion-minimum">Minimum order value</label>
          <input id="promotion-minimum" name="minOrderValue" type="number" min="0" step="0.01" />
          <label htmlFor="promotion-exclusion">Exclusion group</label>
          <input id="promotion-exclusion" name="exclusionGroup" maxLength={64} />
          <div className="adminFieldPair">
            <div><label htmlFor="promotion-limit-total">Total redemption limit</label><input id="promotion-limit-total" name="maxTotalRedemptions" type="number" min="1" step="1" /></div>
            <div><label htmlFor="promotion-limit-customer">Per-customer limit</label><input id="promotion-limit-customer" name="maxRedemptionsPerCustomer" type="number" min="1" step="1" /></div>
          </div>
          <div className="adminFieldPair">
            <div><label htmlFor="promotion-start">Starts at</label><input id="promotion-start" name="startsAt" type="datetime-local" /></div>
            <div><label htmlFor="promotion-end">Ends at</label><input id="promotion-end" name="endsAt" type="datetime-local" /></div>
          </div>
          <label className="checkField"><input type="checkbox" name="stackable" /> Stackable</label>
          <label className="checkField"><input type="checkbox" name="active" defaultChecked /> Active</label>
          <button type="submit" disabled={busy}>Create promotion</button>
          {promotionResult ? <p className="privacyNote">Created: {promotionResult.code} · ID {promotionResult.id}</p> : null}
        </form>
      ) : null}

      {activeTab === "imports" ? (
        <section className="adminGrid adminImportGrid">
          <div className="contentPanel adminForm">
            <h2>Catalog workbook</h2>
            <p>Download the current import template, then preview a completed workbook before applying changes.</p>
            <button type="button" className="ghost" onClick={() => void downloadTemplate()} disabled={busy}>Download template</button>
            <form onSubmit={(event) => void previewImport(event)}>
              <label htmlFor="import-file">Excel workbook</label>
              <input id="import-file" type="file" accept=".xlsx" onChange={(event) => setImportFile(event.target.files?.[0] ?? null)} />
              <button type="submit" disabled={busy || !importFile}>{busy ? "Previewing..." : "Preview workbook"}</button>
            </form>
          </div>
          {importPreview ? (
            <section className="contentPanel adminForm" aria-live="polite">
              <h2>Preview {importPreview.jobId}</h2>
              <dl className="adminImportStats">
                <div><dt>Total rows</dt><dd>{importPreview.totalRows}</dd></div>
                <div><dt>Valid rows</dt><dd>{importPreview.validRows}</dd></div>
                <div><dt>Invalid rows</dt><dd>{importPreview.invalidRows}</dd></div>
              </dl>
              <div className="inlineActions">
                <button type="button" onClick={() => void confirmImport()} disabled={busy || importPreview.validRows === 0}>
                  Confirm import
                </button>
                {importPreview.invalidRows > 0 ? <button type="button" className="ghost" onClick={() => void downloadImportErrors()} disabled={busy}>Download error CSV</button> : null}
              </div>
              {importResult ? <p className="privacyNote">Applied {importResult.appliedRows} rows; created {importResult.createdCategories} categories, {importResult.createdProducts} products, and {importResult.createdVariants} variants.</p> : null}
            </section>
          ) : null}

        </section>
      ) : null}

      {activeTab === "aftercare" ? (
        <section className="adminGrid" aria-label="Order cancellations and refunds">
          <div className="contentPanel adminForm">
            <h2>Cancellation requests</h2>
            {cancellationRequests.length ? cancellationRequests.map((request) => (
              <article className="rowCard" key={request.id}>
                <div>
                  <h3>{request.orderNumber ?? `Order ${request.orderId}`}</h3>
                  <p>{request.customerEmail ?? "Guest customer"} · {request.reason || "No reason provided"}</p>
                  <p>Status: {request.status}</p>
                  {request.status === "PROCESSING" ? (
                    <p className="privacyNote">Approval is processing. Check Razorpay before taking any manual action.</p>
                  ) : (
                    <>
                      <button type="button" disabled={busy} onClick={() => void reviewAftercare("cancellation", request.id, true)}>Approve cancellation</button>
                      <button type="button" className="ghost" disabled={busy} onClick={() => void reviewAftercare("cancellation", request.id, false)}>Reject</button>
                    </>
                  )}
                </div>
              </article>
            )) : <p className="privacyNote">No open cancellation requests.</p>}
          </div>
          <div className="contentPanel adminForm">
            <h2>Refund requests</h2>
            {refundRequests.length ? refundRequests.map((request) => (
              <article className="rowCard" key={request.id}>
                <div>
                  <h3>{request.orderNumber ?? `Order ${request.orderId}`}</h3>
                  <p>{request.customerEmail ?? "Guest customer"} · INR {Number(request.amount ?? 0).toFixed(2)}</p>
                  <p>{request.reason || "No reason provided"} · Status: {request.status}</p>
                  {request.providerRefundId ? <p>Razorpay refund ID: {request.providerRefundId} ({request.providerStatus})</p> : null}
                  {request.status === "PROCESSING" ? (
                    <p className="privacyNote">Approval is processing. Check Razorpay before taking any manual action.</p>
                  ) : (
                    <>
                      <button type="button" disabled={busy} onClick={() => void reviewAftercare("refund", request.id, true)}>Approve and refund via Razorpay</button>
                      <button type="button" className="ghost" disabled={busy} onClick={() => void reviewAftercare("refund", request.id, false)}>Reject</button>
                    </>
                  )}
                </div>
              </article>
            )) : <p className="privacyNote">No open refund requests.</p>}
          </div>
        </section>
      ) : null}

      <p className="privacyNote"><Link href="/">Return to storefront</Link></p>
    </main>
  );
}

function matchesFilter(status: string, filter: string): boolean {
  switch (filter) {
    case "ALL": return true;
    case "OPEN": return status !== "DELIVERED" && status !== "CANCELLED";
    case "READY_TO_PACK": return status === "READY_TO_PACK" || status === "SHIPMENT_CREATED";
    case "SHIPPED": return ["SHIPPED", "PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY"].includes(status);
    default: return status === filter;
  }
}

function formatAddress(json: string | null): string {
  if (!json) {
    return "No address on file";
  }
  try {
    const parsed = JSON.parse(json) as Record<string, unknown>;
    return Object.values(parsed).filter((value) => typeof value === "string" && value).join(", ");
  } catch {
    return json;
  }
}

function messageFor(error: unknown): string {
  return error instanceof Error ? error.message : "The admin request could not be completed. Try again.";
}

function downloadBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(url);
}