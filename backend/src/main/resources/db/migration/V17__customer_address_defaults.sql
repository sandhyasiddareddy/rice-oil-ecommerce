with ranked_defaults as (
    select id,
           row_number() over (partition by customer_id order by created_at, id) as rn
    from customer_address
    where is_default = true
)
update customer_address
set is_default = false
where id in (
    select id
    from ranked_defaults
    where rn > 1
);

create unique index if not exists uq_customer_address_single_default
    on customer_address (customer_id)
    where is_default = true;
