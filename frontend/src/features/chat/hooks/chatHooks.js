"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { createOrGetDirectRoom, getChatMessages, getChatRooms } from "@/features/chat/api/chatApi";
import {
  connectSocket,
  disconnectSocket,
  subscribeRoom,
  unsubscribeRoom,
} from "@/features/chat/api/chatSocket";
import { useChatStore } from "@/features/chat/store";

const parseMessages = (content) =>
  content.map((message) => ({ ...message, createdAt: new Date(message.createdAt) })).reverse();

const parseRooms = (content) =>
  content.map((room) => ({ ...room, createdAt: new Date(room.createdAt) }));

export const useChatRoomSubscription = (chatRoomId, onMessage, options = {}) => {
  const { onError, onDisconnect, onConnected } = options;

  useEffect(() => {
    if (chatRoomId == null) return undefined;

    connectSocket({ onError, onDisconnect, onConnected });
    subscribeRoom(chatRoomId, onMessage);

    return () => {
      unsubscribeRoom();
      disconnectSocket();
    };
  }, [chatRoomId, onMessage, onError, onDisconnect, onConnected]);
};

export const useChatRoomHistory = (chatRoomId) => {
  const setRoomMessages = useChatStore((state) => state.setRoomMessages);
  const prependRoomMessages = useChatStore((state) => state.prependRoomMessages);

  const [hasMore, setHasMore] = useState(false);
  const [nextCursor, setNextCursor] = useState(null);
  const [isLoadingInitial, setIsLoadingInitial] = useState(true);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [error, setError] = useState(null);

  // 방이 바뀌거나 언마운트되면 true가 돼서, 그 시점 이후 도착하는 응답(loadMore 포함)이
  // 다른 방의 상태(hasMore/nextCursor 등)를 덮어쓰지 못하게 막는다.
  const cancelledRef = useRef(false);

  useEffect(() => {
    if (chatRoomId == null) return undefined;

    cancelledRef.current = false;

    getChatMessages(chatRoomId)
      .then((response) => {
        if (cancelledRef.current) return;

        setRoomMessages(chatRoomId, parseMessages(response.content));
        setHasMore(response.hasNext);
        setNextCursor(response.nextCursor);
      })
      .catch((err) => {
        if (!cancelledRef.current) setError(err);
      })
      .finally(() => {
        if (!cancelledRef.current) setIsLoadingInitial(false);
      });

    return () => {
      cancelledRef.current = true;
    };
  }, [chatRoomId, setRoomMessages]);

  const loadMore = useCallback(() => {
    if (chatRoomId == null || !hasMore || isLoadingMore) return Promise.resolve();

    setIsLoadingMore(true);
    setError(null);

    return getChatMessages(chatRoomId, { beforeMessageId: nextCursor })
      .then((response) => {
        if (cancelledRef.current) return;

        prependRoomMessages(chatRoomId, parseMessages(response.content));
        setHasMore(response.hasNext);
        setNextCursor(response.nextCursor);
      })
      .catch((err) => {
        if (!cancelledRef.current) setError(err);
      })
      .finally(() => {
        if (!cancelledRef.current) setIsLoadingMore(false);
      });
  }, [chatRoomId, hasMore, isLoadingMore, nextCursor, prependRoomMessages]);

  return { loadMore, hasMore, isLoadingInitial, isLoadingMore, error };
};

export const useChatRooms = (params, { enabled = true } = {}) =>
  useQuery({
    queryKey: ["chat-rooms", params],
    queryFn: () => getChatRooms(params),
    select: (response) => ({ ...response, content: parseRooms(response.content) }),
    enabled,
  });

export const useCreateDirectChatRoom = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (targetUserId) => createOrGetDirectRoom(targetUserId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["chat-rooms"] }),
  });
};

// 프로필/목록에서 "채팅하기" 클릭 시 1:1 방을 생성/조회하고 해당 방으로 이동시키는 공용 진입점
export const useStartDirectChat = () => {
  const router = useRouter();
  const mutation = useCreateDirectChatRoom();

  const startChat = (targetUserId) => {
    mutation.mutate(targetUserId, {
      onSuccess: (room) => router.push(`/chat?roomId=${room.chatRoomId}`),
    });
  };

  return {
    startChat,
    isPending: mutation.isPending,
    error: mutation.error,
    targetUserId: mutation.variables,
  };
};
