package com.deundeun.pay.service;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import com.deundeun.pay.client.NaverPayApiClient;
import com.deundeun.pay.config.NaverPayProperties;
import com.deundeun.pay.domain.CashTransaction;
import com.deundeun.pay.enums.CashTransactionStatus;
import com.deundeun.pay.enums.CashTransactionType;
import com.deundeun.pay.domain.ChargeLot;
import com.deundeun.pay.domain.Wallet;
import com.deundeun.pay.dto.ChargeResponse;
import com.deundeun.pay.dto.NaverPayCallbackRequest;
import com.deundeun.pay.dto.NaverPayCallbackResponse;
import com.deundeun.pay.dto.naverpay.NaverPayApplyBody;
import com.deundeun.pay.dto.naverpay.NaverPayPaymentDetail;
import com.deundeun.pay.mapper.CashTransactionMapper;
import com.deundeun.pay.mapper.ChargeLotMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChargeService {

    private static final long MIN_CHARGE_AMOUNT = 1_000L;
    private static final long MAX_CHARGE_AMOUNT = 50_000L;
    private static final long CHARGE_UNIT = 1_000L;
    private static final String MERCHANT_PAY_KEY_PREFIX = "CHG-";
    private static final String CHARGE_PRODUCT_NAME = "프루비 캐시 충전";

    private final WalletService walletService;
    private final CashTransactionMapper cashTransactionMapper;
    private final ChargeLotMapper chargeLotMapper;
    private final NaverPayProperties naverPayProperties;
    private final NaverPayApiClient naverPayApiClient;

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
     * 콜백이 중복으로 들어와도(FOR UPDATE로 거래 row를 잠가) PENDING 확인과 충전 처리가
     * 한 요청씩만 통과하도록 보장한다.
     */
    @Transactional
    public NaverPayCallbackResponse handlePaymentCompleted(NaverPayCallbackRequest callback) {
        //MerchantPayKey에서 transactionId 추출
        Long transactionId = parseTransactionId(callback.getMerchantPayKey());
        CashTransaction transaction = cashTransactionMapper.selectByIdForUpdate(transactionId);
        if (transaction == null) {
            throw new ApiException(ErrorCode.CHARGE_TRANSACTION_NOT_FOUND);
        }

        if (transaction.getStatus() != CashTransactionStatus.PENDING) {
            log.info("이미 처리된 충전 - transactionId={}, status={}", transactionId, transaction.getStatus());
            return NaverPayCallbackResponse.builder()
                    .chargeTransactionId(transactionId)
                    .status(transaction.getStatus())
                    .build();
        }

        NaverPayApplyBody applyResult = naverPayApiClient.applyPayment(callback.getPaymentId());
        NaverPayPaymentDetail detail = applyResult.detail();

        if (detail == null || !callback.getMerchantPayKey().equals(detail.merchantPayKey())) {
            cashTransactionMapper.updateStatus(transactionId, CashTransactionStatus.FAILED);
            throw new ApiException(ErrorCode.PG_AMOUNT_MISMATCH, "merchantPayKey가 일치하지 않습니다.");
        }

        if (!detail.isAdmitted()) {
            cashTransactionMapper.updateStatus(transactionId, CashTransactionStatus.FAILED);
            return NaverPayCallbackResponse.builder()
                    .chargeTransactionId(transactionId)
                    .status(CashTransactionStatus.FAILED)
                    .build();
        }

        if (!transaction.getAmount().equals(detail.totalPayAmount())) {
            cashTransactionMapper.updateStatus(transactionId, CashTransactionStatus.FAILED);
            throw new ApiException(ErrorCode.PG_AMOUNT_MISMATCH);
        }

        Wallet wallet = walletService.getWalletByIdForUpdate(transaction.getWalletId());
        long newChargedBalance = wallet.getChargedBalance() + transaction.getAmount();
        walletService.updateChargedBalance(wallet.getId(), newChargedBalance);

        ChargeLot chargeLot = ChargeLot.builder()
                .walletId(wallet.getId())
                .amount(transaction.getAmount())
                .remainingAmount(transaction.getAmount())
                .build();
        chargeLotMapper.insert(chargeLot);

        cashTransactionMapper.completeCharge(transactionId, detail.paymentId(), newChargedBalance);

        return NaverPayCallbackResponse.builder()
                .chargeTransactionId(transactionId)
                .status(CashTransactionStatus.COMPLETED)
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
