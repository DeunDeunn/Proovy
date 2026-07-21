"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
  createOrGetDirectRoom,
  deleteChatMessage,
  getChatMessages,
  getChatRooms,
  markChatRoomRead,
  sendChatAttachment,
  shareCertificationToChatRoom,
} from "@/features/chat/api/chatApi";
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

// DirectChatRoomResponse(생성/조회 API 응답)를 방 목록 아이템(ChatRoomSummaryResponse)과 같은 모양으로 맞춘다.
const normalizeDirectRoom = (room) => ({
  chatRoomId: room.chatRoomId,
  chatRoomType: room.chatRoomType,
  challengeId: null,
  challengeTitle: null,
  directChatPartner: room.partner,
  lastMessage: room.lastMessage,
  unreadCount: room.unreadCount,
  lastReadMessageId: room.lastReadMessageId,
  lastReadAt: room.lastReadAt,
  createdAt: new Date(room.createdAt),
});

export const useChatRoomSubscription = (chatRoomId, onMessage, options = {}) => {
  const { onError, onDisconnect, onConnected } = options;

  useEffect(() => {
    if (chatRoomId == null) return undefined;

    connectSocket({ onError, onDisconnect, onConnected });
    subscribeRoom(chatRoomId, onMessage);

    // 소켓 연결 자체는 useChatRealtimeSync(앱 전역)가 계속 유지하므로,
    // 방을 닫을 때는 그 방 구독만 해제하고 연결은 끊지 않는다.
    return () => {
      unsubscribeRoom();
    };
  }, [chatRoomId, onMessage, onError, onDisconnect, onConnected]);
};

// 앱이 열려있는 동안 소켓 연결을 계속 유지하면서, 서버가 개인 큐로 보내는 "방 업데이트"
// 신호를 받으면 방 목록 캐시를 무효화한다. 채팅방을 열어보지 않아도(예: 사이드바만 봐도)
// 안 읽은 개수가 최신으로 반영되도록 하기 위함이다 (알림의 SSE 실시간 동기화와 동일한 목적).
export const useChatRealtimeSync = () => {
  const queryClient = useQueryClient();

  useEffect(() => {
    connectSocket({
      onRoomUpdated: () => queryClient.invalidateQueries({ queryKey: ["chat-rooms"] }),
    });

    return () => disconnectSocket();
  }, [queryClient]);
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

// 훅 렌더마다 새로 만들어지면 react-query가 select 결과를 재사용하지 못해
// roomsData 참조가 매번 바뀌므로, 모듈 스코프에 고정해 재사용한다.
const selectChatRooms = (response) => ({ ...response, content: parseRooms(response.content) });

export const useChatRooms = (params, { enabled = true } = {}) =>
  useQuery({
    queryKey: ["chat-rooms", params],
    queryFn: () => getChatRooms(params),
    select: selectChatRooms,
    enabled,
  });

// 채팅방 목록을 조회해 store에 동기화한다. 사이드바(안 읽은 개수 배지)와 채팅 페이지가
// 함께 사용해서, 로그인 후 어느 화면에 있든 목록이 채워지도록 한다.
export const useChatRoomsSync = ({ enabled = true } = {}) => {
  const setRooms = useChatStore((state) => state.setRooms);
  const query = useChatRooms(undefined, { enabled });

  useEffect(() => {
    if (query.data) setRooms(query.data.content);
  }, [query.data, setRooms]);

  return query;
};

// 방 입장 시 서버에 읽음 처리하고, 응답으로 받은 커서를 store의 방 목록에 반영한다.
export const useMarkRoomRead = () => {
  const markRoomRead = useChatStore((state) => state.markRoomRead);

  return useMutation({
    mutationFn: (chatRoomId) => markChatRoomRead(chatRoomId),
    onSuccess: (response) => markRoomRead(response),
  });
};

// 삭제 결과는 REST 응답이 아니라, 방 소켓 구독으로 돌아오는 MESSAGE_DELETED
// 브로드캐스트를 통해 store에 반영된다 (메시지 전송과 동일한 패턴).
export const useDeleteChatMessage = () =>
  useMutation({
    mutationFn: (messageId) => deleteChatMessage(messageId),
  });

// 첨부파일 전송도 삭제와 마찬가지로, 결과는 REST 응답이 아니라 방 소켓 구독으로
// 돌아오는 MESSAGE_CREATED 브로드캐스트를 통해 store에 반영된다.
export const useSendChatAttachment = (chatRoomId) =>
  useMutation({
    mutationFn: ({ messageType, content, file }) =>
      sendChatAttachment(chatRoomId, { messageType, content, file }),
  });

// 인증글 상세 페이지처럼 채팅 소켓 연결이 없는 화면에서도 공유할 수 있도록 REST로 보낸다.
export const useShareCertificationToChatRoom = () =>
  useMutation({
    mutationFn: ({ chatRoomId, certificationId }) =>
      shareCertificationToChatRoom(chatRoomId, certificationId),
  });

export const useCreateDirectChatRoom = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (targetUserId) => createOrGetDirectRoom(targetUserId),
    onSuccess: (room) => {
      // 목록 페이지 밖에 있던 기존 방일 수 있어, 재조회 결과를 기다리지 않고 바로 store에 반영한다.
      useChatStore.getState().upsertRoom(normalizeDirectRoom(room));
      queryClient.invalidateQueries({ queryKey: ["chat-rooms"] });
    },
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
