CREATE TABLE charge_lot_allocations (
                                         id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                         charge_lot_id  BIGINT NOT NULL REFERENCES charge_lots(id) ON DELETE RESTRICT,
                                         wallet_id      BIGINT NOT NULL REFERENCES wallets(id) ON DELETE RESTRICT,
                                         reference_id   BIGINT NOT NULL,
                                         amount         BIGINT NOT NULL,
                                         created_at     TIMESTAMP NOT NULL DEFAULT now()
);

COMMENT ON COLUMN charge_lot_allocations.reference_id IS '홀딩을 발생시킨 challenges.id (도메인 밖). CHALLENGE_HOLD 타입 cash_transactions.reference_id와 동일한 값';
COMMENT ON COLUMN charge_lot_allocations.amount IS '이 charge_lot에서 이 홀딩으로 차감된 금액. 정산 성공 시 이만큼 remaining_amount에 복구';

CREATE INDEX idx_charge_lot_allocations_wallet_reference ON charge_lot_allocations(wallet_id, reference_id);
