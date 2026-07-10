-- AI 티켓 / 검수 도메인
-- 도메인 밖(users, challenges, certification_posts, certification_post_images) 참조는
-- FK 없이 컬럼만 둠

CREATE TABLE ai_ticket_plans (
                                 id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                 name          VARCHAR(50)  NOT NULL,
                                 duration_days INT          NOT NULL,
                                 price         INT          NOT NULL,
                                 description   VARCHAR(255),
                                 active        BOOLEAN      NOT NULL DEFAULT true,
                                 created_at    TIMESTAMP    NOT NULL DEFAULT now(),
                                 updated_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_ai_ticket_plans_updated_at
    BEFORE UPDATE ON ai_ticket_plans
    FOR EACH ROW EXECUTE FUNCTION set_timestamp('updated_at');


CREATE TABLE ai_ticket_subscriptions (
                                         id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                         host_id     BIGINT      NOT NULL, -- users.id (도메인 밖)
                                         plan_id     BIGINT      NOT NULL REFERENCES ai_ticket_plans(id) ON DELETE RESTRICT,
                                         paid_price  INT      NOT NULL,
                                         started_at  TIMESTAMP   NOT NULL,
                                         expired_at  TIMESTAMP   NOT NULL,
                                         status      VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'CANCELLED', 'EXPIRED', 'REFUNDED')),
                                         created_at  TIMESTAMP   NOT NULL DEFAULT now(),
                                         updated_at  TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_ticket_subscriptions_host_id ON ai_ticket_subscriptions(host_id);
CREATE INDEX idx_ai_ticket_subscriptions_plan_id ON ai_ticket_subscriptions(plan_id);

CREATE TRIGGER trg_ai_ticket_subscriptions_updated_at
    BEFORE UPDATE ON ai_ticket_subscriptions
    FOR EACH ROW EXECUTE FUNCTION set_timestamp('updated_at');


CREATE TABLE ai_tickets (
                            id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            host_id          BIGINT      NOT NULL, -- users.id (도메인 밖)
                            subscription_id  BIGINT      NOT NULL REFERENCES ai_ticket_subscriptions(id) ON DELETE RESTRICT,
                            type             VARCHAR(20) NOT NULL CHECK (type IN ('PURCHASE', 'USE', 'REFUND', 'EXPIRE')),
                            created_at       TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_tickets_host_id ON ai_tickets(host_id);
CREATE INDEX idx_ai_tickets_subscription_id ON ai_tickets(subscription_id);


CREATE TABLE ai_review_results (
                                   id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                   challenge_id           BIGINT       NOT NULL, -- challenges.id (도메인 밖)
                                   host_id                BIGINT       NOT NULL, -- users.id (도메인 밖)
                                   review_image_id        BIGINT       NOT NULL, -- certification_post_images.id (도메인 밖)
                                   verification_post_id   BIGINT       NOT NULL, -- certification_posts.id (도메인 밖)
                                   review_mode            VARCHAR(20)  NOT NULL,
                                   decision                VARCHAR(20)  CHECK (decision IS NULL OR decision IN ('APPROVED', 'REJECTED', 'NEEDS_REVIEW')),
                                   confidence             NUMERIC(4,3),
                                   reason                 TEXT,
                                   raw_response           JSONB,
                                   status                 VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                                       CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
                                   previous_post_status   VARCHAR(20)  CHECK (previous_post_status IS NULL OR previous_post_status IN ('PENDING', 'APPROVED', 'REJECTED')),
                                   new_post_status        VARCHAR(20)  CHECK (new_post_status IS NULL OR new_post_status IN ('PENDING', 'APPROVED', 'REJECTED')),
                                   created_at             TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_review_results_challenge_id ON ai_review_results(challenge_id);
CREATE INDEX idx_ai_review_results_host_id ON ai_review_results(host_id);

-- 인증글 하나당 검수 결과 1건만 (재검수 시 갱신 방식이라면 이 제약 재검토 필요)
CREATE UNIQUE INDEX uq_ai_review_results_verification_post ON ai_review_results(verification_post_id);


CREATE TABLE ai_review_rules (
                                 id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                 host_id      BIGINT      NOT NULL, -- users.id (도메인 밖)
                                 challenge_id BIGINT      NOT NULL, -- challenges.id (도메인 밖)
                                 rule_text    TEXT        NOT NULL,
                                 review_mode  VARCHAR(20) NOT NULL,
                                 active       BOOLEAN     NOT NULL DEFAULT true,
                                 created_at   TIMESTAMP   NOT NULL DEFAULT now(),
                                 updated_at   TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_review_rules_host_id ON ai_review_rules(host_id);

-- 챌린지 하나당 검수 기준 1건
CREATE UNIQUE INDEX uq_ai_review_rules_challenge ON ai_review_rules(challenge_id);

CREATE TRIGGER trg_ai_review_rules_updated_at
    BEFORE UPDATE ON ai_review_rules
    FOR EACH ROW EXECUTE FUNCTION set_timestamp('updated_at');