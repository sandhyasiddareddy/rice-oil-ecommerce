alter table customer_order
    add column if not exists payment_provider varchar(40),
    add column if not exists payment_reference varchar(120),
    add column if not exists payment_linked_at timestamptz;

create index if not exists idx_customer_order_payment_reference
    on customer_order (payment_reference);
