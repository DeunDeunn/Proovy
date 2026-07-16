package com.deundeun.follow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Follow {
    private Long id;
    private Long followerId;
    private Long followingId;
    private LocalDateTime createdAt;
}
