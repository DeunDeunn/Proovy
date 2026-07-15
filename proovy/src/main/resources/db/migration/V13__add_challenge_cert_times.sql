-- 인증 게시글 등록 가능 시간대 (방장 검수 시간 확보용)
ALTER TABLE challenges
    ADD COLUMN cert_start_time TIME NOT NULL DEFAULT '00:00',
    ADD COLUMN cert_end_time   TIME NOT NULL DEFAULT '22:00';

ALTER TABLE challenges
    ADD CONSTRAINT chk_challenges_cert_times CHECK (cert_start_time < cert_end_time);