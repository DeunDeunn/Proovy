-- 결제 / 캐시 / 정산 도메인
-- 도메인 밖(users, challenges) 참조는 FK 없이 컬럼만 둠
-- 도메인 안 참조(wallets, settlements)는 FK 제약 있음 (ON DELETE RESTRICT)

CREATE TABLE wallets (
                         id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         user_id          BIGINT    NOT NULL, -- users.id (도메인 밖)
                         charged_balance  BIGINT    NOT NULL DEFAULT 0,
                         reward_balance   BIGINT    NOT NULL DEFAULT 0,
                         locked_balance   BIGINT    NOT NULL DEFAULT 0,
                         created_at       TIMESTAMP NOT NULL DEFAULT now(),
                         updated_at       TIMESTAMP NOT NULL DEFAULT now()
);

COMMENT ON COLUMN wallets.charged_balance IS '충전으로 쌓인 잔액';
COMMENT ON COLUMN wallets.reward_balance IS '정산 수익으로 쌓인 잔액';
COMMENT ON COLUMN wallets.locked_balance IS '참가 중인 챌린지 참가비 홀딩액';

-- 탈퇴 여부와 무관하게 유저당 지갑 1개
CREATE UNIQUE INDEX uq_wallets_user_id ON wallets(user_id);

CREATE TRIGGER trg_wallets_updated_at
    BEFORE UPDATE ON wallets
    FOR EACH ROW EXECUTE FUNCTION set_timestamp('updated_at');


CREATE TABLE charge_lots (
                             id                BIGINT    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             wallet_id         BIGINT    NOT NULL REFERENCES wallets(id) ON DELETE RESTRICT,
                             amount            BIGINT    NOT NULL,
                             remaining_amount  BIGINT    NOT NULL,
                             charged_at        TIMESTAMP NOT NULL DEFAULT now(),
                             withdrawable_at   TIMESTAMP GENERATED ALWAYS AS (charged_at + INTERVAL '7 days') STORED
);

COMMENT ON COLUMN charge_lots.amount IS '최초 충전액';
COMMENT ON COLUMN charge_lots.remaining_amount IS '참가비 홀딩으로 FIFO 차감된 후 남은 금액. wallets.charged_balance 소진 시 이 컬럼도 오래된 lot부터 같이 차감해야 함 (SUM(remaining_amount) = wallets.charged_balance 불변식 유지)';
COMMENT ON COLUMN charge_lots.withdrawable_at IS 'charged_at + 7일, Generated Column이라 INSERT 시 값 넣지 않음';

CREATE INDEX idx_charge_lots_wallet_id ON charge_lots(wallet_id);


CREATE TABLE cash_transactions (
                                   id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                   wallet_id          BIGINT       NOT NULL REFERENCES wallets(id) ON DELETE RESTRICT,
                                   type               VARCHAR(30)  NOT NULL
                                       CHECK (type IN ('CHARGE', 'CHALLENGE_HOLD', 'CHALLENGE_PRINCIPAL_REFUND',
                                                       'CHALLENGE_PROFIT_DISTRIBUTION', 'HOST_FEE', 'WITHDRAWAL',
                                                       'AI_TICKET_PURCHASE', 'AI_TICKET_REFUND')),
                                   amount             BIGINT       NOT NULL,
                                   balance_after      BIGINT       NOT NULL,
                                   pg_transaction_id  VARCHAR(100),
                                   status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
                                   reference_id       BIGINT,
                                   created_at         TIMESTAMP    NOT NULL DEFAULT now()
);

COMMENT ON COLUMN cash_transactions.balance_after IS '감사 추적용 스냅샷';
COMMENT ON COLUMN cash_transactions.pg_transaction_id IS '네이버페이 거래 ID (충전 시)';
COMMENT ON COLUMN cash_transactions.reference_id IS '다형 참조(FK 없음) — type에 따라 challenges.id / settlements.id / withdrawal_requests.id / ai_ticket_subscriptions.id 중 하나';

CREATE INDEX idx_cash_transactions_wallet_id ON cash_transactions(wallet_id);


