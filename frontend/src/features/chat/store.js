import { create } from "zustand";

import { publishMessage } from "@/features/chat/api/chatSocket";
import { createMockChatRooms } from "@/features/chat/mockData";

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
      messagesByRoomId: { ...state.messagesByRoomId, [chatRoomId]: messages },
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
