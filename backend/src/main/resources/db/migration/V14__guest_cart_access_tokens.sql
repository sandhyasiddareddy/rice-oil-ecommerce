alter table cart
    add column if not exists guest_access_token_hash varchar(64);
