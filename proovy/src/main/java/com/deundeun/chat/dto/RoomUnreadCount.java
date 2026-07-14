package com.deundeun.chat.dto;

// 방별 안읽음 개수 배치 조회 결과
public record RoomUnreadCount(Long chatRoomId, int unreadCount) {
}
