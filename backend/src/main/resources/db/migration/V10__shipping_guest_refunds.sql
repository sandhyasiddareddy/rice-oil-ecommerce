create table if not exists shipping_quote (
    id bigserial primary key,
    quote_code varchar(40) not null unique,
    cart_id bigint not null references cart(id),
    status varchar(20) not null,
    shipping_fee numeric(12,2) not null,
    currency varchar(3) not null,
    provider varchar(80),
    service_level varchar(80),
    expires_at timestamptz not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists shipment (
    id bigserial primary key,
    order_id bigint not null references customer_order(id),
    shipment_code varchar(40) not null unique,
    provider varchar(80) not null,
    tracking_number varchar(120),
    status varchar(30) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists guest_order_access_token (
    id bigserial primary key,
    order_id bigint not null references customer_order(id),
    token_hash varchar(120) not null unique,
    expires_at timestamptz not null,
    created_at timestamptz not null default now()
);

create table if not exists order_cancellation_request (
    id bigserial primary key,
    order_id bigint not null references customer_order(id),
    reason varchar(300),
    status varchar(20) not null,
    requested_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists refund_request (
    id bigserial primary key,
    order_id bigint not null references customer_order(id),
    amount numeric(12,2) not null,
    reason varchar(300),
    status varchar(20) not null,
    requested_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_shipping_quote_cart_id on shipping_quote(cart_id);
create index if not exists idx_shipment_order_id on shipment(order_id);
create index if not exists idx_guest_order_access_order_id on guest_order_access_token(order_id);
create index if not exists idx_cancellation_order_id on order_cancellation_request(order_id);
create index if not exists idx_refund_order_id on refund_request(order_id);
