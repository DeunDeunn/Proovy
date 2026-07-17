package com.deundeun.chat.service.support;

import com.deundeun.chat.constant.ChatStompDestinations;
import com.deundeun.chat.dto.response.ChatSubscribeFailedEvent;
import com.deundeun.chat.event.ChatSubscriptionRejectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSubscriptionRejectionNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handle(ChatSubscriptionRejectedEvent event) {
        messagingTemplate.convertAndSendToUser(
            event.username(), ChatStompDestinations.PERSONAL_ERROR_QUEUE, ChatSubscribeFailedEvent.of(event.errorCode()));
    }
}
