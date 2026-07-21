-- 댓글 좋아요: 사용자별 좋아요 기록 + 목록 조회용 집계 컬럼
ALTER TABLE comments
    ADD COLUMN like_count BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN comments.like_count IS '조회용 집계 필드 (비정규화)';

-- 좋아요 집계 변경은 본문 수정이 아니므로 "수정됨" 표시의 기준인 modified_at을 바꾸지 않는다.
DROP TRIGGER trg_comments_modified_at ON comments;

CREATE TRIGGER trg_comments_modified_at
    BEFORE UPDATE OF contents ON comments
    FOR EACH ROW EXECUTE FUNCTION set_timestamp('modified_at');

CREATE TABLE comment_like (
                              id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                              comment_id  BIGINT    NOT NULL REFERENCES comments(id) ON DELETE RESTRICT,
                              user_id     BIGINT    NOT NULL, -- users.id (도메인 밖)
                              created_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- 동일 댓글에 대한 사용자별 중복 좋아요 방지
CREATE UNIQUE INDEX uq_comment_like ON comment_like(comment_id, user_id);
