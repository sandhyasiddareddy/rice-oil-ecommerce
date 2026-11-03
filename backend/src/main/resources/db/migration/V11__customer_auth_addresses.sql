create table if not exists customer_account (
    id bigserial primary key,
    full_name varchar(160) not null,
    email varchar(160) not null unique,
    password_hash varchar(255) not null,
    role varchar(40) not null default 'ROLE_CUSTOMER',
    enabled boolean not null default true,
    created_at timestamptz not null default now()
);

create table if not exists customer_address (
    id bigserial primary key,
    customer_id bigint not null references customer_account(id) on delete cascade,
    label varchar(80) not null,
    full_name varchar(160) not null,
    phone varchar(30) not null,
    line1 varchar(200) not null,
    line2 varchar(200),
    city varchar(120) not null,
    state varchar(120) not null,
    postal_code varchar(30) not null,
    country varchar(80) not null default 'IN',
    is_default boolean not null default false,
    is_shipping boolean not null default true,
    is_billing boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_customer_address_customer_id
    on customer_address (customer_id, created_at);
