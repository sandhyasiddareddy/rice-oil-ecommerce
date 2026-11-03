alter table shipment
    add column if not exists tracking_url varchar(500);

create table if not exists shipment_status_event (
    id bigserial primary key,
    shipment_id bigint not null references shipment(id) on delete cascade,
    status varchar(30) not null,
    description varchar(500),
    occurred_at timestamptz not null default now()
);

create index if not exists idx_shipment_event_timeline
    on shipment_status_event (shipment_id, occurred_at desc);
