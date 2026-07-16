package com.deundeun.pay.service;

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

/**
 * PG 승인(applyPayment)은 성공했는데 그 직후 우리 쪽 DB 반영이 실패해서 PROCESSING에
 * 멈춘 충전 건을 주기적으로 찾아 복구한다. applyPayment는 paymentId 기반 고정 멱등키를
 * 쓰므로 재호출해도 안전하고, 그 응답을 기준으로 완료/실패를 마저 반영한다.
 * (단일 인스턴스 운영을 가정 - 여러 인스턴스가 동시에 도는 경우의 중복 처리 방지는 아직 없음)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChargeReconciliationScheduler {

    private static final long STUCK_THRESHOLD_SECONDS = 30;

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
        try {
            NaverPayApplyBody applyResult = naverPayApiClient.applyPayment(transaction.getPgTransactionId());
            NaverPayPaymentDetail detail = applyResult.detail();
            String expectedMerchantPayKey = ChargeService.MERCHANT_PAY_KEY_PREFIX + transaction.getId();

            if (detail == null || !expectedMerchantPayKey.equals(detail.merchantPayKey()) || !detail.isAdmitted()) {
                log.warn("충전 보정 실패 처리 - transactionId={}", transaction.getId());
                chargeTransactionStateService.markFailed(transaction.getId());
                return;
            }

            if (!transaction.getAmount().equals(detail.totalPayAmount())) {
                log.warn("충전 보정 중 금액 불일치 - transactionId={}", transaction.getId());
                chargeTransactionStateService.markFailed(transaction.getId());
                return;
            }

            chargeTransactionStateService.completeCharge(transaction, detail);
            log.info("충전 보정 완료 - transactionId={}", transaction.getId());
        } catch (Exception e) {
            log.error("충전 보정 중 예외 발생, 다음 주기에 재시도됨 - transactionId={}", transaction.getId(), e);
        }
    }
}
