-- 피드 공개 범위 (전체 공개 / 챌린지 참가자만)
ALTER TABLE challenges
    ADD COLUMN feed_visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC'
        CHECK (feed_visibility IN ('PUBLIC', 'PARTICIPANTS_ONLY'));