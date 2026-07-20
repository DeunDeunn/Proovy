import { create } from "zustand";

import { CURRENT_USER } from "@/features/chat/currentUser";
import { createMockChatRooms } from "@/features/chat/mockData";
import { createMockMessages } from "@/features/chat/mockMessages";

let nextMessageId = 1000;

export const useChatStore = create((set) => ({
  rooms: createMockChatRooms(),
  messagesByRoomId: createMockMessages(),

  markRoomRead: (chatRoomId) =>
    set((state) => ({
      rooms: state.rooms.map((room) =>
        room.chatRoomId === chatRoomId ? { ...room, unreadCount: 0 } : room
      ),
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

  sendMessage: (chatRoomId, content) =>
    set((state) => {
      const message = {
        messageId: nextMessageId++,
        chatRoomId,
        senderId: CURRENT_USER.id,
        senderNickname: CURRENT_USER.nickname,
        senderBadgeApproved: false,
        content,
        messageType: "TEXT",
        sharedCertification: null,
        deletedAt: null,
        createdAt: new Date(),
        read: false,
      };

      return {
        messagesByRoomId: {
          ...state.messagesByRoomId,
          [chatRoomId]: [...(state.messagesByRoomId[chatRoomId] ?? []), message],
        },
        rooms: state.rooms.map((room) =>
          room.chatRoomId === chatRoomId
            ? {
                ...room,
                lastMessage: { senderNickname: CURRENT_USER.nickname, content },
                createdAt: message.createdAt,
              }
            : room
        ),
      };
    }),
}));

export const useUnreadChatCount = () =>
  useChatStore((state) => state.rooms.reduce((sum, room) => sum + room.unreadCount, 0));
