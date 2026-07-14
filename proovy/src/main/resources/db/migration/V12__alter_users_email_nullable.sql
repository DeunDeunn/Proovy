ALTER TABLE users ALTER COLUMN email DROP NOT NULL;

-- 탈퇴(deleted_at) 유저는 닉네임 유니크 체크에서 제외 → 재가입/재사용 허용
CREATE UNIQUE INDEX uq_users_nickname_active
    ON users (nickname)
    WHERE deleted_at IS NULL;
