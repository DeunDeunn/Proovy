package com.deundeun.chat.service.support;

import com.deundeun.certification.service.CertificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatCertificationShareValidator {

    private final CertificationService certificationService;

    public void validateShareable(Long postId, Long viewerId) {
        certificationService.getCertificationPostDetail(postId, viewerId);
    }
}
