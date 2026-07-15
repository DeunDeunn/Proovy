package com.deundeun.chat.controller;

import java.security.Principal;

import org.springframework.dao.DataAccessException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.ControllerAdvice;

import com.deundeun.chat.dto.response.ChatMessageSendFailedEvent;
import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@ControllerAdvice(assignableTypes = ChatStompController.class)
public class ChatStompExceptionHandler {

    private static final String ERROR_DESTINATION = "/queue/errors";

    private final SimpMessagingTemplate messagingTemplate;

    @MessageExceptionHandler(ApiException.class)
    public void handleApiException(ApiException e, Principal principal) {
        log.warn("STOMP ApiException: {} - {}", e.getErrorCode().getCode(), e.getMessage(), e);
        sendErrorEvent(principal, e.getErrorCode());
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    public void handleMethodArgumentNotValidException(MethodArgumentNotValidException e, Principal principal) {
        log.warn("STOMP 페이로드 검증 실패: {}", e.getBindingResult(), e);
        sendErrorEvent(principal, ErrorCode.CHAT_INVALID_MESSAGE_TYPE);
    }

    @MessageExceptionHandler(DataAccessException.class)
    public void handleDataAccessException(DataAccessException e, Principal principal) {
        log.error("STOMP DB 접근 오류", e);
        sendErrorEvent(principal, ErrorCode.DATABASE_ERROR);
    }

    @MessageExceptionHandler(Exception.class)
    public void handleException(Exception e, Principal principal) {
        log.error("STOMP 처리 중 알 수 없는 오류", e);
        sendErrorEvent(principal, ErrorCode.SERVER_ERROR);
    }

    private void sendErrorEvent(Principal principal, ErrorCode errorCode) {
        if (principal == null) {
            return;
        }

        messagingTemplate.convertAndSendToUser(principal.getName(), ERROR_DESTINATION, ChatMessageSendFailedEvent.of(errorCode));
    }
}
