create table if not exists cart (
    id bigserial primary key,
    status varchar(20) not null,
    currency varchar(3) not null,
    tax_code varchar(80),
    coupon_codes_json text,
    subtotal_amount numeric(12,2) not null,
    tax_amount numeric(12,2) not null,
    discount_amount numeric(12,2) not null,
    total_amount numeric(12,2) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists cart_item (
    id bigserial primary key,
    cart_id bigint not null references cart(id) on delete cascade,
    sku varchar(80) not null,
    product_name varchar(160) not null,
    unit_price numeric(12,2) not null,
    quantity integer not null,
    line_total numeric(12,2) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_cart_item_cart_id on cart_item(cart_id);
