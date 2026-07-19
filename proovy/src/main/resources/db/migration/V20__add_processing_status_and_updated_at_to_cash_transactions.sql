ALTER TABLE cash_transactions DROP CONSTRAINT cash_transactions_status_check;
ALTER TABLE cash_transactions ADD CONSTRAINT cash_transactions_status_check
    CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'));

ALTER TABLE cash_transactions ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT now();
