package com.deundeun.pay.service;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.pay.client.NaverPayApiClient;
import com.deundeun.pay.domain.CashTransaction;
import com.deundeun.pay.dto.naverpay.NaverPayApplyBody;
import com.deundeun.pay.dto.naverpay.NaverPayPaymentDetail;
import com.deundeun.pay.mapper.CashTransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * PG 승인(applyPayment)은 성공했는데 그 직후 우리 쪽 DB 반영이 실패해서 PROCESSING에
 * 멈춘 충전 건을 주기적으로 찾아 복구한다. applyPayment는 paymentId 기반 고정 멱등키를
 * 쓰므로 재호출해도 안전하고, 그 응답을 기준으로 완료/실패를 마저 반영한다.
 *
 * applyPayment의 readTimeout(60초)이 이 스케줄러의 정지 판단 기준(30초)보다 길어서, 원래
 * 콜백 요청이 아직 정상적으로 PG 응답을 기다리는 중일 수도 있다. 그래서 보정을 시도하기 전에
 * claimForReconciliation으로 processing_token(lease)을 먼저 인수해야 하고, 그 사이 원래
 * 요청이 이미 끝냈다면(토큰이 이미 바뀌었다면) 조용히 넘어간다.
 * (단일 인스턴스 운영을 가정 - 여러 인스턴스가 동시에 도는 경우의 중복 처리 방지는 아직 없음)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChargeReconciliationScheduler {

    private static final long STUCK_THRESHOLD_SECONDS = 30;

    // PG가 이 거래 자체에 대해 이미 최종 판정을 내린 코드 - 재시도해도 판정이 바뀌지 않으므로 즉시 실패 처리한다.
    private static final Set<ErrorCode> TERMINAL_FAILURE_CODES = Set.of(
            ErrorCode.PG_TIME_EXPIRED,
            ErrorCode.PG_INSUFFICIENT_BALANCE,
            ErrorCode.PG_OWNER_AUTH_FAIL
    );

    private final CashTransactionMapper cashTransactionMapper;
    private final NaverPayApiClient naverPayApiClient;
    private final ChargeTransactionStateService chargeTransactionStateService;

    @Scheduled(fixedDelay = 30_000)
    public void reconcileStuckCharges() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(STUCK_THRESHOLD_SECONDS);
        List<CashTransaction> stuck = cashTransactionMapper.selectStuckProcessing(threshold);

        for (CashTransaction transaction : stuck) {
            reconcile(transaction);
        }
    }

    private void reconcile(CashTransaction transaction) {
        if (transaction.getProcessingToken() == null) {
            return;
        }

        Optional<Long> lease = chargeTransactionStateService.claimForReconciliation(
                transaction.getId(), transaction.getProcessingToken());
        if (lease.isEmpty()) {
            // 원래 콜백 요청이 그 사이 이미 끝냈다는 뜻 - 보정 시도할 필요 없음
            return;
        }
        long processingToken = lease.get();

        try {
            NaverPayApplyBody applyResult = naverPayApiClient.applyPayment(transaction.getPgTransactionId());
            NaverPayPaymentDetail detail = applyResult.detail();
            String expectedMerchantPayKey = ChargeService.MERCHANT_PAY_KEY_PREFIX + transaction.getId();

            if (detail == null || !expectedMerchantPayKey.equals(detail.merchantPayKey()) || !detail.isAdmitted()) {
                log.warn("충전 보정 실패 처리 - transactionId={}", transaction.getId());
                chargeTransactionStateService.markFailed(transaction.getId(), processingToken);
                return;
            }

            if (!transaction.getAmount().equals(detail.totalPayAmount())) {
                log.warn("충전 보정 중 금액 불일치 - transactionId={}", transaction.getId());
                chargeTransactionStateService.markFailed(transaction.getId(), processingToken);
                return;
            }

            chargeTransactionStateService.completeCharge(transaction, detail, processingToken);
            log.info("충전 보정 완료 - transactionId={}", transaction.getId());
        } catch (ApiException e) {
            if (TERMINAL_FAILURE_CODES.contains(e.getErrorCode())) {
                log.warn("충전 보정 영구 실패로 종료 처리 - transactionId={}", transaction.getId(), e);
                chargeTransactionStateService.markFailed(transaction.getId(), processingToken);
            } else {
                log.error("충전 보정 중 예외 발생, 다음 주기에 재시도됨 - transactionId={}", transaction.getId(), e);
            }
        } catch (Exception e) {
            log.error("충전 보정 중 예외 발생, 다음 주기에 재시도됨 - transactionId={}", transaction.getId(), e);
        }
    }
}
