create table if not exists customer_order (
    id bigserial primary key,
    order_number varchar(40) not null unique,
    cart_id bigint not null references cart(id),
    idempotency_key varchar(120) not null unique,
    status varchar(20) not null,
    currency varchar(3) not null,
    subtotal_amount numeric(12,2) not null,
    tax_amount numeric(12,2) not null,
    discount_amount numeric(12,2) not null,
    total_amount numeric(12,2) not null,
    customer_email varchar(160),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_customer_order_cart_id on customer_order(cart_id);
create index if not exists idx_customer_order_created_at on customer_order(created_at);
