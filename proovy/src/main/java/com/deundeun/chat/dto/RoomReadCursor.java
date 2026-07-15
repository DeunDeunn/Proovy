package com.deundeun.chat.dto;

// 방별 안읽음 개수 배치 조회 파라미터 (방 ID + 그 방에서 내가 마지막으로 읽은 메시지 ID)
public record RoomReadCursor(Long chatRoomId, Long lastReadMessageId) {
}
