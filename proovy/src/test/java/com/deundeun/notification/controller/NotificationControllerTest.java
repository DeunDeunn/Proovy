package com.deundeun.notification.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.deundeun.global.security.jwt.CustomUserDetails;
import com.deundeun.notification.dto.response.NotificationDeleteAllResponse;
import com.deundeun.notification.dto.response.NotificationDeleteResponse;
import com.deundeun.notification.dto.response.NotificationPageResponse;
import com.deundeun.notification.dto.response.NotificationReadAllResponse;
import com.deundeun.notification.dto.response.NotificationReadResponse;
import com.deundeun.notification.dto.response.UnreadCountResponse;
import com.deundeun.notification.service.NotificationService;
import com.deundeun.notification.sse.SseEmitterService;

@DisplayName("NotificationController")
@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private SseEmitterService sseEmitterService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(notificationService, sseEmitterService)).build();

        CustomUserDetails userDetails = new CustomUserDetails(1L, "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("목록 조회 시 userId/page/size를 서비스에 그대로 전달한다")
    void getNotifications_delegatesToService() throws Exception {
        NotificationPageResponse response = NotificationPageResponse.of(List.of(), 0, 20, 0);
        when(notificationService.getNotifications(1L, 0, 20)).thenReturn(response);

        mockMvc.perform(get("/api/notifications?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("알림 목록 조회를 완료했습니다."));

        verify(notificationService).getNotifications(1L, 0, 20);
    }

    @Test
    @DisplayName("page/size 생략 시 기본값(0, 20)으로 조회한다")
    void getNotifications_usesDefaultPageAndSize() throws Exception {
        NotificationPageResponse response = NotificationPageResponse.of(List.of(), 0, 20, 0);
        when(notificationService.getNotifications(1L, 0, 20)).thenReturn(response);

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk());

        verify(notificationService).getNotifications(1L, 0, 20);
    }

    @Test
    @DisplayName("안 읽은 개수 조회 시 userId를 서비스에 그대로 전달한다")
    void getUnreadCount_delegatesToService() throws Exception {
        when(notificationService.countUnread(1L)).thenReturn(new UnreadCountResponse(5));

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(5));

        verify(notificationService).countUnread(1L);
    }

    @Test
    @DisplayName("단건 읽음 처리 시 userId/notificationId를 서비스에 그대로 전달한다")
    void markAsRead_delegatesToService() throws Exception {
        LocalDateTime readAt = LocalDateTime.now();
        when(notificationService.markAsRead(1L, 10L)).thenReturn(new NotificationReadResponse(10L, readAt));

        mockMvc.perform(patch("/api/notifications/10/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("알림을 읽음 처리했습니다."))
                .andExpect(jsonPath("$.data.id").value(10));

        verify(notificationService).markAsRead(1L, 10L);
    }

    @Test
    @DisplayName("전체 읽음 처리 시 userId를 서비스에 그대로 전달한다")
    void markAllAsRead_delegatesToService() throws Exception {
        LocalDateTime readAt = LocalDateTime.now();
        when(notificationService.markAllAsRead(1L)).thenReturn(new NotificationReadAllResponse(3, readAt));

        mockMvc.perform(patch("/api/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updatedCount").value(3));

        verify(notificationService).markAllAsRead(1L);
    }

    @Test
    @DisplayName("삭제 시 userId/notificationId를 서비스에 그대로 전달한다")
    void delete_delegatesToService() throws Exception {
        LocalDateTime deletedAt = LocalDateTime.now();
        when(notificationService.delete(1L, 10L)).thenReturn(new NotificationDeleteResponse(10L, deletedAt));

        mockMvc.perform(delete("/api/notifications/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("알림을 삭제했습니다."))
                .andExpect(jsonPath("$.data.id").value(10));

        verify(notificationService).delete(1L, 10L);
    }

    @Test
    @DisplayName("전체 삭제 시 userId를 서비스에 그대로 전달한다")
    void deleteAll_delegatesToService() throws Exception {
        LocalDateTime deletedAt = LocalDateTime.now();
        when(notificationService.deleteAll(1L)).thenReturn(new NotificationDeleteAllResponse(4, deletedAt));

        mockMvc.perform(delete("/api/notifications/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("전체 알림을 삭제했습니다."))
                .andExpect(jsonPath("$.data.deletedCount").value(4));

        verify(notificationService).deleteAll(1L);
    }
}
