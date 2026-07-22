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

    //회원-OAuth 로그인
    OAUTH_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "U010", "잘못된 OAuth 요청입니다."),
    GOOGLE_PROFILE_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "U011", "Google 사용자 정보를 가져올 수 없습니다."),
    GOOGLE_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "U012", "Google 인증에 실패했습니다."),
    KAKAO_PROFILE_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "U013", "Kakao 사용자 정보를 가져올 수 없습니다."),
    KAKAO_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "U014", "Kakao 인증에 실패했습니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "U018", "리프레시 토큰이 유효하지 않습니다. 다시 로그인해주세요."),
    REAUTH_FAILED(HttpStatus.UNAUTHORIZED, "U019", "본인 확인에 실패했습니다. 로그인된 계정과 일치하지 않습니다."),
    WITHDRAWAL_ACTIVE_CHALLENGE(HttpStatus.BAD_REQUEST, "U005", "진행 중인 챌린지(참가 또는 방장)가 있어 탈퇴할 수 없습니다."),
    WITHDRAWAL_CASH_REMAINING(HttpStatus.BAD_REQUEST, "U006", "충전 캐시 잔액이 남아있어 탈퇴할 수 없습니다. 출금 후 다시 시도해주세요."),
    NICKNAME_DUPLICATE(HttpStatus.CONFLICT, "U002", "이미 사용 중인 닉네임입니다."),
    NICKNAME_INVALID(HttpStatus.BAD_REQUEST, "U003", "닉네임은 2자 이상 10자 이하로 입력해주세요."),

    //회원-팔로우
    SELF_FOLLOW_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "U020", "자기 자신을 팔로우할 수 없습니다."),
    ALREADY_FOLLOWING(HttpStatus.CONFLICT, "U021", "이미 팔로우 중인 사용자입니다."),
    NOT_FOLLOWING(HttpStatus.BAD_REQUEST, "U022", "팔로우하지 않은 사용자입니다."),

    //회원-우수 사용자 인증
    VERIFICATION_ALREADY_PENDING(HttpStatus.CONFLICT, "U023", "이미 심사 중인 신청 건이 있습니다."),
    VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "U024", "신청 내역을 찾을 수 없습니다."),
    VERIFICATION_ALREADY_PROCESSED(HttpStatus.CONFLICT, "U025", "이미 처리된 신청 건입니다."),
    VERIFICATION_REJECTION_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "U026", "반려 사유는 필수 입력사항입니다."),
    VERIFICATION_INELIGIBLE(HttpStatus.BAD_REQUEST, "U027", "성공한 챌린지가 20개 미만이라 신청할 수 없습니다."),
    WITHDRAWAL_SUSPENDED(HttpStatus.BAD_REQUEST, "U028", "정지 중에는 탈퇴할 수 없습니다."),
    VERIFICATION_ALREADY_APPROVED(HttpStatus.CONFLICT, "U029", "이미 우수 사용자로 인증되어 있습니다."),

    //challenge
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CHL001", "존재하지 않는 카테고리입니다."),
    INVALID_CHALLENGE_PERIOD(HttpStatus.BAD_REQUEST, "CHL002", "종료일은 시작일 이후여야 합니다."),
    INVALID_CERT_TIME_RANGE(HttpStatus.BAD_REQUEST, "CHL003", "인증 종료 시간은 시작 시간 이후여야 합니다."),
    CHALLENGE_NOT_EDITABLE(HttpStatus.CONFLICT, "CHL004", "참가자가 있는 챌린지는 핵심 조건을 수정할 수 없습니다."),
    CHALLENGE_NOT_RECRUITING(HttpStatus.CONFLICT, "CHL005", "모집 중인 챌린지가 아닙니다."),
    CHALLENGE_FULL(HttpStatus.CONFLICT, "CHL006", "모집 정원이 가득 찼습니다."),
    ALREADY_JOINED_CHALLENGE(HttpStatus.CONFLICT, "CHL007", "이미 참여 중인 챌린지입니다."),
    HOST_CANNOT_LEAVE(HttpStatus.BAD_REQUEST, "CHL008", "방장은 자신의 챌린지에서 탈퇴할 수 없습니다."),
    CHALLENGE_HAS_PARTICIPANTS(HttpStatus.CONFLICT, "CHL009", "참가자가 있어 챌린지를 취소할 수 없습니다."),
    CANNOT_REJOIN_CHALLENGE(HttpStatus.CONFLICT, "CHL010", "참가 이력이 있는 챌린지는 다시 참가할 수 없습니다."),
    START_DATE_TOO_SOON(HttpStatus.BAD_REQUEST, "CHL011", "시작일은 최소 내일 이후여야 합니다."),
    CERT_TIME_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "CHL012", "인증 가능 시간은 오전 2시 ~ 오후 11시 사이여야 합니다."),

    // AI
    AI_REVIEW_RULE_NOT_FOUND(HttpStatus.NOT_FOUND, "A001", "AI 검수 규칙을 찾을 수 없습니다."),
    AI_REVIEW_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "A002", "AI 검수 요청이 올바르지 않습니다."),
    AI_REVIEW_RESULT_ALREADY_EXISTS(HttpStatus.CONFLICT, "A003", "이미 AI 검수 결과가 존재합니다."),
    AI_REVIEW_MODE_INVALID(HttpStatus.BAD_REQUEST, "A004", "AI 검수 모드가 올바르지 않습니다."),
    AI_REVIEW_IMAGE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "A005", "AI 검수 이미지는 최대 4개까지 요청할 수 있습니다."),
    AI_REVIEW_IMAGE_TOO_LARGE(HttpStatus.BAD_REQUEST, "A006", "AI 검수 이미지 용량이 허용 범위를 초과했습니다."),
    AI_REVIEW_IMAGE_TOTAL_TOO_LARGE(HttpStatus.BAD_REQUEST, "A007", "AI 검수 이미지 전체 용량이 허용 범위를 초과했습니다."),
    AI_REVIEW_IMAGE_INVALID_URL(HttpStatus.BAD_REQUEST, "A008", "AI 검수 이미지 URL이 올바르지 않습니다."),
    AI_REVIEW_IMAGE_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "A009", "AI 검수 이미지 다운로드에 실패했습니다."),
    AI_REVIEW_IMAGE_EMPTY(HttpStatus.INTERNAL_SERVER_ERROR, "A010", "AI 검수 이미지가 비어 있습니다."),
    GEMINI_API_FAILED(HttpStatus.BAD_GATEWAY, "A011", "Gemini API 호출에 실패했습니다."),
    GEMINI_API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "A012", "Gemini API 응답이 지연되고 있습니다."),
    GEMINI_RESPONSE_EMPTY(HttpStatus.BAD_GATEWAY, "A013", "Gemini 응답이 비어 있습니다."),
    GEMINI_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "A014", "Gemini 응답 형식이 올바르지 않습니다."),
    GEMINI_REVIEW_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "A015", "Gemini AI 검수 처리에 실패했습니다."),
    AI_TICKET_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "A016", "존재하지 않는 AI 티켓 상품입니다."),
    AI_TICKET_PLAN_INACTIVE(HttpStatus.BAD_REQUEST, "A017", "판매 중인 AI 티켓 상품이 아닙니다."),
    AI_TICKET_PURCHASE_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "A018", "AI 티켓 구매 요청이 올바르지 않습니다."),
    AI_TICKET_ALREADY_ACTIVE(HttpStatus.CONFLICT, "A019", "이미 활성화된 AI 티켓 이용권이 있습니다."),
    AI_TICKET_HISTORY_TYPE_INVALID(HttpStatus.BAD_REQUEST, "A020", "유효하지 않은 AI 티켓 이력 타입입니다."),

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
    TICKET_ALREADY_PURCHASED(HttpStatus.CONFLICT, "CG021", "이미 구매 처리된 티켓입니다."),
    HOLD_ALREADY_EXISTS(HttpStatus.CONFLICT, "CG022", "이미 홀딩 처리된 요청입니다."),

    //chat
    CHAT_ROOM_FORBIDDEN(HttpStatus.FORBIDDEN, "CH001", "해당 채팅방에 접근할 권한이 없습니다."),
    CHAT_ROOM_ALREADY_LEFT(HttpStatus.CONFLICT, "CH002", "이미 나간 채팅방입니다."),
    CHAT_ROOM_ALREADY_JOINED(HttpStatus.CONFLICT, "CH003", "이미 참여 중인 채팅방입니다."),
    CHAT_ROOM_SELF_CHAT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "CH004", "자기 자신과는 1:1 채팅방을 생성할 수 없습니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CH005", "존재하지 않는 채팅방입니다."),
    CHAT_INVALID_READ_CURSOR(HttpStatus.BAD_REQUEST, "CH006", "읽음 처리할 메시지 위치가 올바르지 않습니다."),
    CHAT_INVALID_MESSAGE_TYPE(HttpStatus.BAD_REQUEST, "CH007", "지원하지 않는 메시지 타입입니다."),
    CHAT_MESSAGE_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "CH008", "메시지 내용을 입력해주세요."),
    CHAT_MESSAGE_CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "CH009", "메시지 내용은 1000자를 초과할 수 없습니다."),
    CHAT_ATTACHMENT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "CH010", "텍스트 메시지에는 첨부파일을 첨부할 수 없습니다."),
    CHAT_ATTACHMENT_REQUIRED(HttpStatus.BAD_REQUEST, "CH011", "이미지/파일 메시지에는 첨부파일이 필요합니다."),
    CHAT_REFERENCE_REQUIRED(HttpStatus.BAD_REQUEST, "CH012", "인증 글 공유 메시지는 참조 정보가 필요합니다."),
    CHAT_ATTACHMENT_ENDPOINT_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "CH013", "이 엔드포인트는 이미지/파일 메시지만 전송할 수 있습니다."),
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "CH014", "존재하지 않는 메시지입니다."),
    CHAT_MESSAGE_NOT_OWNER(HttpStatus.FORBIDDEN, "CH015", "본인이 작성한 메시지만 삭제할 수 있습니다."),
    CHAT_MESSAGE_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "CH016", "이미 삭제된 메시지입니다."),

    //notification
    NOTIFICATION_FORBIDDEN(HttpStatus.FORBIDDEN, "NT001", "해당 알림에 접근할 권한이 없습니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NT002", "알림을 찾을 수 없습니다."),
    NOTIFICATION_SUBSCRIBE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "NT003", "알림 구독 연결에 실패했습니다."),

    // 인증/피드
    CHALLENGE_NOT_FOUND(HttpStatus.NOT_FOUND, "CF001", "챌린지가 존재하지 않습니다."),
    CHALLENGE_NOT_IN_PROGRESS(HttpStatus.BAD_REQUEST, "CF002", "진행중인 챌린지에서만 인증글을 등록할 수 있습니다."),
    NOT_CHALLENGE_PARTICIPANT(HttpStatus.FORBIDDEN, "CF003", "이 챌린지의 참가자가 아닙니다."),
    PARTICIPANT_NOT_ACTIVE(HttpStatus.FORBIDDEN, "CF004", "인증글을 등록할 수 없습니다."),
    ALREADY_CERTIFIED_TODAY(HttpStatus.CONFLICT, "CF005", "한 챌린지당 하루에 한 개의 인증글만 등록할 수 있습니다."),
    TOO_MANY_IMAGES(HttpStatus.BAD_REQUEST, "CF006", "추가 이미지는 최대 3장까지 등록할 수 있습니다."),
    THUMBNAIL_REQUIRED(HttpStatus.BAD_REQUEST, "CF007", "대표 인증 이미지는 필수 항목 입니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "CF008", "인증글을 찾을 수 없습니다."),
    NOT_PENDING_POST(HttpStatus.CONFLICT, "CF009", "승인대기 상태의 인증글만 처리 가능합니다."),
    REJECTION_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "CF010", "반려 사유는 필수 입력사항입니다."),
    NOT_IN_CERT_TIME_RANGE(HttpStatus.BAD_REQUEST, "CF011", "인증 등록 가능 시간대가 아닙니다."),
    CANNOT_LIKE_UNAPPROVED(HttpStatus.CONFLICT, "CF012", "승인된 인증글에만 좋아요할 수 있습니다."),
    // 인기순 커서 부분 입력(cursor/cursorLike 중 하나만) 거부 — 상세는 CertificationService.validatePopularCursor 참고
    INVALID_POPULAR_CURSOR(HttpStatus.BAD_REQUEST, "CF013", "잘못된 페이지 요청입니다."),

    //신고
    REPORT_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "CF014", "신고 대상을 찾을 수 없습니다."),
    ALREADY_REPORTED(HttpStatus.CONFLICT, "CF015", "이미 신고한 대상입니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "CF016", "신고 내역을 찾을 수 없습니다."),
    ALREADY_PROCESSED_REPORT(HttpStatus.CONFLICT, "CF017", "이미 처리(완료/기각)된 신고입니다."),

    //댓글
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CF018", "댓글을 찾을 수 없습니다."),
    CANNOT_COMMENT_UNAPPROVED(HttpStatus.CONFLICT, "CF019", "승인된 인증글에만 댓글을 작성할 수 있습니다."),
    // 대댓글(parent_comment_id != null)에 다시 대댓글을 다는 경우 — 2단계까지만 허용
    COMMENT_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "CF020", "대댓글에는 다시 댓글을 달 수 없습니다."),
    COMMENT_CONTENTS_REQUIRED(HttpStatus.BAD_REQUEST, "CF021", "댓글 내용은 필수 입력사항입니다."),
    NO_COMMENT_PERMISSION(HttpStatus.FORBIDDEN, "CF022", "댓글에 대한 권한이 없습니다."),
    CANNOT_LIKE_COMMENT_UNAPPROVED(HttpStatus.CONFLICT, "CF023", "승인된 인증글의 댓글에만 좋아요할 수 있습니다."),
    INVALID_KEPT_IMAGE(HttpStatus.BAD_REQUEST, "CF024", "유지할 수 없는 이미지입니다."),
    HOST_POST_MANUAL_REVIEW_FORBIDDEN(HttpStatus.FORBIDDEN, "CF025", "방장의 인증글은 AI만 검수할 수 있습니다."),

    //파일 업로드
    FILE_EMPTY(HttpStatus.BAD_REQUEST, "F001", "업로드할 파일이 비어 있습니다."),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "F002", "파일 용량이 허용된 크기를 초과했습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "F003", "허용되지 않는 파일 형식입니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F004", "파일 업로드 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
