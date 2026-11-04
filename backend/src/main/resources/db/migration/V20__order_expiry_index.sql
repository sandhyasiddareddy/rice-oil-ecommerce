CREATE INDEX IF NOT EXISTS idx_customer_order_status_created ON customer_order(status, created_at);
