package com.deundeun.notification.event;

import com.deundeun.notification.domain.TargetType;
import com.deundeun.notification.dto.NotificationCreateCommand;
import com.deundeun.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(VerificationApprovedEvent event) {
        NotificationCreateCommand command = new NotificationCreateCommand(
            event.userId(),
            event.type(),
            "인증이 승인되었습니다.", //TODO {챌린지 이름} 인증이 승인되었습니다.
            "챌린지 인증이 승인되었습니다. 연속 인증을 응원해요!", //TODO {YYYY.MM.dd(요일)} 인증이 승인되었습니다. 연속 인증을 응원해요!
            TargetType.VERIFICATION_POST,
            event.verificationPostId(),
            event.eventKey()
        );

        notificationService.create(command);
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(VerificationSubmittedEvent event) {
        NotificationCreateCommand command = new NotificationCreateCommand(
            event.userId(),
            event.type(),
            "새 인증이 등록되었습니다.",
            "참여자가 인증글을 등록했습니다. 확인해주세요!", //TODO {인증 보낸 사용자}님이 인증글을 등록했습니다.
            TargetType.VERIFICATION_POST,
            event.verificationPostId(),
            event.eventKey()
        );

        notificationService.create(command);
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(VerificationRejectedEvent event) {
        NotificationCreateCommand command = new NotificationCreateCommand(
            event.userId(),
            event.type(),
            "인증이 거절되었습니다.", //TODO {챌린지 이름} 인증이 거절되었습니다.
            "챌린지 인증이 거절되었습니다. 사유를 확인해주세요.", //TODO {YYYY.MM.dd(요일)} 인증이 거절되었습니다.
            TargetType.VERIFICATION_POST,
            event.verificationPostId(),
            event.eventKey()
        );

        notificationService.create(command);
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SettlementCompletedEvent event) {
        NotificationCreateCommand command = new NotificationCreateCommand(
            event.userId(),
            event.type(),
            "챌린지 정산이 완료되어 수익이 지급되었습니다.",
            "챌린지 정산이 완료되었습니다. 결과를 확인해보세요.", //TODO {챌린지 이름} 정산이 완료되었습니다. 총 수익 {정산 금액}이 캐시에 지급되었습니다.
            TargetType.SETTLEMENT,
            event.settlementId(),
            event.eventKey()
        );

        notificationService.create(command);
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(HostRevenuePaidEvent event) {
        NotificationCreateCommand command = new NotificationCreateCommand(
            event.userId(),
            event.type(),
            "방장 수수료가 지급되었습니다.",
            "챌린지 정산 후 방장 수수료가 지급되었습니다.", //TODO {챌린지 이름} 방장 수수료 {방장 수수료 금액}원이 지급되었습니다.
            TargetType.HOST_REVENUE,
            event.hostRevenueId(),
            event.eventKey()
        );

        notificationService.create(command);
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(BadgeApprovedEvent event) {
        NotificationCreateCommand command = new NotificationCreateCommand(
            event.userId(),
            event.type(),
            "뱃지 신청이 승인되었습니다.",
            "신청하신 뱃지가 승인되었습니다. 축하드려요!",
            TargetType.BADGE_APPLICATION,
            event.badgeApplicationId(),
            event.eventKey()
        );

        notificationService.create(command);
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(BadgeRejectedEvent event) {
        NotificationCreateCommand command = new NotificationCreateCommand(
            event.userId(),
            event.type(),
            "뱃지 신청이 거절되었습니다.",
            "신청하신 뱃지가 거절되었습니다. 사유를 확인해주세요.",
            TargetType.BADGE_APPLICATION,
            event.badgeApplicationId(),
            event.eventKey()
        );

        notificationService.create(command);
    }
}
