-- 홀딩이 어느 charge_lot에서 얼마를 가져갔는지 기록 (정산 성공 시 정확히 복구하기 위함)
CREATE TABLE charge_lot_allocations (
                                         id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                         charge_lot_id  BIGINT NOT NULL REFERENCES charge_lots(id) ON DELETE RESTRICT,
                                         wallet_id      BIGINT NOT NULL REFERENCES wallets(id) ON DELETE RESTRICT,
                                         reference_id   BIGINT NOT NULL,
                                         amount         BIGINT NOT NULL,
                                         created_at     TIMESTAMP NOT NULL DEFAULT now(),
                                         released_at    TIMESTAMP
);

COMMENT ON COLUMN charge_lot_allocations.reference_id IS '홀딩을 발생시킨 challenges.id (도메인 밖). CHALLENGE_HOLD 타입 cash_transactions.reference_id와 동일한 값';
COMMENT ON COLUMN charge_lot_allocations.amount IS '이 charge_lot에서 이 홀딩으로 차감된 금액. 정산 성공 시 이만큼 remaining_amount에 복구';
COMMENT ON COLUMN charge_lot_allocations.released_at IS '정산 성공으로 복구 처리된 시각. null이면 아직 미처리 (같은 참조로 중복 복구되는 것을 방지)';

ALTER TABLE charge_lot_allocations ADD CONSTRAINT uq_charge_lot_allocations_wallet_reference_lot
    UNIQUE (wallet_id, reference_id, charge_lot_id);

-- wallets.locked_balance를 홀딩 출처별로 분리
ALTER TABLE wallets ADD COLUMN locked_charged_balance BIGINT NOT NULL DEFAULT 0;
ALTER TABLE wallets ADD COLUMN locked_reward_balance BIGINT NOT NULL DEFAULT 0;

UPDATE wallets SET locked_charged_balance = locked_balance;

ALTER TABLE wallets DROP COLUMN locked_balance;

COMMENT ON COLUMN wallets.locked_charged_balance IS '참가 중인 챌린지 참가비 중 charged_balance에서 홀딩된 금액';
COMMENT ON COLUMN wallets.locked_reward_balance IS '참가 중인 챌린지 참가비 중 reward_balance에서 홀딩된 금액';
