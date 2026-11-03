CREATE TABLE IF NOT EXISTS import_job (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(80),
    status VARCHAR(30) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    initiated_by VARCHAR(120) NOT NULL,
    total_rows INTEGER NOT NULL,
    valid_rows INTEGER NOT NULL,
    invalid_rows INTEGER NOT NULL,
    summary_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    confirmed_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_import_job_idempotency_key
    ON import_job (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS import_row_result (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES import_job(id),
    row_number INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_code VARCHAR(80),
    error_message TEXT,
    payload_json TEXT,
    CONSTRAINT uk_import_row_result_job_row UNIQUE (job_id, row_number)
);

CREATE INDEX IF NOT EXISTS idx_import_row_result_job_status
    ON import_row_result (job_id, status);

CREATE TABLE IF NOT EXISTS import_audit_log (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES import_job(id),
    action VARCHAR(60) NOT NULL,
    actor VARCHAR(120) NOT NULL,
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
