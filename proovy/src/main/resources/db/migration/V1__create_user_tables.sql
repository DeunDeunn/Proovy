-- 회원 도메인

CREATE TABLE users (
                       id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       provider            VARCHAR(20)  NOT NULL CHECK (provider IN ('GOOGLE', 'KAKAO')),
                       provider_id         VARCHAR(100) NOT NULL,
                       email               VARCHAR(255) NOT NULL,
                       nickname            VARCHAR(50)  NOT NULL,
                       profile_image_url   VARCHAR(500),
                       role                VARCHAR(20)  NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
                       created_at          TIMESTAMP    NOT NULL DEFAULT now(),
                       updated_at          TIMESTAMP    NOT NULL DEFAULT now(),
                       deleted_at          TIMESTAMP,
                       suspended_from      TIMESTAMP,
                       suspended_until     TIMESTAMP
);

-- 탈퇴(deleted_at) 유저는 중복 체크에서 제외 → 같은 소셜 계정 재가입 허용
CREATE UNIQUE INDEX uq_users_provider_active
    ON users (provider, provider_id)
    WHERE deleted_at IS NULL;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_timestamp('updated_at');


CREATE TABLE user_verifications (
                                    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                    user_id           BIGINT       NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                                    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED','REVOKED')),
                                    applied_at        TIMESTAMP    NOT NULL DEFAULT now(),
                                    approved_at       TIMESTAMP,
                                    rejection_reason  VARCHAR(500)
);

CREATE INDEX idx_user_verifications_user_id ON user_verifications(user_id);

-- 한 유저가 동시에 여러 건 PENDING 신청 못 하게 방지
CREATE UNIQUE INDEX uq_user_verifications_pending
    ON user_verifications(user_id)
    WHERE status = 'PENDING';

CREATE TABLE user_warnings (
                               id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               user_id                BIGINT       NOT NULL,
                               reason                 VARCHAR(30)  NOT NULL
                                   CHECK (reason IN ('AUTO_APPROVAL')),
                               challenge_id           BIGINT,
                               certification_post_id  BIGINT,
                               status                 VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                                   CHECK (status IN ('ACTIVE', 'RESOLVED', 'EXPIRED')),
                               created_at             TIMESTAMP    NOT NULL DEFAULT now()
);

-- 기간 내 유효 경고카운트용 (3회 누적)
CREATE INDEX idx_user_warnings_user_status_created
    ON user_warnings (user_id, status, created_at DESC);