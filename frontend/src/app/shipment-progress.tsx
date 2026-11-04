import type { Shipment } from "../lib/api";

const steps = ["Confirmed", "Packed", "Shipped", "Out for delivery", "Delivered"];

function stepIndex(orderStatus: string, shipment?: Shipment): number {
  if (!shipment) {
    return orderStatus === "PAID" ? 0 : -1;
  }
  switch (shipment.status) {
    case "SHIPMENT_CREATED":
    case "PACKED":
      return 1;
    case "SHIPPED":
    case "PICKED_UP":
    case "IN_TRANSIT":
      return 2;
    case "OUT_FOR_DELIVERY":
      return 3;
    case "DELIVERED":
      return 4;
    default:
      return 0;
  }
}

function safeUrl(value: string | null): string | null {
  try {
    return value && new URL(value).protocol === "https:" ? value : null;
  } catch {
    return null;
  }
}

export function ShipmentProgress({ orderStatus, shipments }: { orderStatus: string; shipments?: Shipment[] }) {
  if (orderStatus === "CANCELLED") {
    return <p className="shipmentNote">This order was cancelled.</p>;
  }
  if (orderStatus !== "PAID") {
    return <p className="shipmentNote">Shipping starts once payment is received.</p>;
  }

  const shipment = shipments?.length ? shipments[shipments.length - 1] : undefined;
  const current = stepIndex(orderStatus, shipment);
  const trackingUrl = shipment ? safeUrl(shipment.trackingUrl) : null;

  return (
    <div className="shipmentProgress">
      <ol className="shipmentSteps" aria-label="Shipping progress">
        {steps.map((label, index) => (
          <li
            key={label}
            className={index < current ? "isDone" : index === current ? "isCurrent" : ""}
            aria-current={index === current ? "step" : undefined}
          >
            <span className="shipmentDot" aria-hidden="true">{index < current ? "✓" : index + 1}</span>
            <span>{label}</span>
          </li>
        ))}
      </ol>
      {shipment?.status === "EXCEPTION" ? (
        <p className="shipmentNote">There was a delivery issue. Our team is looking into it.</p>
      ) : null}
      {shipment?.trackingNumber ? (
        <p className="shipmentNote">
          {shipment.provider} tracking: <strong>{shipment.trackingNumber}</strong>
          {trackingUrl ? <> · <a href={trackingUrl} target="_blank" rel="noreferrer">Track with carrier</a></> : null}
        </p>
      ) : !shipment ? (
        <p className="shipmentNote">Payment received. We are getting your order ready.</p>
      ) : null}
    </div>
  );
}
