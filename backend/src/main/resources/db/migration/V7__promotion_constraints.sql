alter table promotion
    add column if not exists stackable boolean not null default false,
    add column if not exists exclusion_group varchar(80),
    add column if not exists max_total_redemptions integer,
    add column if not exists max_redemptions_per_customer integer,
    add column if not exists redeemed_count integer not null default 0;

create table if not exists promotion_customer_redemption (
    id bigserial primary key,
    promotion_id bigint not null references promotion(id),
    customer_email varchar(160) not null,
    redemption_count integer not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (promotion_id, customer_email)
);

create index if not exists idx_promotion_customer_redemption_lookup
    on promotion_customer_redemption (promotion_id, customer_email);
