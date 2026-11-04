alter table refund_request
    add column if not exists provider_refund_id varchar(120),
    add column if not exists provider_status varchar(40);

create unique index if not exists idx_refund_provider_refund_id
    on refund_request(provider_refund_id)
    where provider_refund_id is not null;

alter table order_cancellation_request
    add column if not exists refund_request_id bigint references refund_request(id);
