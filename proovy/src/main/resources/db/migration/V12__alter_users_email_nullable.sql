ALTER TABLE users ALTER COLUMN email DROP NOT NULL;

-- 탈퇴(deleted_at) 유저는 닉네임 유니크 체크에서 제외 → 재가입/재사용 허용
-- 유니크 인덱스 생성 전, 기존 활성 유저 중 닉네임 중복이 있으면 인덱스 생성이 실패하므로 먼저 정리
WITH duplicates AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY nickname ORDER BY id) AS rn
    FROM users
    WHERE deleted_at IS NULL
)
UPDATE users u
SET nickname = u.nickname || '_' || duplicates.rn
FROM duplicates
WHERE u.id = duplicates.id
  AND duplicates.rn > 1;

CREATE UNIQUE INDEX uq_users_nickname_active
    ON users (nickname)
    WHERE deleted_at IS NULL;
