package com.deundeun.challenge.service;

import com.deundeun.challenge.domain.CertFrequency;
import com.deundeun.challenge.domain.Challenge;
import com.deundeun.challenge.domain.ChallengeStatus;
import com.deundeun.challenge.dto.request.ChallengeCreateRequest;
import com.deundeun.challenge.dto.request.ChallengeSearchCondition;
import com.deundeun.challenge.dto.response.ChallengeCreateResponse;
import com.deundeun.challenge.dto.response.ChallengeDetailResponse;
import com.deundeun.challenge.dto.response.ChallengeSummaryResponse;
import com.deundeun.challenge.dto.response.PageResponse;
import com.deundeun.challenge.mapper.CategoryMapper;
import com.deundeun.challenge.mapper.ChallengeMapper;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeMapper  challengeMapper;
    private final CategoryMapper  categoryMapper;

    @Transactional
    public ChallengeCreateResponse create(Long hostId, ChallengeCreateRequest request) {
        // 규칙검증
        if (!request.endDate().isAfter(request.startDate())) {
            throw new ApiException(ErrorCode.INVALID_CHALLENGE_PERIOD);
        }
        if (!request.certEndTime().isAfter(request.certStartTime())) {
            throw new ApiException(ErrorCode.INVALID_CERT_TIME_RANGE);
        }
        if (!categoryMapper.existsById(request.categoryId())) {
            throw new ApiException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        Challenge challenge = Challenge.builder()
                .hostId(hostId)                                    // 개설자 = 방장
                .title(request.title())
                .description(request.description())
                .categoryId(request.categoryId())
                .entryFee(request.entryFee())
                .verificationMethod(request.verificationMethod())
                .certFrequency(CertFrequency.DAILY)                // MVP 고정
                .dailyCertLimit(request.dailyCertLimit())
                .successCriteriaRate(80)                           // 80% 고정
                .aiReviewEnabled(request.aiReviewEnabled())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .maxParticipants(request.maxParticipants())
                .status(ChallengeStatus.RECRUITING)                // 생성 시 무조건 모집중
                .certStartTime(request.certStartTime())
                .certEndTime(request.certEndTime())
                .feedVisibility(request.feedVisibility())
                .build();

        challengeMapper.insert(challenge);

        return ChallengeCreateResponse.from(challenge);
    }

    @Transactional(readOnly = true)
    public PageResponse<ChallengeSummaryResponse> search(ChallengeSearchCondition condition) {
        List<ChallengeSummaryResponse> content = challengeMapper.search(
                condition.categoryId(),
                condition.status(),
                condition.keyword(),
                condition.offset(),
                condition.size());

        long totalElements = challengeMapper.countBySearch(
                condition.categoryId(),
                condition.status(),
                condition.keyword());

        return PageResponse.of(content, condition.page(), condition.size(), totalElements);
    }

    @Transactional(readOnly = true)
    public ChallengeDetailResponse getDetail(Long challengeId) {
        ChallengeDetailResponse detail = challengeMapper.findDetailById(challengeId);
        if (detail == null) {
            throw new ApiException(ErrorCode.CHALLENGE_NOT_FOUND);
        }
        return detail;
    }

}
