"use client";

import { useEffect, useRef } from "react";
import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  deleteAllNotifications,
  deleteNotification,
  getNotifications,
  getUnreadCount,
  markAllNotificationsAsRead,
  markNotificationAsRead,
} from "../api/notificationApi";
import { notificationKeys } from "./notificationQueryKeys";

const PAGE_SIZE = 20;

export const useNotifications = (category, { enabled = true } = {}) =>
  useInfiniteQuery({
    queryKey: notificationKeys.list({ size: PAGE_SIZE, category }),
    queryFn: ({ pageParam }) => getNotifications({ page: pageParam, size: PAGE_SIZE, category }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => (lastPage.hasNext ? lastPage.page + 1 : undefined),
    enabled,
  });

export const useUnreadCount = ({ enabled = true } = {}) =>
  useQuery({ queryKey: notificationKeys.unreadCount(), queryFn: getUnreadCount, enabled });

export const useMarkAsRead = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: markNotificationAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.lists() });
      queryClient.invalidateQueries({ queryKey: notificationKeys.unreadCount() });
    },
  });
};

export const useMarkAllAsRead = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: markAllNotificationsAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.lists() });
      queryClient.invalidateQueries({ queryKey: notificationKeys.unreadCount() });
    },
  });
};

export const useDeleteNotification = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deleteNotification,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.lists() });
      queryClient.invalidateQueries({ queryKey: notificationKeys.unreadCount() });
    },
  });
};

export const useDeleteAllNotifications = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: deleteAllNotifications,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.lists() });
      queryClient.invalidateQueries({ queryKey: notificationKeys.unreadCount() });
    },
  });
};

// EventSource로 /api/notifications/subscribe에 연결하고, NOTIFICATION_CREATED 수신 시 콜백을 호출한다.
// 재연결은 브라우저 EventSource의 기본 동작(Last-Event-ID 자동 전송)에 맡긴다.
export const useNotificationSubscription = (onNotificationCreated, { enabled = true } = {}) => {
  const callbackRef = useRef(onNotificationCreated);

  useEffect(() => {
    callbackRef.current = onNotificationCreated;
  }, [onNotificationCreated]);

  useEffect(() => {
    if (!enabled) return undefined;

    const eventSource = new EventSource("/api/notifications/subscribe", { withCredentials: true });

    eventSource.addEventListener("NOTIFICATION_CREATED", (event) => {
      callbackRef.current?.(JSON.parse(event.data));
    });

    return () => eventSource.close();
  }, [enabled]);
};

// SSE로 새 알림이 오면 목록/안읽음 캐시를 무효화해서 재조회한다.
// 필터(category)별로 캐시가 여러 개 동시에 존재할 수 있어(전체/인증/정산/기타), 특정 캐시 하나만
// 직접 수정하는 대신 notificationKeys.lists()로 모든 변형을 한 번에 무효화한다.
export const useNotificationRealtimeSync = ({ enabled = true } = {}) => {
  const queryClient = useQueryClient();

  useNotificationSubscription(
    () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.lists() });
      queryClient.invalidateQueries({ queryKey: notificationKeys.unreadCount() });
    },
    { enabled }
  );
};
