package com.deundeun.ai.dto;

import lombok.Getter;

@Getter
public class AiReviewContext {

    private Long verificationPostId;
    private Long challengeId;
    private Long hostId;
    private Long authorId;
    private String challengeTitle;
    private String verificationMethod;
    private String previousPostStatus;
    private String postContent;
    private String thumbnailUrl;
}
