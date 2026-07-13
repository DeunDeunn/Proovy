package com.deundeun.chat.dto.request;

import jakarta.validation.constraints.NotNull;

public record DirectChatRoomRequest(
    @NotNull(message = "상대 사용자 ID를 입력해주세요.")
    Long targetUserId
) {
}
