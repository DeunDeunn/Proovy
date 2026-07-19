-- 신고 처리 결과에 '기각(REJECTED)' 추가
ALTER TABLE reports DROP CONSTRAINT reports_status_check;
ALTER TABLE reports ADD CONSTRAINT reports_status_check
    CHECK (status IN ('PENDING', 'PROCESSED', 'REJECTED'));

-- penalted_at에 저장된 날짜까지 패널티 적용됨
ALTER TABLE users ADD COLUMN penalted_at TIMESTAMP;