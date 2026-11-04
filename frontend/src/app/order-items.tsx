import type { OrderItem } from "../lib/api";

type Props = {
  items?: OrderItem[];
  shippingAmount?: number;
  currency?: string;
};

export function OrderItems({ items, shippingAmount, currency = "INR" }: Props) {
  if (!items?.length) {
    return null;
  }
  const money = (value: number) => `${currency} ${Number(value).toFixed(2)}`;
  return (
    <div className="orderItems">
      <ul className="orderLines">
        {items.map((item) => (
          <li key={item.sku} className="orderLine">
            <div className="orderLineInfo">
              <strong>{item.productName}</strong>
              <span className="orderLineMeta">{item.sku} · {money(item.unitPrice)} each</span>
            </div>
            <span className="orderLineQty">Qty {item.quantity}</span>
            <span className="orderLineTotal">{money(item.lineTotal)}</span>
          </li>
        ))}
      </ul>
      {shippingAmount !== undefined ? (
        <p className="orderShipping">
          <span>Shipping</span>
          <strong>{Number(shippingAmount) === 0 ? "Free" : money(shippingAmount)}</strong>
        </p>
      ) : null}
    </div>
  );
}
