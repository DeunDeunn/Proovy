package com.deundeun.pay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

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
import com.deundeun.pay.enums.HostRevenueStatus;
import com.deundeun.pay.mapper.CashTransactionMapper;
import com.deundeun.pay.mapper.HostRevenueMapper;
import com.deundeun.pay.mapper.SettlementMapper;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private SettlementMapper settlementMapper;
    @Mock
    private HostRevenueMapper hostRevenueMapper;
    @Mock
    private WalletService walletService;
    @Mock
    private CashTransactionMapper cashTransactionMapper;

    @InjectMocks
    private SettlementService settlementService;

    private Wallet walletWith(Long id, long charged, long reward, long lockedCharged) {
        return walletWith(id, charged, reward, lockedCharged, 0L);
    }

    private Wallet walletWith(Long id, long charged, long reward, long lockedCharged, long lockedReward) {
        return Wallet.builder().id(id).chargedBalance(charged).rewardBalance(reward)
                .lockedChargedBalance(lockedCharged).lockedRewardBalance(lockedReward).build();
    }

    private void stubInsertGeneratesId(SettlementMapper mapper, long generatedId) {
        doAnswer(invocation -> {
            Settlement arg = invocation.getArgument(0);
            arg.setId(generatedId);
            return null;
        }).when(mapper).insert(any(Settlement.class));
    }

    @Test
    void settle_alreadyProcessed_throwsAndDoesNothingElse() {
        when(settlementMapper.existsByChallengeId(1L)).thenReturn(true);

        assertThatThrownBy(() -> settlementService.settle(1L, List.of(10L), List.of(30L), 99L, false, 1_000L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_ALREADY_PROCESSED);

        verify(settlementMapper, never()).insert(any());
        verify(walletService, never()).getWalletForUpdate(anyLong());
        verify(cashTransactionMapper, never()).insert(any());
    }

    @Test
    void settle_duplicateKeyOnInsert_translatesToAlreadyProcessed() {
        // existsByChallengeId 체크 통과 후, insert 시점에 유니크 제약(레이스)에 걸린 경우를 시뮬레이션
        when(settlementMapper.existsByChallengeId(1L)).thenReturn(false);
        doThrow(new DuplicateKeyException("dup")).when(settlementMapper).insert(any(Settlement.class));

        assertThatThrownBy(() -> settlementService.settle(1L, List.of(10L), List.of(30L), 99L, false, 1_000L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_ALREADY_PROCESSED);

        verify(walletService, never()).getWalletForUpdate(anyLong());
        verify(cashTransactionMapper, never()).insert(any());
    }

    @Test
    void settle_normalCase_distributesPrincipalProfitAndHostFee() {
        Long challengeId = 1L;
        Long successA = 10L;
        Long successB = 20L;
        Long failUser = 30L;
        Long hostId = 99L;
        long perPersonFee = 1_000L; // failurePool = 1 * 1000 = 1000

        stubInsertGeneratesId(settlementMapper, 555L);

        when(walletService.getWalletForUpdate(successA)).thenReturn(walletWith(101L, 5_000L, 0L, 1_000L));
        when(walletService.getWalletForUpdate(successB)).thenReturn(walletWith(102L, 3_000L, 500L, 1_000L));
        when(walletService.getWalletForUpdate(failUser)).thenReturn(walletWith(103L, 2_000L, 0L, 1_000L));
        when(walletService.getWalletForUpdate(hostId)).thenReturn(walletWith(104L, 0L, 0L, 0L));
        // 세 명 다 홀딩 전액이 charged에서 왔다고 가정 (기존 테스트 시나리오와 동일)
        when(walletService.releaseChargeLotsFifo(101L, challengeId)).thenReturn(1_000L);
        when(walletService.releaseChargeLotsFifo(102L, challengeId)).thenReturn(1_000L);
        when(walletService.sumChargeLotAllocations(103L, challengeId)).thenReturn(1_000L);

        Long settlementId = settlementService.settle(
                challengeId, List.of(successA, successB), List.of(failUser), hostId, false, perPersonFee);

        assertThat(settlementId).isEqualTo(555L);

        // failurePool=1000 -> participantShare=700(0.7), hostFee=100(0.1), platform=나머지 200(0.2)
        // profitPerUser = floor(700/2) = 350, 잔돈 없음
        verify(walletService).updateRewardBalance(104L, 100L); // 방장 수수료
        verify(cashTransactionMapper).insert(argThat(t ->
                t.getWalletId().equals(104L) && t.getType() == CashTransactionType.HOST_FEE && t.getAmount() == 100L));
        verify(hostRevenueMapper).insert(argThat((HostRevenue hr) ->
                hr.getHostId().equals(hostId) && hr.getAmount() == 100L && hr.getStatus() == HostRevenueStatus.PAID));

        verify(walletService).updateLockedChargedBalance(101L, 0L);   // 1000 - 1000(charged 몫)
        verify(walletService).updateLockedRewardBalance(101L, 0L);    // 0 - 0(reward 몫)
        verify(walletService).updateRewardBalance(101L, 350L); // 0 + 350
        verify(walletService).updateLockedChargedBalance(102L, 0L);
        verify(walletService).updateLockedRewardBalance(102L, 0L);
        verify(walletService).updateRewardBalance(102L, 850L); // 500 + 350

        verify(walletService).updateLockedChargedBalance(103L, 0L);
        verify(walletService).updateLockedRewardBalance(103L, 0L);
        verify(walletService).updateChargedBalance(103L, 1_000L); // 2000 - 1000(charged 몫)
        verify(walletService).updateRewardBalance(103L, 0L); // reward 몫 0이라 그대로

        ArgumentCaptor<CashTransaction> captor = ArgumentCaptor.forClass(CashTransaction.class);
        verify(cashTransactionMapper, times(6)).insert(captor.capture());
        List<CashTransaction> recorded = captor.getAllValues();
        assertThat(recorded).extracting(CashTransaction::getType).containsExactlyInAnyOrder(
                CashTransactionType.HOST_FEE,
                CashTransactionType.CHALLENGE_PRINCIPAL_SUCCESS,
                CashTransactionType.CHALLENGE_PROFIT_DISTRIBUTION,
                CashTransactionType.CHALLENGE_PRINCIPAL_SUCCESS,
                CashTransactionType.CHALLENGE_PROFIT_DISTRIBUTION,
                CashTransactionType.CHALLENGE_PRINCIPAL_FAIL
        );

        // 실패자의 CHALLENGE_PRINCIPAL_FAIL 레코드 금액 합이 실제 손실액(perPersonFee)과 정확히 일치해야 한다 (중복 기록 방지 회귀 테스트)
        long failRecordedTotal = recorded.stream()
                .filter(t -> t.getType() == CashTransactionType.CHALLENGE_PRINCIPAL_FAIL)
                .mapToLong(CashTransaction::getAmount)
                .sum();
        assertThat(failRecordedTotal).isEqualTo(perPersonFee);

        ArgumentCaptor<Settlement> settlementCaptor = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementMapper).insert(settlementCaptor.capture());
        Settlement saved = settlementCaptor.getValue();
        assertThat(saved.getChallengeId()).isEqualTo(challengeId);
        assertThat(saved.getFailurePool()).isEqualTo(1_000L);
        assertThat(saved.getParticipantShareAmount()).isEqualTo(700L);
        assertThat(saved.getHostFeeAmount()).isEqualTo(100L);
        assertThat(saved.getPlatformFeeAmount()).isEqualTo(200L);
        assertThat(saved.getProfitPerUser()).isEqualTo(350L);
        assertThat(saved.getRoundingRemainder()).isEqualTo(0L);
    }

    @Test
    void settle_zeroSuccessUsers_avoidsDivisionByZeroAndGivesEverythingToPlatformAndHost() {
        stubInsertGeneratesId(settlementMapper, 1L);
        when(walletService.getWalletForUpdate(30L)).thenReturn(walletWith(103L, 2_000L, 0L, 1_000L));
        when(walletService.getWalletForUpdate(99L)).thenReturn(walletWith(104L, 0L, 0L, 0L));

        settlementService.settle(1L, List.of(), List.of(30L), 99L, false, 1_000L);

        // 성공자가 없으니 홀딩 해제/복구 대상 자체가 없음
        verify(walletService, never()).releaseChargeLotsFifo(anyLong(), anyLong());

        // failurePool=1000, 성공자 0명 -> participantShare 0%, platform 90%, host 10%
        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementMapper).insert(captor.capture());
        Settlement saved = captor.getValue();
        assertThat(saved.getParticipantShareAmount()).isZero();
        assertThat(saved.getProfitPerUser()).isZero(); // 0으로 나누기 없이 안전하게 0
        assertThat(saved.getHostFeeAmount()).isEqualTo(100L);
        assertThat(saved.getPlatformFeeAmount()).isEqualTo(900L);
    }

    @Test
    void settle_hostDisqualified_getsNoFeeAndNoHostRevenueRecord() {
        stubInsertGeneratesId(settlementMapper, 1L);
        when(walletService.getWalletForUpdate(10L)).thenReturn(walletWith(101L, 5_000L, 0L, 1_000L));
        when(walletService.getWalletForUpdate(30L)).thenReturn(walletWith(103L, 2_000L, 0L, 1_000L));

        settlementService.settle(1L, List.of(10L), List.of(30L), 99L, true, 1_000L);

        verify(walletService).releaseChargeLotsFifo(101L, 1L); // 성공자는 방장 자격 박탈 여부와 무관하게 복구됨

        // 방장 자격 박탈 -> host 지갑/거래/host_revenues 전부 안 건드림
        verify(walletService, never()).getWalletForUpdate(99L);
        verify(hostRevenueMapper, never()).insert(any());
        verify(cashTransactionMapper, never()).insert(argThat(t -> t.getType() == CashTransactionType.HOST_FEE));

        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementMapper).insert(captor.capture());
        assertThat(captor.getValue().getHostFeeAmount()).isZero();
        assertThat(captor.getValue().getPlatformFeeRate()).isEqualByComparingTo("0.3");
    }

    @Test
    void settle_threeWaySplit_neverLeaksAWon() {
        // perPersonFee=1,001 -> failurePool=1,001. participant=floor(700.7)=700, host=floor(100.1)=100.
        // platform을 독립적으로 floor(1001*0.2)=200으로 계산했다면 700+100+200=1000으로 1원이 샜을 상황.
        stubInsertGeneratesId(settlementMapper, 1L);
        when(walletService.getWalletForUpdate(10L)).thenReturn(walletWith(101L, 5_000L, 0L, 1_001L));
        when(walletService.getWalletForUpdate(30L)).thenReturn(walletWith(103L, 2_000L, 0L, 1_001L));
        when(walletService.getWalletForUpdate(99L)).thenReturn(walletWith(104L, 0L, 0L, 0L));

        settlementService.settle(1L, List.of(10L), List.of(30L), 99L, false, 1_001L);

        verify(walletService).releaseChargeLotsFifo(101L, 1L);

        ArgumentCaptor<Settlement> captor = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementMapper).insert(captor.capture());
        Settlement saved = captor.getValue();

        assertThat(saved.getParticipantShareAmount()).isEqualTo(700L);
        assertThat(saved.getHostFeeAmount()).isEqualTo(100L);
        assertThat(saved.getPlatformFeeAmount()).isEqualTo(201L); // 나머지 전부 -> 독립 계산(200)과 달리 1원 안 샘

        long total = saved.getParticipantShareAmount() + saved.getHostFeeAmount() + saved.getPlatformFeeAmount();
        assertThat(total).isEqualTo(saved.getFailurePool());
    }

    @Test
    void settle_failUserHeldPartlyFromReward_splitsLossAndDoesNotDoubleRecordTransactions() {
        // perPersonFee=1,000인데 그중 600만 charged에서, 400은 reward에서 홀딩된 상황
        Long failUser = 30L;
        long perPersonFee = 1_000L;
        stubInsertGeneratesId(settlementMapper, 1L);
        when(walletService.getWalletForUpdate(failUser)).thenReturn(walletWith(103L, 2_000L, 1_000L, 600L, 400L));
        when(walletService.sumChargeLotAllocations(103L, 1L)).thenReturn(600L);

        settlementService.settle(1L, List.of(), List.of(failUser), 99L, true, perPersonFee);

        verify(walletService).updateChargedBalance(103L, 1_400L); // 2,000 - 600(charged 몫만)
        verify(walletService).updateRewardBalance(103L, 600L);    // 1,000 - 400(reward 몫만)
        verify(walletService).updateLockedChargedBalance(103L, 0L); // 600 - 600
        verify(walletService).updateLockedRewardBalance(103L, 0L);  // 400 - 400

        ArgumentCaptor<CashTransaction> captor = ArgumentCaptor.forClass(CashTransaction.class);
        verify(cashTransactionMapper, times(2)).insert(captor.capture());
        List<CashTransaction> recorded = captor.getAllValues();
        assertThat(recorded).allMatch(t -> t.getType() == CashTransactionType.CHALLENGE_PRINCIPAL_FAIL);

        long totalRecorded = recorded.stream().mapToLong(CashTransaction::getAmount).sum();
        assertThat(totalRecorded).isEqualTo(perPersonFee); // 실제 손실액과 정확히 일치, 중복 기록 없음
    }

    @Test
    void getSettlementResult_requesterIsParticipant_returnsIt() {
        SettlementResultResponse response = SettlementResultResponse.builder().challengeId(1L).build();
        Long requesterId = 10L;
        when(settlementMapper.selectByChallengeId(1L)).thenReturn(response);
        when(walletService.getOrCreateWallet(requesterId)).thenReturn(walletWith(101L, 0L, 0L, 0L));
        when(cashTransactionMapper.existsSettlementParticipation(101L, 1L)).thenReturn(true);
        when(hostRevenueMapper.existsByChallengeIdAndHostId(1L, requesterId)).thenReturn(false);

        assertThat(settlementService.getSettlementResult(1L, requesterId)).isSameAs(response);
    }

    @Test
    void getSettlementResult_requesterIsHostOnly_returnsIt() {
        SettlementResultResponse response = SettlementResultResponse.builder().challengeId(1L).build();
        Long requesterId = 99L;
        when(settlementMapper.selectByChallengeId(1L)).thenReturn(response);
        when(walletService.getOrCreateWallet(requesterId)).thenReturn(walletWith(104L, 0L, 0L, 0L));
        when(cashTransactionMapper.existsSettlementParticipation(104L, 1L)).thenReturn(false);
        when(hostRevenueMapper.existsByChallengeIdAndHostId(1L, requesterId)).thenReturn(true);

        assertThat(settlementService.getSettlementResult(1L, requesterId)).isSameAs(response);
    }

    @Test
    void getSettlementResult_notFound_throwsSettlementNotFound() {
        when(settlementMapper.selectByChallengeId(1L)).thenReturn(null);

        assertThatThrownBy(() -> settlementService.getSettlementResult(1L, 10L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_NOT_FOUND);

        verify(walletService, never()).getOrCreateWallet(anyLong());
    }

    @Test
    void getSettlementResult_requesterNeitherParticipantNorHost_throwsNotFound() {
        SettlementResultResponse response = SettlementResultResponse.builder().challengeId(1L).build();
        Long requesterId = 77L;
        when(settlementMapper.selectByChallengeId(1L)).thenReturn(response);
        when(walletService.getOrCreateWallet(requesterId)).thenReturn(walletWith(105L, 0L, 0L, 0L));
        when(cashTransactionMapper.existsSettlementParticipation(105L, 1L)).thenReturn(false);
        when(hostRevenueMapper.existsByChallengeIdAndHostId(1L, requesterId)).thenReturn(false);

        assertThatThrownBy(() -> settlementService.getSettlementResult(1L, requesterId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_NOT_FOUND);
    }

    @Test
    void getHostRevenueByChallengeId_requesterIsHost_returnsIt() {
        HostRevenueItem item = HostRevenueItem.builder().challengeId(1L).amount(100L).build();
        Long hostId = 99L;
        when(hostRevenueMapper.selectByChallengeId(1L)).thenReturn(item);
        when(hostRevenueMapper.existsByChallengeIdAndHostId(1L, hostId)).thenReturn(true);

        assertThat(settlementService.getHostRevenueByChallengeId(1L, hostId)).isSameAs(item);
    }

    @Test
    void getHostRevenueByChallengeId_notFound_throwsHostRevenueNotFound() {
        when(hostRevenueMapper.selectByChallengeId(1L)).thenReturn(null);

        assertThatThrownBy(() -> settlementService.getHostRevenueByChallengeId(1L, 99L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.HOST_REVENUE_NOT_FOUND);

        verify(hostRevenueMapper, never()).existsByChallengeIdAndHostId(anyLong(), anyLong());
    }

    @Test
    void getHostRevenueByChallengeId_requesterIsNotHost_throwsHostRevenueNotFound() {
        HostRevenueItem item = HostRevenueItem.builder().challengeId(1L).amount(100L).build();
        Long notHostId = 77L;
        when(hostRevenueMapper.selectByChallengeId(1L)).thenReturn(item);
        when(hostRevenueMapper.existsByChallengeIdAndHostId(1L, notHostId)).thenReturn(false);

        assertThatThrownBy(() -> settlementService.getHostRevenueByChallengeId(1L, notHostId))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.HOST_REVENUE_NOT_FOUND);
    }

    @Test
    void getHostRevenueHistory_returnsPageWithCorrectTotalPages() {
        Long hostId = 99L;
        List<HostRevenueItem> content = List.of(
                HostRevenueItem.builder().id(2L).challengeId(20L).amount(100L).build(),
                HostRevenueItem.builder().id(1L).challengeId(10L).amount(200L).build());
        when(hostRevenueMapper.selectByHostId(hostId, 0, 10)).thenReturn(content);
        when(hostRevenueMapper.countByHostId(hostId)).thenReturn(15L);

        HostRevenueHistoryResponse response = settlementService.getHostRevenueHistory(hostId, 0, 10);

        assertThat(response.getContent()).isEqualTo(content);
        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(15L);
        assertThat(response.getTotalPages()).isEqualTo(2); // ceil(15/10)
    }
}
