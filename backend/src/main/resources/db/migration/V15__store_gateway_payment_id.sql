alter table customer_order
    add column if not exists payment_id varchar(120);
