-- 파일 업로드와 메시지 전송이 하나의 트랜잭션으로 통합되면서, 메시지 없이 첨부파일만
-- 먼저 만들어지는 흐름 자체가 없어졌다. message_id를 nullable로 열어뒀던 이유(V6)가
-- 사라졌으므로 NOT NULL로 강화한다.
ALTER TABLE chat_attachments ALTER COLUMN message_id SET NOT NULL;
