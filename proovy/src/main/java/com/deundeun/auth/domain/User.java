package com.deundeun.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private Long id;
    private String provider;
    private String providerId;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime suspendedFrom;
    private LocalDateTime suspendedUntil;
    private LocalDateTime demotedAt;
}
