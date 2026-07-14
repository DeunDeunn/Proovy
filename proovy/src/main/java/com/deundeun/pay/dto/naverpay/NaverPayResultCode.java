package com.deundeun.pay.dto.naverpay;

/**
 * 네이버페이 API 응답 code에 등장하는 알려진 값들.
 * enum이 아니라 상수인 이유: 승인/취소 API마다 코드 종류가 다르고, 문서에 없는
 * 새 코드가 응답으로 와도 파싱 자체는 깨지면 안 되기 때문 (String으로만 비교).
 */
public final class NaverPayResultCode {

    public static final String SUCCESS = "Success";
    public static final String INVALID_MERCHANT = "InvalidMerchant";

    // 승인(apply) API 전용 - NaverPayApiClient.mapResultCode에서 분기 처리
    public static final String TIME_EXPIRED = "TimeExpired";
    public static final String ALREADY_ON_GOING = "AlreadyOnGoing";
    public static final String ALREADY_COMPLETE = "AlreadyComplete";
    public static final String OWNER_AUTH_FAIL = "OwnerAuthFail";
    public static final String BANK_MAINTENANCE = "BankMaintenance";
    public static final String NOT_ENOUGH_ACCOUNT_BALANCE = "NotEnoughAccountBalance";
    public static final String MAINTENANCE_ONGOING = "MaintenanceOngoing";
    public static final String FAULT_CHECK_ONGOING = "FaultCheckOngoing";

    // 취소(cancel) API 아직 미구현 - 구현 시 사용
    // public static final String INVALID_PAYMENT_ID = "InvalidPaymentId";
    // public static final String ALREADY_CANCELED = "AlreadyCanceled";
    // public static final String OVER_REMAIN_AMOUNT = "OverRemainAmount";
    // public static final String PRE_CANCEL_NOT_COMPLETE = "PreCancelNotComplete";
    // public static final String CANCEL_DEADLINE_EXPIRED = "CancelDeadlineExpired";
    // public static final String CANCEL_NOT_COMPLETE = "CancelNotComplete";

    private NaverPayResultCode() {
    }
}
