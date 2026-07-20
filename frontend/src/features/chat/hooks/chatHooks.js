import { useCallback, useEffect, useState } from "react";

import { getChatMessages } from "@/features/chat/api/chatApi";
import { connectSocket, subscribeRoom, unsubscribeRoom } from "@/features/chat/api/chatSocket";
import { useChatStore } from "@/features/chat/store";

const parseMessages = (content) =>
  content.map((message) => ({ ...message, createdAt: new Date(message.createdAt) })).reverse();

export const useChatRoomSubscription = (chatRoomId, onMessage) => {
  useEffect(() => {
    if (chatRoomId == null) return undefined;

    connectSocket();
    subscribeRoom(chatRoomId, onMessage);

    return () => unsubscribeRoom();
  }, [chatRoomId, onMessage]);
};

export const useChatRoomHistory = (chatRoomId) => {
  const setRoomMessages = useChatStore((state) => state.setRoomMessages);
  const prependRoomMessages = useChatStore((state) => state.prependRoomMessages);

  const [hasMore, setHasMore] = useState(false);
  const [nextCursor, setNextCursor] = useState(null);
  const [isLoadingMore, setIsLoadingMore] = useState(false);

  useEffect(() => {
    if (chatRoomId == null) return undefined;

    let cancelled = false;

    getChatMessages(chatRoomId).then((response) => {
      if (cancelled) return;

      setRoomMessages(chatRoomId, parseMessages(response.content));
      setHasMore(response.hasNext);
      setNextCursor(response.nextCursor);
    });

    return () => {
      cancelled = true;
    };
  }, [chatRoomId, setRoomMessages]);

  const loadMore = useCallback(() => {
    if (chatRoomId == null || !hasMore || isLoadingMore) return Promise.resolve();

    setIsLoadingMore(true);

    return getChatMessages(chatRoomId, { beforeMessageId: nextCursor })
      .then((response) => {
        prependRoomMessages(chatRoomId, parseMessages(response.content));
        setHasMore(response.hasNext);
        setNextCursor(response.nextCursor);
      })
      .finally(() => setIsLoadingMore(false));
  }, [chatRoomId, hasMore, isLoadingMore, nextCursor, prependRoomMessages]);

  return { loadMore, hasMore, isLoadingMore };
};