CREATE TABLE settlements (
                             id                          BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             challenge_id                BIGINT        NOT NULL, -- challenges.id (도메인 밖)
                             per_person_fee              BIGINT        NOT NULL,
                             total_participant_count     INT           NOT NULL,
                             success_user_count          INT           NOT NULL,
                             fail_user_count             INT           NOT NULL,
                             failure_pool                BIGINT        NOT NULL,
                             participant_share_rate      NUMERIC(5,4)  NOT NULL DEFAULT 0.7,
                             platform_fee_rate           NUMERIC(5,4)  NOT NULL DEFAULT 0.2,
                             host_fee_rate               NUMERIC(5,4)  NOT NULL DEFAULT 0.1,
                             participant_share_amount    BIGINT        NOT NULL,
                             platform_fee_amount         BIGINT        NOT NULL,
                             host_fee_amount             BIGINT        NOT NULL,
                             profit_per_user             BIGINT        NOT NULL,
                             rounding_remainder          BIGINT        NOT NULL DEFAULT 0,
                             is_host_disqualified        BOOLEAN       NOT NULL DEFAULT false,
                             status                      VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                                 CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
                             settled_at                  TIMESTAMP
);

COMMENT ON COLUMN settlements.failure_pool IS 'fail_user_count × per_person_fee';
COMMENT ON COLUMN settlements.participant_share_rate IS '스냅샷, 엣지케이스별 변동 (일반 0.7 / 성공자0명 0 / 방장자격박탈 0.7)';
COMMENT ON COLUMN settlements.platform_fee_rate IS '스냅샷, 엣지케이스별 변동 (일반 0.2 / 성공자0명 0.9 / 방장자격박탈 0.3)';
COMMENT ON COLUMN settlements.host_fee_rate IS '스냅샷, 엣지케이스별 변동 (일반 0.1 / 성공자0명 0.1 / 방장자격박탈 0)';
COMMENT ON COLUMN settlements.is_host_disqualified IS '방장이 자격 박탈된 경우(자진 탈퇴는 불가 정책). true면 방장 몫이 플랫폼으로 흡수됨';
COMMENT ON COLUMN settlements.profit_per_user IS 'floor(participant_share_amount / success_user_count)';
COMMENT ON COLUMN settlements.rounding_remainder IS 'n빵 잔돈, platform_fee_amount에 합산됨';

CREATE INDEX idx_settlements_challenge_id ON settlements(challenge_id);


CREATE TABLE host_revenues (
                               id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               host_id        BIGINT      NOT NULL, -- users.id (도메인 밖)
                               challenge_id   BIGINT      NOT NULL, -- challenges.id (도메인 밖)
                               settlement_id  BIGINT      NOT NULL REFERENCES settlements(id) ON DELETE RESTRICT,
                               amount         BIGINT      NOT NULL,
                               status         VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PAID')),
                               paid_at        TIMESTAMP
);

COMMENT ON COLUMN host_revenues.amount IS '방장 자격 박탈 시 레코드 미생성';

CREATE INDEX idx_host_revenues_host_id ON host_revenues(host_id);
CREATE INDEX idx_host_revenues_challenge_id ON host_revenues(challenge_id);

-- 정산 1건당 방장 수익 지급 1건 (중복 지급 방지)
CREATE UNIQUE INDEX uq_host_revenues_settlement ON host_revenues(settlement_id);


CREATE TABLE withdrawal_requests (
                                     id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                     wallet_id             BIGINT       NOT NULL REFERENCES wallets(id) ON DELETE RESTRICT,
                                     source_type           VARCHAR(20)  NOT NULL CHECK (source_type IN ('CHARGED', 'REWARD')),
                                     amount                BIGINT       NOT NULL,
                                     fee_amount            BIGINT       NOT NULL,
                                     net_transfer_amount   BIGINT       NOT NULL,
                                     bank_name             VARCHAR(50)  NOT NULL,
                                     account_number        VARCHAR(50)  NOT NULL,
                                     account_holder_name   VARCHAR(50)  NOT NULL,
                                     status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                                         CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'COMPLETED')),
                                     requested_at          TIMESTAMP    NOT NULL DEFAULT now(),
                                     processed_at          TIMESTAMP
);

COMMENT ON COLUMN withdrawal_requests.fee_amount IS 'amount × 0.05(충전) 또는 × 0.01(리워드)';
COMMENT ON COLUMN withdrawal_requests.net_transfer_amount IS 'amount - fee_amount';

CREATE INDEX idx_withdrawal_requests_wallet_id ON withdrawal_requests(wallet_id);