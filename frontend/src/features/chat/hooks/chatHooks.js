import { useEffect } from "react";

import { getChatMessages } from "@/features/chat/api/chatApi";
import { connectSocket, subscribeRoom, unsubscribeRoom } from "@/features/chat/api/chatSocket";
import { useChatStore } from "@/features/chat/store";

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

  useEffect(() => {
    if (chatRoomId == null) return;

    let cancelled = false;

    getChatMessages(chatRoomId).then((response) => {
      if (cancelled) return;

      const messages = response.content
        .map((message) => ({ ...message, createdAt: new Date(message.createdAt) }))
        .reverse();

      setRoomMessages(chatRoomId, messages);
    });

    return () => {
      cancelled = true;
    };
  }, [chatRoomId, setRoomMessages]);
};
