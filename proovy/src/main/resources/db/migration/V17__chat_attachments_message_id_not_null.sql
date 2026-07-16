-- 파일 업로드와 메시지 전송이 하나의 트랜잭션으로 통합되면서, 메시지 없이 첨부파일만
-- 먼저 만들어지는 흐름 자체가 없어졌다. message_id를 nullable로 열어뒀던 이유(V6)가
-- 사라졌으므로 NOT NULL로 강화한다.

-- 통합 설계 이전, 파일 선업로드 후 나중에 연결하던 방식으로 만들어졌던
-- message_id NULL 고아 행 정리 — 연결할 메시지 자체가 없어 백필이 불가능하므로 삭제한다.
DELETE FROM chat_attachments WHERE message_id IS NULL;

ALTER TABLE chat_attachments ALTER COLUMN message_id SET NOT NULL;
