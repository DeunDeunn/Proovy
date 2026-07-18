package com.deundeun.pay.service;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.pay.client.NaverPayApiClient;
import com.deundeun.pay.config.NaverPayProperties;
import com.deundeun.pay.domain.CashTransaction;
import com.deundeun.pay.enums.CashTransactionStatus;
import com.deundeun.pay.enums.CashTransactionType;
import com.deundeun.pay.domain.Wallet;
import com.deundeun.pay.dto.ChargeResponse;
import com.deundeun.pay.dto.NaverPayCallbackRequest;
import com.deundeun.pay.dto.NaverPayCallbackResponse;
import com.deundeun.pay.dto.naverpay.NaverPayApplyBody;
import com.deundeun.pay.dto.naverpay.NaverPayPaymentDetail;
import com.deundeun.pay.mapper.CashTransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChargeService {

    private static final long MIN_CHARGE_AMOUNT = 1_000L;
    private static final long MAX_CHARGE_AMOUNT = 50_000L;
    private static final long CHARGE_UNIT = 1_000L;
    static final String MERCHANT_PAY_KEY_PREFIX = "CHG-";
    private static final String CHARGE_PRODUCT_NAME = "프루비 캐시 충전";

    private final WalletService walletService;
    private final CashTransactionMapper cashTransactionMapper;
    private final NaverPayProperties naverPayProperties;
    private final NaverPayApiClient naverPayApiClient;
    private final ChargeTransactionStateService chargeTransactionStateService;

    /**
     * 충전 요청. PENDING 상태의 거래를 먼저 만들어두고, 프론트가 네이버페이 JS SDK
     * (oPay.open())를 호출할 때 그대로 넘길 파라미터를 돌려준다.
     * 백엔드가 별도로 "예약 API"를 호출할 필요는 없다 (SDK가 클라이언트에서 처리).
     * 실제 잔액 반영은 결제 완료 후 승인 처리(handlePaymentCompleted)에서 이루어진다.
     */
    @Transactional
    public ChargeResponse requestCharge(Long userId, long amount) {
        validateChargeAmount(amount);
        //지갑 조회(없을 시 생성)
        Wallet wallet = walletService.getOrCreateWallet(userId);

        CashTransaction transaction = CashTransaction.builder()
                .walletId(wallet.getId())
                .type(CashTransactionType.CHARGE)
                .amount(amount)
                .balanceAfter(wallet.getChargedBalance())
                .status(CashTransactionStatus.PENDING)
                .build();
        cashTransactionMapper.insert(transaction);

        String merchantPayKey = MERCHANT_PAY_KEY_PREFIX + transaction.getId();

        return ChargeResponse.builder()
                .chargeTransactionId(transaction.getId())
                .merchantPayKey(merchantPayKey)
                .productName(CHARGE_PRODUCT_NAME)
                .productCount(1)
                .totalPayAmount(amount)
                .taxScopeAmount(amount)
                .taxExScopeAmount(0L)
                .returnUrl(naverPayProperties.returnUrl())
                .status(CashTransactionStatus.PENDING)
                .build();
    }

    /**
     * 결제 팝업 완료 후 returnUrl로 돌아온 프론트가 호출. paymentId로 네이버페이 서버에
     * 직접 승인을 요청해서 결과를 확인하고(클라이언트가 보낸 값은 신뢰하지 않음), 그 응답
     * 기준으로만 잔액을 반영한다. merchantPayKey는 내 거래를 먼저 찾아 이미 처리됐는지
     * 확인하는 용도로만 쓰고, 최종 확인은 네이버페이 응답의 detail.merchantPayKey로 한다.
     * PG 호출(applyPayment) 동안 DB 락/커넥션을 붙잡지 않도록, PENDING→PROCESSING 전환과
     * 완료 반영을 각각 별도의 짧은 트랜잭션으로 나누고 그 사이에서만 PG를 호출한다.
     * 중복 콜백은 PENDING→PROCESSING 조건부 UPDATE가 한 요청에서만 성공하는 것으로 막는다.
     */
    public NaverPayCallbackResponse handlePaymentCompleted(NaverPayCallbackRequest callback) {
        //MerchantPayKey에서 transactionId 추출
        Long transactionId = parseTransactionId(callback.getMerchantPayKey());

        Optional<Long> claim = chargeTransactionStateService.beginProcessing(transactionId, callback.getPaymentId());
        if (claim.isEmpty()) {
            return currentStatusResponse(transactionId);
        }
        long processingToken = claim.get();

        CashTransaction transaction = cashTransactionMapper.selectById(transactionId);

        NaverPayApplyBody applyResult = naverPayApiClient.applyPayment(callback.getPaymentId());
        NaverPayPaymentDetail detail = applyResult.detail();

        if (detail == null || !callback.getMerchantPayKey().equals(detail.merchantPayKey())) {
            if (!chargeTransactionStateService.markFailed(transactionId, processingToken)) {
                return currentStatusResponse(transactionId);
            }
            throw new ApiException(ErrorCode.PG_AMOUNT_MISMATCH, "merchantPayKey가 일치하지 않습니다.");
        }

        if (!detail.isAdmitted()) {
            if (!chargeTransactionStateService.markFailed(transactionId, processingToken)) {
                return currentStatusResponse(transactionId);
            }
            return NaverPayCallbackResponse.builder()
                    .chargeTransactionId(transactionId)
                    .status(CashTransactionStatus.FAILED)
                    .build();
        }

        if (!transaction.getAmount().equals(detail.totalPayAmount())) {
            if (!chargeTransactionStateService.markFailed(transactionId, processingToken)) {
                return currentStatusResponse(transactionId);
            }
            throw new ApiException(ErrorCode.PG_AMOUNT_MISMATCH);
        }

        if (!chargeTransactionStateService.completeCharge(transaction, detail, processingToken)) {
            return currentStatusResponse(transactionId);
        }

        return NaverPayCallbackResponse.builder()
                .chargeTransactionId(transactionId)
                .status(CashTransactionStatus.COMPLETED)
                .build();
    }

    /**
     * beginProcessing/markFailed/completeCharge가 토큰 불일치로 거부됐을 때(이미 보정
     * 스케줄러 등 다른 쪽이 이 거래를 다른 결과로 확정한 뒤일 수 있음) 호출자가 그 결과를
     * 그대로 응답하지 않고, 실제 현재 상태를 다시 조회해서 돌려주기 위한 헬퍼.
     */
    private NaverPayCallbackResponse currentStatusResponse(Long transactionId) {
        CashTransaction existing = cashTransactionMapper.selectById(transactionId);
        if (existing == null) {
            throw new ApiException(ErrorCode.CHARGE_TRANSACTION_NOT_FOUND);
        }
        log.info("이미 처리(중)인 충전 - transactionId={}, status={}", transactionId, existing.getStatus());
        return NaverPayCallbackResponse.builder()
                .chargeTransactionId(transactionId)
                .status(existing.getStatus())
                .build();
    }

    private void validateChargeAmount(long amount) {
        if (amount < MIN_CHARGE_AMOUNT || amount > MAX_CHARGE_AMOUNT || amount % CHARGE_UNIT != 0) {
            throw new ApiException(ErrorCode.INVALID_CHARGE_AMOUNT);
        }
    }

    private Long parseTransactionId(String merchantPayKey) {
        if (merchantPayKey == null || !merchantPayKey.startsWith(MERCHANT_PAY_KEY_PREFIX)) {
            throw new ApiException(ErrorCode.CHARGE_TRANSACTION_NOT_FOUND);
        }
        try {
            return Long.parseLong(merchantPayKey.substring(MERCHANT_PAY_KEY_PREFIX.length()));
        } catch (NumberFormatException e) {
            throw new ApiException(ErrorCode.CHARGE_TRANSACTION_NOT_FOUND);
        }
    }
}
