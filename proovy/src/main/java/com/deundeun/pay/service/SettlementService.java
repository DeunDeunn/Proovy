package com.deundeun.pay.service;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.pay.domain.CashTransaction;
import com.deundeun.pay.enums.CashTransactionType;
import com.deundeun.pay.domain.HostRevenue;
import com.deundeun.pay.domain.Settlement;
import com.deundeun.pay.domain.Wallet;
import com.deundeun.pay.dto.HostRevenueHistoryResponse;
import com.deundeun.pay.dto.HostRevenueItem;
import com.deundeun.pay.dto.SettlementResultResponse;
import com.deundeun.pay.enums.CashTransactionStatus;
import com.deundeun.pay.enums.HostRevenueStatus;
import com.deundeun.pay.enums.SettlementStatus;
import com.deundeun.pay.mapper.CashTransactionMapper;
import com.deundeun.pay.mapper.HostRevenueMapper;
import com.deundeun.pay.mapper.SettlementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import static com.deundeun.global.exception.ErrorCode.SETTLEMENT_ALREADY_PROCESSED;

@Service
@RequiredArgsConstructor
public class SettlementService implements WalletSettlementService {
    private final SettlementMapper settlementMapper;
    private final HostRevenueMapper hostRevenueMapper;
    private final WalletService walletService;
    private final CashTransactionMapper cashTransactionMapper;
    @Override
    @Transactional
    public Long settle(Long challengeId, List<Long> successUserIds, List<Long> failUserIds, Long hostId, boolean isHostDisqualified, long perPersonFee) {
        if(settlementMapper.existsByChallengeId(challengeId)){
            throw new ApiException(SETTLEMENT_ALREADY_PROCESSED);
        }
        // 호출자가 같은 유저를 리스트에 중복으로 넘기면 아래 for문에서 잠금 잔액 차감/수익 지급/영구 손실 반영이
        // 그 유저에 대해 두 번 일어나 버리므로(charge_lots는 released_at 가드로 막아도, locked_reward_balance나
        // charged_balance/reward_balance 자체는 그대로 두 번 깎이거나 지급됨), 입력 단계에서 중복을 제거한다.
        successUserIds = successUserIds.stream().distinct().toList();
        failUserIds = failUserIds.stream().distinct().toList();
        BigDecimal participantShareRate;
        BigDecimal platformFeeRate;
        BigDecimal hostFeeRate;

        if (successUserIds.isEmpty() && isHostDisqualified) {
            participantShareRate = BigDecimal.valueOf(0.0);
            platformFeeRate = BigDecimal.valueOf(1.0);
            hostFeeRate = BigDecimal.valueOf(0.0);
        } else if (successUserIds.isEmpty()) {
            participantShareRate = BigDecimal.valueOf(0.0);
            platformFeeRate = BigDecimal.valueOf(0.9);
            hostFeeRate = BigDecimal.valueOf(0.1);
        } else if (isHostDisqualified) {
            participantShareRate = BigDecimal.valueOf(0.7);
            platformFeeRate = BigDecimal.valueOf(0.3);
            hostFeeRate = BigDecimal.valueOf(0.0);
        } else {
            participantShareRate = BigDecimal.valueOf(0.7);
            platformFeeRate = BigDecimal.valueOf(0.2);
            hostFeeRate = BigDecimal.valueOf(0.1);
        }
        long failurePool = failUserIds.size() * perPersonFee;
        long participantShareAmount = returnAmount(failurePool, participantShareRate);
        long hostFeeAmount = returnAmount(failurePool, hostFeeRate);
        long platformFeeAmount = failurePool - participantShareAmount - hostFeeAmount;

        // 성공자 0명이면 나눠줄 사람이 없으니 0으로 두고, 0으로 나누기(ArithmeticException) 자체를 피한다.
        long profitPerUser = successUserIds.isEmpty()
                ? 0L
                : participantShareAmount / successUserIds.size();
        long roundingRemainder = participantShareAmount - (profitPerUser * successUserIds.size());
        platformFeeAmount += roundingRemainder; // n빵 잔돈은 플랫폼 몫에 합산

        Settlement settlement = Settlement.builder()
                .challengeId(challengeId)
                .perPersonFee(perPersonFee)
                .totalParticipantCount(successUserIds.size() + failUserIds.size())
                .successUserCount(successUserIds.size())
                .failUserCount(failUserIds.size())
                .isHostDisqualified(isHostDisqualified)
                .participantShareRate(participantShareRate)
                .platformFeeRate(platformFeeRate)
                .hostFeeRate(hostFeeRate)
                .failurePool(failurePool)
                .participantShareAmount(participantShareAmount)
                .hostFeeAmount(hostFeeAmount)
                .platformFeeAmount(platformFeeAmount)
                .profitPerUser(profitPerUser)
                .roundingRemainder(roundingRemainder)
                .status(SettlementStatus.COMPLETED)
                .settledAt(LocalDateTime.now())
                .build();
        try {
            settlementMapper.insert(settlement);
        } catch (DataIntegrityViolationException e) {
            // existsByChallengeId 체크와 실제 insert 사이의 레이스로 유니크 제약(challenge_id)에 걸린 경우.
            // DuplicateKeyException도 DataIntegrityViolationException의 하위 타입이라 여기서 같이 잡힌다.
            // 다른 종류의 DB 오류(DataAccessException)는 그대로 던져서 기존 DATABASE_ERROR 처리로 흘러가게 둔다.
            throw new ApiException(SETTLEMENT_ALREADY_PROCESSED);
        }
        if (!isHostDisqualified) {
            Wallet hostWallet = walletService.getWalletForUpdate(hostId);
            walletService.updateRewardBalance(hostWallet.getId(), hostWallet.getRewardBalance() + hostFeeAmount);

            cashTransactionMapper.insert(CashTransaction.builder()
                    .walletId(hostWallet.getId())
                    .type(CashTransactionType.HOST_FEE)
                    .amount(hostFeeAmount)
                    .balanceAfter(hostWallet.getAvailableBalance() + hostFeeAmount)
                    .status(CashTransactionStatus.COMPLETED)
                    .referenceId(challengeId)
                    .build());

            hostRevenueMapper.insert(HostRevenue.builder()
                    .hostId(hostId)
                    .challengeId(challengeId)
                    .settlementId(settlement.getId())
                    .amount(hostFeeAmount)
                    .status(HostRevenueStatus.PAID)
                    .paidAt(LocalDateTime.now())
                    .build());
        }
        for (Long userId : successUserIds) {
            Wallet wallet = walletService.getWalletForUpdate(userId);
            long chargedPortion = walletService.releaseChargeLotsFifo(wallet.getId(), challengeId); // charge_lots 복구 + charged 몫 확인
            long rewardPortion = perPersonFee - chargedPortion;
            walletService.updateLockedChargedBalance(wallet.getId(), wallet.getLockedChargedBalance() - chargedPortion);
            walletService.updateLockedRewardBalance(wallet.getId(), wallet.getLockedRewardBalance() - rewardPortion);
            walletService.updateRewardBalance(wallet.getId(), wallet.getRewardBalance() + profitPerUser);  // 수익 지급
            cashTransactionMapper.insert(CashTransaction.builder()
                    .walletId(wallet.getId())
                    .type(CashTransactionType.CHALLENGE_PRINCIPAL_SUCCESS)
                    .amount(perPersonFee)
                    .balanceAfter(wallet.getAvailableBalance() + perPersonFee)  // 홀딩 해제로 늘어난 사용가능잔액
                    .status(CashTransactionStatus.COMPLETED)
                    .referenceId(challengeId)
                    .build());

            cashTransactionMapper.insert(CashTransaction.builder()
                    .walletId(wallet.getId())
                    .type(CashTransactionType.CHALLENGE_PROFIT_DISTRIBUTION)
                    .amount(profitPerUser)
                    .balanceAfter(wallet.getAvailableBalance() + perPersonFee + profitPerUser)  // 위 + 수익까지 반영된 값
                    .status(CashTransactionStatus.COMPLETED)
                    .referenceId(challengeId)
                    .build());
        }
        for (Long userId : failUserIds) {
            Wallet wallet = walletService.getWalletForUpdate(userId);
            // charge_lots는 홀딩 시점에 이미 깎인 채로 둬야 맞음(돈이 진짜 사라짐) -> 복구하지 않고 charged 몫만 조회
            long chargedPortion = walletService.sumChargeLotAllocations(wallet.getId(), challengeId);
            long rewardPortion = perPersonFee - chargedPortion;

            walletService.updateLockedChargedBalance(wallet.getId(), wallet.getLockedChargedBalance() - chargedPortion); // 홀딩 해제
            walletService.updateLockedRewardBalance(wallet.getId(), wallet.getLockedRewardBalance() - rewardPortion);   // 홀딩 해제
            walletService.updateChargedBalance(wallet.getId(), wallet.getChargedBalance() - chargedPortion); // 진짜 출금(영구 손실, charged 몫만)
            walletService.updateRewardBalance(wallet.getId(), wallet.getRewardBalance() - rewardPortion);   // 진짜 출금(영구 손실, reward 몫만)

            if (chargedPortion > 0) {
                cashTransactionMapper.insert(CashTransaction.builder()
                        .walletId(wallet.getId())
                        .type(CashTransactionType.CHALLENGE_PRINCIPAL_FAIL)
                        .amount(chargedPortion)
                        .balanceAfter(wallet.getChargedBalance() - chargedPortion)  // 실제로 줄어든 충전잔액 (charged 몫만)
                        .status(CashTransactionStatus.COMPLETED)
                        .referenceId(challengeId)
                        .build());
            }

            if (rewardPortion > 0) {
                cashTransactionMapper.insert(CashTransaction.builder()
                        .walletId(wallet.getId())
                        .type(CashTransactionType.CHALLENGE_PRINCIPAL_FAIL)
                        .amount(rewardPortion)
                        .balanceAfter(wallet.getRewardBalance() - rewardPortion)  // 실제로 줄어든 리워드잔액 (reward 몫만)
                        .status(CashTransactionStatus.COMPLETED)
                        .referenceId(challengeId)
                        .build());
            }
        }
        return settlement.getId();
    }

