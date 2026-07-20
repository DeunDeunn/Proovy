import { useEffect } from "react";

import { connectSocket, subscribeRoom, unsubscribeRoom } from "@/features/chat/api/chatSocket";

export const useChatRoomSubscription = (chatRoomId, onMessage) => {
  useEffect(() => {
    if (chatRoomId == null) return undefined;

    connectSocket();
    subscribeRoom(chatRoomId, onMessage);

    return () => unsubscribeRoom();
  }, [chatRoomId, onMessage]);
};
