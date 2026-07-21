package com.deundeun.chat.dto.request;

import jakarta.validation.constraints.NotNull;

public record CertificationShareRequest(
    @NotNull(message = "공유할 인증글 ID를 입력해주세요.")
    Long certificationId
) {
}
