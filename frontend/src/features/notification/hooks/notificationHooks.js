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

export const useNotifications = () =>
  useInfiniteQuery({
    queryKey: notificationKeys.list({ size: PAGE_SIZE }),
    queryFn: ({ pageParam }) => getNotifications({ page: pageParam, size: PAGE_SIZE }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => (lastPage.hasNext ? lastPage.page + 1 : undefined),
  });

export const useUnreadCount = () =>
  useQuery({ queryKey: notificationKeys.unreadCount(), queryFn: getUnreadCount });

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
    onSuccess: () => queryClient.invalidateQueries({ queryKey: notificationKeys.lists() }),
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
export const useNotificationSubscription = (onNotificationCreated) => {
  const callbackRef = useRef(onNotificationCreated);

  useEffect(() => {
    callbackRef.current = onNotificationCreated;
  }, [onNotificationCreated]);

  useEffect(() => {
    const eventSource = new EventSource("/api/notifications/subscribe", { withCredentials: true });

    eventSource.addEventListener("NOTIFICATION_CREATED", (event) => {
      callbackRef.current?.(JSON.parse(event.data));
    });

    return () => eventSource.close();
  }, []);
};

// SSE로 새 알림이 오면 목록 캐시 맨 앞에 바로 꽂아넣고, 안읽음 개수는 무효화해서 재조회한다.
export const useNotificationRealtimeSync = () => {
  const queryClient = useQueryClient();

  useNotificationSubscription((notification) => {
    queryClient.setQueryData(notificationKeys.list({ size: PAGE_SIZE }), (data) => {
      if (!data) return data;

      const [firstPage, ...restPages] = data.pages;
      return {
        ...data,
        pages: [{ ...firstPage, content: [notification, ...firstPage.content] }, ...restPages],
      };
    });

    queryClient.invalidateQueries({ queryKey: notificationKeys.unreadCount() });
  });
};
