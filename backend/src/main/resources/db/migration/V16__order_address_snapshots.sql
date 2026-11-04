alter table customer_order
    add column if not exists shipping_address_json text,
    add column if not exists billing_address_json text;
