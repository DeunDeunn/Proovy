-- 채팅 도메인
--
-- [설계 원칙]
-- 1. 도메인 밖 테이블(users, challenges, certification_posts 등)은 물리 FK 없이 ID 컬럼만 둔다.
--    - user_id, sender_id, uploader_id       -> users.id 논리 참조
--    - challenge_id                         -> challenges.id 논리 참조
--    - reference_id                         -> reference_type에 따라 외부 도메인 ID 논리 참조
--
-- 2. 도메인 안 테이블(chat_rooms, chat_messages)은 필요한 경우 물리 FK 제약을 둔다.
--    - chat_room_members.chat_room_id       -> chat_rooms.id
--    - chat_messages.chat_room_id           -> chat_rooms.id
--    - chat_attachments.message_id          -> chat_messages.id
--
-- 3. ON DELETE 정책은 RESTRICT를 사용한다.
--    - 채팅방, 메시지, 첨부파일은 히스토리 성격이 강하므로 물리 삭제를 막는다.
--    - 메시지 삭제는 chat_messages.deleted_at을 사용하는 soft delete로 처리한다.
--
-- [이번 수정 반영 사항]
-- 1. CREATE 순서를 도메인 흐름에 맞게 변경했다.
--    - chat_rooms -> chat_room_members -> chat_messages -> chat_attachments
--
-- 2. chat_room_members.last_read_message_id는 물리 FK를 걸지 않는다.
--    - 논리적으로는 chat_messages.id를 참조한다.
--    - 같은 채팅방의 메시지인지 여부는 서비스 로직에서 검증한다.
--
-- 3. chat_messages에 메시지 타입별 최소 유효성 CHECK를 추가했다.
--    - TEXT 메시지는 content가 반드시 있어야 한다.
--    - reference_type과 reference_id는 함께 NULL이거나 함께 값이 있어야 한다.
--    - CERTIFICATION_SHARE 메시지는 reference_type = 'CHALLENGE_CERTIFICATION'과 reference_id가 필수이다.
--
-- 4. 이전 메시지 조회는 커서 페이징을 고려하여 (chat_room_id, id DESC) 복합 인덱스를 둔다.
--
-- 5. chat_attachments.message_id는 NULL을 허용한다.
--    - 파일 선업로드 후 메시지 전송 시 message_id를 연결하는 흐름을 지원하기 위함이다.


CREATE TABLE chat_rooms (
                            id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            challenge_id      BIGINT,       -- challenges.id 논리 참조, CHALLENGE 타입일 때만 값 있음
                            direct_chat_key   VARCHAR(100), -- "min(userId):max(userId)" 형식, DIRECT 타입일 때만 값 있음
                            type              VARCHAR(30) NOT NULL CHECK (type IN ('CHALLENGE', 'DIRECT')),
                            created_at        TIMESTAMP   NOT NULL DEFAULT now(),

                            CONSTRAINT chk_chat_rooms_type CHECK (
                                (type = 'CHALLENGE' AND challenge_id IS NOT NULL AND direct_chat_key IS NULL) OR
                                (type = 'DIRECT' AND challenge_id IS NULL AND direct_chat_key IS NOT NULL)
                                )
);

-- 1:1 채팅 중복 생성 방지
CREATE UNIQUE INDEX uq_chat_rooms_direct_chat_key
    ON chat_rooms(direct_chat_key)
    WHERE direct_chat_key IS NOT NULL;

-- 챌린지 하나당 그룹 채팅방 1개
CREATE UNIQUE INDEX uq_chat_rooms_challenge
    ON chat_rooms(challenge_id)
    WHERE challenge_id IS NOT NULL;


CREATE TABLE chat_room_members (
                                   id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                   chat_room_id           BIGINT    NOT NULL REFERENCES chat_rooms(id) ON DELETE RESTRICT,
                                   user_id                BIGINT    NOT NULL, -- users.id 논리 참조
                                   last_read_message_id   BIGINT,             -- chat_messages.id 논리 참조, 물리 FK 없음
                                   last_read_at           TIMESTAMP,
                                   joined_at              TIMESTAMP NOT NULL DEFAULT now(),
                                   left_at                TIMESTAMP
);

CREATE INDEX idx_chat_room_members_chat_room_id
    ON chat_room_members(chat_room_id);

CREATE INDEX idx_chat_room_members_user_id
    ON chat_room_members(user_id);

-- 같은 유저가 같은 채팅방에 중복 참여하는 것을 방지한다.
-- left_at을 사용하는 경우 재입장은 새 row 생성이 아니라 기존 row의 left_at을 NULL로 복구하는 방식으로 처리한다.
CREATE UNIQUE INDEX uq_chat_room_members
    ON chat_room_members(chat_room_id, user_id);


CREATE TABLE chat_messages (
                               id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               chat_room_id    BIGINT       NOT NULL REFERENCES chat_rooms(id) ON DELETE RESTRICT,
                               sender_id       BIGINT       NOT NULL, -- users.id 논리 참조
                               content         VARCHAR(1000),
                               message_type    VARCHAR(30)  NOT NULL CHECK (message_type IN ('TEXT', 'IMAGE', 'FILE', 'CERTIFICATION_SHARE')),
                               reference_type  VARCHAR(50)  CHECK (reference_type IS NULL OR reference_type IN ('CHALLENGE_CERTIFICATION')),
                               reference_id    BIGINT,
                               deleted_at      TIMESTAMP,
                               created_at      TIMESTAMP    NOT NULL DEFAULT now(),

                               CONSTRAINT chk_chat_messages_text_content CHECK (
                                   message_type <> 'TEXT'
                                       OR (content IS NOT NULL AND length(trim(content)) > 0)
                                   ),

                               CONSTRAINT chk_chat_messages_reference_pair CHECK (
                                   (reference_type IS NULL AND reference_id IS NULL)
                                       OR (reference_type IS NOT NULL AND reference_id IS NOT NULL)
                                   ),

                               CONSTRAINT chk_chat_messages_certification_reference CHECK (
                                   message_type <> 'CERTIFICATION_SHARE'
                                       OR (reference_type = 'CHALLENGE_CERTIFICATION' AND reference_id IS NOT NULL)
                                   )
);

