create table if not exists tax_rule (
    id bigserial primary key,
    code varchar(80) not null,
    name varchar(160) not null,
    rate_percent numeric(8,4) not null,
    inclusive boolean not null,
    active boolean not null,
    effective_from timestamptz not null,
    effective_to timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create unique index if not exists ux_tax_rule_code_effective_from
    on tax_rule (code, effective_from);

create index if not exists idx_tax_rule_active_window
    on tax_rule (active, code, effective_from, effective_to);

create table if not exists promotion (
    id bigserial primary key,
    code varchar(80) not null unique,
    discount_type varchar(30) not null,
    discount_value numeric(12,2) not null,
    min_order_value numeric(12,2),
    active boolean not null,
    starts_at timestamptz,
    ends_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_promotion_active_window
    on promotion (active, starts_at, ends_at);
