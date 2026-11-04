create table password_reset_token (
    id bigserial primary key,
    customer_id bigint not null references customer_account(id) on delete cascade,
    token_hash varchar(64) not null unique,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null default now()
);

create index idx_password_reset_token_customer
    on password_reset_token (customer_id, created_at desc);
