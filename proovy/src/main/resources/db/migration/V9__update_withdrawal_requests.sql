ALTER TABLE withdrawal_requests DROP CONSTRAINT withdrawal_requests_status_check;
ALTER TABLE withdrawal_requests ADD CONSTRAINT withdrawal_requests_status_check
    CHECK (status IN ('PENDING', 'REJECTED', 'COMPLETED'));

ALTER TABLE withdrawal_requests ADD COLUMN reject_reason VARCHAR(255);
COMMENT ON COLUMN withdrawal_requests.reject_reason IS '반려 사유. status=REJECTED일 때만 값 존재';
