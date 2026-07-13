ALTER TABLE wallets ADD COLUMN locked_charged_balance BIGINT NOT NULL DEFAULT 0;
ALTER TABLE wallets ADD COLUMN locked_reward_balance BIGINT NOT NULL DEFAULT 0;

UPDATE wallets SET locked_charged_balance = locked_balance;

ALTER TABLE wallets DROP COLUMN locked_balance;

COMMENT ON COLUMN wallets.locked_charged_balance IS '참가 중인 챌린지 참가비 중 charged_balance에서 홀딩된 금액';
COMMENT ON COLUMN wallets.locked_reward_balance IS '참가 중인 챌린지 참가비 중 reward_balance에서 홀딩된 금액';
