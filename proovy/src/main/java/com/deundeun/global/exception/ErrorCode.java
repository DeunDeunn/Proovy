package com.deundeun.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 기존
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "C002", "권한이 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "C001", "잘못된 요청입니다."),
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S001", "서버 오류입니다."),
    INVALID_JSON_FORMAT(HttpStatus.BAD_REQUEST, "C003", "요청 본문의 JSON 형식이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C004", "로그인이 필요합니다."),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S002", "데이터베이스 처리 중 오류가 발생했습니다."),

    // 결제/캐시/정산
    INVALID_CHARGE_AMOUNT(HttpStatus.BAD_REQUEST, "CG001", "충전 금액은 1,000원 이상 50,000원 이하, 1,000원 단위여야 합니다."),
    PG_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "CG002", "결제 승인 금액이 요청 금액과 일치하지 않습니다."),
    CHARGE_TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "CG003", "충전 거래를 찾을 수 없습니다."),
    PG_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "CG004", "PG사 요청 처리 중 오류가 발생했습니다."),
    PG_UNAUTHORIZED(HttpStatus.BAD_GATEWAY, "CG005", "PG사 인증키가 올바르지 않습니다."),
    PG_DUPLICATE_REQUEST(HttpStatus.CONFLICT, "CG006", "이미 처리 중이거나 처리된 요청입니다."),
    PG_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "CG007", "PG사 서비스 점검 중입니다. 잠시 후 다시 시도해주세요."),
    PG_INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "CG008", "계좌 잔액이 부족하여 결제가 거절되었습니다."),
    SETTLEMENT_ALREADY_PROCESSED(HttpStatus.CONFLICT, "CG012", "이미 정산 처리된 챌린지입니다."),
    PG_TIME_EXPIRED(HttpStatus.BAD_REQUEST, "CG009", "결제 유효 시간이 만료되었습니다. 다시 시도해주세요."),
    PG_OWNER_AUTH_FAIL(HttpStatus.BAD_REQUEST, "CG010", "본인 인증에 실패했습니다."),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "CG011", "사용 가능한 잔액이 부족합니다."),
    SETTLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CG013", "정산 결과를 찾을 수 없습니다."),
    HOST_REVENUE_NOT_FOUND(HttpStatus.NOT_FOUND, "CG014", "방장 수익 내역을 찾을 수 없습니다."),
    INVALID_WITHDRAWAL_AMOUNT(HttpStatus.BAD_REQUEST, "CG015", "리워드 출금은 5,000원 이상부터 가능합니다."),
    WITHDRAWAL_NOT_FOUND(HttpStatus.NOT_FOUND, "CG016", "출금 신청 내역을 찾을 수 없습니다."),
    WITHDRAWAL_ALREADY_PROCESSED(HttpStatus.CONFLICT, "CG017", "이미 처리된 출금 신청입니다."),
    HOLD_NOT_FOUND(HttpStatus.NOT_FOUND, "CG018", "홀딩 내역을 찾을 수 없습니다."),
    HOLD_ALREADY_CANCELLED(HttpStatus.CONFLICT, "CG019", "이미 취소 처리된 홀딩입니다."),
    INVALID_CANCEL_AMOUNT(HttpStatus.BAD_REQUEST, "CG020", "취소 금액이 원래 홀딩 금액과 일치하지 않습니다."),

    //chat
    CHAT_ROOM_FORBIDDEN(HttpStatus.FORBIDDEN, "CH001", "해당 채팅방에 접근할 권한이 없습니다."),
    CHAT_ROOM_ALREADY_LEFT(HttpStatus.CONFLICT, "CH002", "이미 나간 채팅방입니다."),
    CHAT_ROOM_ALREADY_JOINED(HttpStatus.CONFLICT, "CH003", "이미 참여 중인 채팅방입니다."),
    CHAT_ROOM_SELF_CHAT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "CH004", "자기 자신과는 1:1 채팅방을 생성할 수 없습니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CH005", "존재하지 않는 채팅방입니다."),

    //notification
    NOTIFICATION_FORBIDDEN(HttpStatus.FORBIDDEN, "NT001", "해당 알림에 접근할 권한이 없습니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NT002", "알림을 찾을 수 없습니다."),
    NOTIFICATION_SUBSCRIBE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "NT003", "알림 구독 연결에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}