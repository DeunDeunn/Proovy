-- 인증 게시글 / 피드 도메인
-- 도메인 밖(users, challenge_participants) 참조는 FK 없이 컬럼만 둠

CREATE TABLE certification_posts (
                                     id                          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                     user_id                     BIGINT       NOT NULL, -- users.id (도메인 밖)
                                     challenge_participant_id    BIGINT       NOT NULL, -- challenge_participants.id (도메인 밖)
                                     contents                    TEXT,
                                     certification_image         VARCHAR(255) NOT NULL,
                                     certification_image_url     VARCHAR(500) NOT NULL,
                                     certification_status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                                         CHECK (certification_status IN ('PENDING', 'APPROVED', 'REJECTED')),
                                     approval_type               VARCHAR(20)
                                         CHECK (approval_type IN ('MANUAL', 'AUTO')),
                                     rejection_reason            VARCHAR(500),
                                     rejected_at                 TIMESTAMP,
                                     like_count                  BIGINT       NOT NULL DEFAULT 0,
                                     comment_count                BIGINT       NOT NULL DEFAULT 0,
                                     created_at                  TIMESTAMP    NOT NULL DEFAULT now(),
                                     modified_at                 TIMESTAMP,
                                     deleted_at                  TIMESTAMP
);

COMMENT ON COLUMN certification_posts.certification_image IS 'UUID 파일명';
COMMENT ON COLUMN certification_posts.like_count IS '조회용 집계 필드 (비정규화)';
COMMENT ON COLUMN certification_posts.comment_count IS '조회용 집계 필드 (비정규화)';

CREATE INDEX idx_certification_posts_user_id ON certification_posts(user_id);
CREATE INDEX idx_certification_posts_challenge_participant_id ON certification_posts(challenge_participant_id);

-- 한 챌린지당 하루 1개 제한 (포기/삭제된 것도 포함해서 카운트하므로 deleted_at 조건 없음)
CREATE UNIQUE INDEX uq_daily_certification
    ON certification_posts (challenge_participant_id, (created_at::date));

CREATE TRIGGER trg_certification_posts_modified_at
    BEFORE UPDATE ON certification_posts
    FOR EACH ROW EXECUTE FUNCTION set_timestamp('modified_at');


CREATE TABLE certification_post_images (
                                           id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                           post_id          BIGINT       NOT NULL REFERENCES certification_posts(id) ON DELETE RESTRICT,
                                           stored_filename  VARCHAR(255) NOT NULL,
                                           image_url        VARCHAR(500) NOT NULL,
                                           sort_order       INT          NOT NULL DEFAULT 0,
                                           created_at       TIMESTAMP    NOT NULL DEFAULT now(),
                                           deleted_at       TIMESTAMP
);

CREATE INDEX idx_certification_post_images_post_id ON certification_post_images(post_id);


CREATE TABLE certification_post_like (
                                         id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                         post_id     BIGINT    NOT NULL REFERENCES certification_posts(id) ON DELETE RESTRICT,
                                         user_id     BIGINT    NOT NULL, -- users.id (도메인 밖)
                                         created_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- 동일 게시글 중복 좋아요 방지
CREATE UNIQUE INDEX uq_certification_post_like ON certification_post_like(post_id, user_id);


CREATE TABLE comments (
                          id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          post_id             BIGINT    NOT NULL REFERENCES certification_posts(id) ON DELETE RESTRICT,
                          user_id             BIGINT    NOT NULL, -- users.id (도메인 밖)
                          contents            TEXT      NOT NULL,
                          parent_comment_id   BIGINT    REFERENCES comments(id) ON DELETE RESTRICT,
                          created_at          TIMESTAMP NOT NULL DEFAULT now(),
                          modified_at         TIMESTAMP,
                          deleted_at          TIMESTAMP
);

COMMENT ON COLUMN comments.parent_comment_id IS '대댓글용, 최상위 댓글은 NULL. 2-depth 제한은 애플리케이션에서 검증';

CREATE INDEX idx_comments_post_id ON comments(post_id);
CREATE INDEX idx_comments_parent_comment_id ON comments(parent_comment_id);

CREATE TRIGGER trg_comments_modified_at
    BEFORE UPDATE ON comments
    FOR EACH ROW EXECUTE FUNCTION set_timestamp('modified_at');


CREATE TABLE reports (
                         id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         user_id      BIGINT       NOT NULL, -- users.id (도메인 밖)
                         target_type  VARCHAR(20)  NOT NULL CHECK (target_type IN ('POST', 'COMMENT')),
                         target_id    BIGINT       NOT NULL,
                         reason       VARCHAR(20)  NOT NULL CHECK (reason IN ('SPAM', 'ABUSE', 'OBSCENE', 'FALSE_CERT', 'ETC')),
                         detail       VARCHAR(500),
                         status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSED')),
                         created_at   TIMESTAMP    NOT NULL DEFAULT now(),
                         processed_at TIMESTAMP
);

COMMENT ON COLUMN reports.target_id IS '다형 참조(FK 없음) — target_type에 따라 certification_posts.id 또는 comments.id';

-- 동일 유저의 동일 대상 중복 신고 방지
CREATE UNIQUE INDEX uq_reports ON reports(target_type, target_id, user_id);