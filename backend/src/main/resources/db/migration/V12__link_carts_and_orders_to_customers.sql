alter table cart
    add column if not exists customer_id bigint references customer_account(id) on delete set null;

alter table customer_order
    add column if not exists customer_id bigint references customer_account(id) on delete set null;

update customer_order o
set customer_id = c.id
from customer_account c
where o.customer_id is null
  and lower(o.customer_email) = lower(c.email);

create index if not exists idx_cart_customer_status_updated
    on cart (customer_id, status, updated_at desc);

create unique index if not exists uq_cart_one_active_per_customer
    on cart (customer_id)
    where customer_id is not null and status = 'ACTIVE';

create index if not exists idx_customer_order_customer_id
    on customer_order (customer_id, id desc);
