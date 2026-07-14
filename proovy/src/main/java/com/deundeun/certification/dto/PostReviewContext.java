package com.deundeun.certification.dto;

import com.deundeun.certification.enums.CertificationStatus;
import lombok.Data;

// [조회 결과] 승인/반려 판단에 필요한 정보 (상태 + 방장)
@Data
public class PostReviewContext {
    private Long postId;
    private CertificationStatus status;
    private Long hostId;     // 챌린지 방장
    private Long authorId;   // 글 작성자
}
