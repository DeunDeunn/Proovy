package com.deundeun.certification.dto;

import lombok.Data;
import java.time.LocalDateTime;

// TODO : 검수 대기 목록의 글 요약인데 화면 나오고 수정
@Data
public class PendingCertificationResponse {
    private Long postId;
    private Long authorId;
    private String thumbnailUrl;
    private String contents;
    private LocalDateTime createdAt;
}