    private long returnAmount(Long failurePool, BigDecimal rate){
        return BigDecimal.valueOf(failurePool)            // long → BigDecimal 변환
                .multiply(rate)                           // 비율 곱하기
                .setScale(0, RoundingMode.FLOOR) // 소수점 버림
                .longValueExact();
    }

    @Override
    public SettlementResultResponse getSettlementResult(Long challengeId, Long requesterId) {
        SettlementResultResponse result = settlementMapper.selectByChallengeId(challengeId);
        if (result == null) {
            throw new ApiException(ErrorCode.SETTLEMENT_NOT_FOUND);
        }

        Wallet requesterWallet = walletService.getOrCreateWallet(requesterId);
        boolean isParticipant = cashTransactionMapper.existsSettlementParticipation(requesterWallet.getId(), challengeId);
        boolean isHost = hostRevenueMapper.existsByChallengeIdAndHostId(challengeId, requesterId);
        if (!isParticipant && !isHost) {
            // 자격 박탈된 방장은 host_revenues에 기록이 없어 여기서도 걸러지는데,
            // 자격 박탈 시 방장 몫 자체가 없으니 정산 결과를 볼 자격도 없는 게 맞아 의도된 동작이다.
            throw new ApiException(ErrorCode.SETTLEMENT_NOT_FOUND);
        }
        return result;
    }

    public HostRevenueItem getHostRevenueByChallengeId(Long challengeId, Long requesterId) {
        HostRevenueItem result = hostRevenueMapper.selectByChallengeId(challengeId);
        if (result == null) {
            throw new ApiException(ErrorCode.HOST_REVENUE_NOT_FOUND);
        }
        if (!hostRevenueMapper.existsByChallengeIdAndHostId(challengeId, requesterId)) {
            throw new ApiException(ErrorCode.HOST_REVENUE_NOT_FOUND);
        }
        return result;
    }

    @Transactional
    public HostRevenueHistoryResponse getHostRevenueHistory(Long hostId, int page, int size) {
        List<HostRevenueItem> content = hostRevenueMapper.selectByHostId(hostId, page * size, size);
        long totalElements = hostRevenueMapper.countByHostId(hostId);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return HostRevenueHistoryResponse.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }
}
