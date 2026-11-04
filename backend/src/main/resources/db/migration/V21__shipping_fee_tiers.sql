create table if not exists shipping_fee_tier (
    id bigserial primary key,
    min_order_amount numeric(12,2) not null,
    fee numeric(12,2) not null,
    created_at timestamptz not null default now(),
    constraint uq_shipping_fee_tier_min unique (min_order_amount),
    constraint ck_shipping_fee_tier_nonneg check (min_order_amount >= 0 and fee >= 0)
);

insert into shipping_fee_tier (min_order_amount, fee) values (0, 80), (499, 40), (999, 0);

alter table customer_order add column if not exists shipping_amount numeric(12,2) not null default 0;
