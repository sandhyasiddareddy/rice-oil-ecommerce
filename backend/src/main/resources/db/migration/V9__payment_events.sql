create table if not exists payment_webhook_event (
    id bigserial primary key,
    event_id varchar(120) not null unique,
    provider varchar(40) not null,
    payload_json text not null,
    processed boolean not null,
    created_at timestamptz not null default now()
);

create index if not exists idx_payment_webhook_event_processed
    on payment_webhook_event (processed, created_at);
