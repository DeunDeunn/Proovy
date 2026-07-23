package com.deundeun.certification.service;

import com.deundeun.certification.mapper.HostWarningMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

// 자정(KST) 미검수 경고 배치의 실제 처리.
// PENDING 게시물의 상태는 변경하지 않고, 미검수 챌린지 방장에게만 경고를 적립한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class PendingReviewWarningService {

    private final HostWarningMapper hostWarningMapper;
    private final PendingReviewWarningProcessor pendingReviewWarningProcessor;

    public void warnForPendingReviews() {
        List<Long> pendingHostIds = hostWarningMapper.findPendingReviewHostIds();
        if (pendingHostIds.isEmpty()) {
            log.info("미검수 경고 - 대상 없음");
            return;
        }

        int warningCount = 0;
        int successCount = 0;
        for (Long hostId : pendingHostIds) {
            try {
                warningCount += pendingReviewWarningProcessor.processHost(hostId);
                successCount++;
            } catch (Exception e) {
                log.error("미검수 경고 처리 실패 - userId={}", hostId, e);
            }
        }

        log.info("미검수 경고 완료 - 경고 {}건, 성공 방장 {}명, 실패 방장 {}명",
                warningCount, successCount, pendingHostIds.size() - successCount);
    }
}