-- 이전 메시지 커서 페이징 조회용
CREATE INDEX idx_chat_messages_room_id_desc
    ON chat_messages(chat_room_id, id DESC);

CREATE INDEX idx_chat_messages_sender_id
    ON chat_messages(sender_id);


CREATE TABLE chat_attachments (
                                  id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                  message_id            BIGINT REFERENCES chat_messages(id) ON DELETE RESTRICT,
                                  uploader_id           BIGINT       NOT NULL, -- users.id 논리 참조
                                  file_url              VARCHAR(500) NOT NULL,
                                  original_file_name    VARCHAR(255) NOT NULL,
                                  file_type             VARCHAR(30)  NOT NULL CHECK (file_type IN ('IMAGE', 'FILE')),
                                  file_size             BIGINT       NOT NULL,
                                  created_at            TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_attachments_message_id
    ON chat_attachments(message_id);

CREATE INDEX idx_chat_attachments_uploader_id
    ON chat_attachments(uploader_id);


-- 알림 도메인
--
-- [설계 원칙]
-- 1. notifications는 사용자별 개인 알림을 저장하는 테이블이다.
--    - 하나의 이벤트가 여러 사용자에게 전달되는 경우에도 수신자 수만큼 row를 각각 생성한다.
--
-- 2. 도메인 밖 테이블(users, challenges, certification_posts, settlements 등)은 물리 FK 없이 ID 컬럼만 둔다.
--    - user_id    -> users.id 논리 참조
--    - target_id  -> target_type에 따라 외부 도메인 ID 논리 참조
--
-- 3. 알림은 실시간 전송 여부와 관계없이 반드시 DB에 저장한다.
--    - 접속 중이면 SSE로 실시간 전송
--    - 미접속 상태이면 알림함에서 추후 조회
--
-- 4. 읽음/삭제는 상태 컬럼으로 관리한다.
--    - read_at IS NULL이면 안 읽은 알림
--    - deleted_at IS NULL이면 삭제되지 않은 알림
--    - 알림 삭제는 물리 삭제가 아니라 deleted_at을 사용하는 soft delete로 처리한다.
--
-- [기존 대비 수정 반영 사항]
-- 1. target_type과 target_id의 쌍 검증 CHECK를 추가했다.
--    - target_type만 있고 target_id가 없는 상태를 방지한다.
--    - target_id만 있고 target_type이 없는 상태를 방지한다.
--    - 둘은 함께 NULL이거나 함께 값이 있어야 한다.
--
-- 2. 기존 user_id 단일 인덱스 대신 실제 조회 패턴에 맞는 partial index를 추가했다.
--    - 알림 목록 조회용: user_id + created_at DESC, deleted_at IS NULL
--    - 안 읽은 알림 개수 조회용: user_id, read_at IS NULL, deleted_at IS NULL
--
-- 3. event_key는 기존과 동일하게 VARCHAR(100)으로 유지한다.
--    - 같은 이벤트로 동일 알림이 중복 생성되는 것을 방지하기 위해 UNIQUE 인덱스를 둔다.


CREATE TABLE notifications (
                               id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               user_id      BIGINT       NOT NULL, -- users.id 논리 참조
                               type         VARCHAR(50)  NOT NULL
                                   CHECK (type IN (
                                                   'VERIFICATION_SUBMITTED',
                                                   'VERIFICATION_APPROVED',
                                                   'VERIFICATION_REJECTED',
                                                   'SETTLEMENT_COMPLETED',
                                                   'HOST_REVENUE_PAID',
                                                   'BADGE_APPROVED',
                                                   'BADGE_REJECTED'
                                       )),
                               title        VARCHAR(100) NOT NULL,
                               content      VARCHAR(500) NOT NULL,
                               target_type  VARCHAR(50)  CHECK (
                                   target_type IS NULL OR
                                   target_type IN (
                                                   'CHALLENGE',
                                                   'VERIFICATION_POST',
                                                   'SETTLEMENT',
                                                   'HOST_REVENUE',
                                                   'BADGE_APPLICATION'
                                       )
                                   ),
                               target_id    BIGINT,
                               event_key    VARCHAR(100) NOT NULL,
                               read_at      TIMESTAMP,
                               deleted_at   TIMESTAMP,
                               created_at   TIMESTAMP    NOT NULL DEFAULT now(),

                               CONSTRAINT chk_notifications_target_pair CHECK (
                                   (target_type IS NULL AND target_id IS NULL)
                                       OR (target_type IS NOT NULL AND target_id IS NOT NULL)
                                   )
);

-- 같은 이벤트로 동일 알림 중복 발송 방지
CREATE UNIQUE INDEX uq_notifications_event_key
    ON notifications(event_key);

-- 알림 목록 조회용
-- 조회 예시:
-- WHERE user_id = ?
--   AND deleted_at IS NULL
-- ORDER BY created_at DESC
CREATE INDEX idx_notifications_user_active_created_at
    ON notifications(user_id, created_at DESC)
    WHERE deleted_at IS NULL;

-- 안 읽은 알림 개수 조회용
-- 조회 예시:
-- WHERE user_id = ?
--   AND read_at IS NULL
--   AND deleted_at IS NULL
CREATE INDEX idx_notifications_user_unread
    ON notifications(user_id)
    WHERE read_at IS NULL
  AND deleted_at IS NULL;