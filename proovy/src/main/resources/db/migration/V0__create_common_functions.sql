-- ============================================================
-- 공통 함수: updated_at / modified_at 자동 갱신 트리거
-- MyBatis는 JPA @PreUpdate 같은 훅이 없어서, DB 트리거로 보장
--
-- 사용법: CREATE TRIGGER ... EXECUTE FUNCTION set_timestamp('updated_at');
--        CREATE TRIGGER ... EXECUTE FUNCTION set_timestamp('modified_at');
-- ============================================================

CREATE OR REPLACE FUNCTION set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    CASE TG_ARGV[0]
        WHEN 'updated_at' THEN
            NEW.updated_at := now();
        WHEN 'modified_at' THEN
            NEW.modified_at := now();
        ELSE
            RAISE EXCEPTION 'set_timestamp: unknown argument %', TG_ARGV[0];
    END CASE;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
