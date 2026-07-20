import { create } from "zustand";

import { publishMessage } from "@/features/chat/api/chatSocket";
import { createMockChatRooms } from "@/features/chat/mockData";

const mergeMessages = (base, extra) => {
  const byId = new Map(base.map((message) => [message.messageId, message]));
  extra.forEach((message) => {
    if (!byId.has(message.messageId)) byId.set(message.messageId, message);
  });
  return Array.from(byId.values()).sort((a, b) => a.messageId - b.messageId);
};

export const useChatStore = create((set) => ({
  rooms: createMockChatRooms(),
  messagesByRoomId: {},

  markRoomRead: (chatRoomId) =>
    set((state) => ({
      rooms: state.rooms.map((room) =>
        room.chatRoomId === chatRoomId ? { ...room, unreadCount: 0 } : room
      ),
    })),

  setRoomMessages: (chatRoomId, messages) =>
    set((state) => ({
      messagesByRoomId: {
        ...state.messagesByRoomId,
        // REST 조회 응답 도착 전에 WS로 먼저 들어온 실시간 메시지가 있을 수 있어 교체 대신 병합한다.
        [chatRoomId]: mergeMessages(messages, state.messagesByRoomId[chatRoomId] ?? []),
      },
    })),

  prependRoomMessages: (chatRoomId, olderMessages) =>
    set((state) => ({
      messagesByRoomId: {
        ...state.messagesByRoomId,
        [chatRoomId]: [...olderMessages, ...(state.messagesByRoomId[chatRoomId] ?? [])],
      },
    })),

  receiveMessage: (event) =>
    set((state) => {
      if (event.eventType !== "MESSAGE_CREATED") return state;

      const message = { ...event, createdAt: new Date(event.createdAt) };
      const { chatRoomId } = message;

      return {
        messagesByRoomId: {
          ...state.messagesByRoomId,
          [chatRoomId]: [...(state.messagesByRoomId[chatRoomId] ?? []), message],
        },
        rooms: state.rooms.map((room) =>
          room.chatRoomId === chatRoomId
            ? {
                ...room,
                lastMessage: { senderNickname: message.senderNickname, content: message.content },
                createdAt: message.createdAt,
              }
            : room
        ),
      };
    }),

  sendMessage: (chatRoomId, content) => {
    publishMessage(chatRoomId, {
      messageType: "TEXT",
      content,
      referenceType: null,
      referenceId: null,
    });
  },
}));

export const useUnreadChatCount = () =>
  useChatStore((state) => state.rooms.reduce((sum, room) => sum + room.unreadCount, 0));
