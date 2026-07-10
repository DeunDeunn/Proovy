-- 챌린지방 도메인
-- 도메인 밖(users) 참조는 FK 제약 없이 컬럼만 둠

CREATE TABLE categories (
                            id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            name        VARCHAR(30) NOT NULL,
                            sort_order  INT         NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_categories_name ON categories(name);

CREATE TABLE challenges (
                            id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            host_id                BIGINT       NOT NULL, -- users.id (도메인 밖, FK 없음)
                            title                  VARCHAR(100) NOT NULL,
                            description            TEXT,
                            category_id            BIGINT       NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
                            entry_fee              BIGINT       NOT NULL DEFAULT 0,
                            verification_method    TEXT         NOT NULL,
                            cert_frequency         VARCHAR(20)  NOT NULL DEFAULT 'DAILY',
                            daily_cert_limit       INT          NOT NULL DEFAULT 1,
                            success_criteria_rate  INT          NOT NULL DEFAULT 80,
                            ai_review_enabled      BOOLEAN      NOT NULL DEFAULT false,
                            start_date             DATE         NOT NULL,
                            end_date               DATE         NOT NULL,
                            max_participants       INT          NOT NULL,
                            status                 VARCHAR(20)  NOT NULL DEFAULT 'RECRUITING'
                                CHECK (status IN ('RECRUITING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
                            created_at             TIMESTAMP    NOT NULL DEFAULT now(),
                            updated_at             TIMESTAMP    NOT NULL DEFAULT now()
);

COMMENT ON COLUMN challenges.success_criteria_rate IS '고정값 80%, 애플리케이션에서 강제 (단위: %)';

CREATE INDEX idx_challenges_host_id ON challenges(host_id);
CREATE INDEX idx_challenges_end_date ON challenges(end_date);
CREATE INDEX idx_challenges_category_id ON challenges(category_id);

CREATE TRIGGER trg_challenges_updated_at
    BEFORE UPDATE ON challenges
    FOR EACH ROW EXECUTE FUNCTION set_timestamp('updated_at');


CREATE TABLE challenge_participants (
                                        id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                        challenge_id  BIGINT      NOT NULL REFERENCES challenges(id) ON DELETE RESTRICT,
                                        user_id       BIGINT      NOT NULL, -- users.id (도메인 밖, FK 없음)
                                        status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'WITHDRAWN', 'KICKED')),
                                        result        VARCHAR(20) CHECK (result IS NULL OR result IN ('SUCCESS', 'FAIL')),
                                        joined_at     TIMESTAMP   NOT NULL DEFAULT now(),
                                        left_at       TIMESTAMP
);

CREATE INDEX idx_challenge_participants_challenge_id ON challenge_participants(challenge_id);
CREATE INDEX idx_challenge_participants_user_id ON challenge_participants(user_id);

-- 한 유저가 같은 방에 중복 참가 못 하게 방지
CREATE UNIQUE INDEX uq_challenge_participants ON challenge_participants(challenge_id, user_id);