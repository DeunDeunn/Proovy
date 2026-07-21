import { create } from "zustand";

import { publishMessage } from "@/features/chat/api/chatSocket";

const mergeMessages = (base, extra) => {
  const byId = new Map(base.map((message) => [message.messageId, message]));
  extra.forEach((message) => {
    if (!byId.has(message.messageId)) byId.set(message.messageId, message);
  });
  return Array.from(byId.values()).sort((a, b) => a.messageId - b.messageId);
};

export const useChatStore = create((set) => ({
  rooms: [],
  messagesByRoomId: {},

  // 로그아웃/계정 전환 시 호출한다. setRooms는 기존 방을 병합해서 유지하므로,
  // 초기화 없이 다른 계정으로 로그인하면 이전 계정의 방/메시지가 그대로 남아 노출될 수 있다.
  resetChat: () => set({ rooms: [], messagesByRoomId: {} }),

  setRooms: (rooms) =>
    set((state) => {
      const freshIds = new Set(rooms.map((room) => room.chatRoomId));
      // 서버가 준 페이지 밖에 있던 방(예: roomId 딥링크로 열어 upsertRoom된 방)이
      // 목록 갱신 시 사라져서 열려있던 대화창이 닫히지 않도록, 교체 대신 병합한다.
      const localOnly = state.rooms.filter((room) => !freshIds.has(room.chatRoomId));
      return { rooms: [...rooms, ...localOnly] };
    }),

  upsertRoom: (room) =>
    set((state) => {
      const exists = state.rooms.some((r) => r.chatRoomId === room.chatRoomId);
      return {
        rooms: exists
          ? state.rooms.map((r) => (r.chatRoomId === room.chatRoomId ? { ...r, ...room } : r))
          : [room, ...state.rooms],
      };
    }),

  markRoomRead: ({ chatRoomId, lastReadMessageId, lastReadAt }) =>
    set((state) => ({
      rooms: state.rooms.map((room) =>
        room.chatRoomId === chatRoomId
          ? {
              ...room,
              unreadCount: 0,
              lastReadMessageId,
              lastReadAt: lastReadAt ? new Date(lastReadAt) : room.lastReadAt,
            }
          : room
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
      if (event.eventType === "ROOM_READ") {
        const { chatRoomId, userId, lastReadMessageId } = event;
        const roomMessages = state.messagesByRoomId[chatRoomId];
        if (!roomMessages || lastReadMessageId == null) return state;

        // 상대방(userId)이 읽음 처리한 시점까지, 내가(=상대방이 아닌 발신자) 보낸 메시지를 읽음으로 표시한다.
        return {
          messagesByRoomId: {
            ...state.messagesByRoomId,
            [chatRoomId]: roomMessages.map((message) =>
              message.senderId !== userId && message.messageId <= lastReadMessageId
                ? { ...message, read: true }
                : message
            ),
          },
        };
      }

      if (event.eventType === "MESSAGE_DELETED") {
        const { chatRoomId, messageId, deletedAt } = event;
        const roomMessages = state.messagesByRoomId[chatRoomId];
        const deletedAtDate = new Date(deletedAt);

        return {
          messagesByRoomId: roomMessages
            ? {
                ...state.messagesByRoomId,
                [chatRoomId]: roomMessages.map((message) =>
                  message.messageId === messageId ? { ...message, deletedAt: deletedAtDate } : message
                ),
              }
            : state.messagesByRoomId,
          // 삭제된 메시지가 방 목록의 마지막 메시지 미리보기였다면, 사이드바에
          // 삭제된 내용이 계속 보이지 않도록 미리보기도 함께 갱신한다.
          rooms: state.rooms.map((room) =>
            room.chatRoomId === chatRoomId && room.lastMessage?.messageId === messageId
              ? { ...room, lastMessage: { ...room.lastMessage, content: null, deletedAt: deletedAtDate } }
              : room
          ),
        };
      }

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
                // messageId를 남겨둬야, 이 메시지가 나중에 삭제될 때 방 목록 미리보기와
                // 매칭해서 함께 갱신할 수 있다.
                lastMessage: {
                  messageId: message.messageId,
                  senderId: message.senderId,
                  senderNickname: message.senderNickname,
                  content: message.content,
                  messageType: message.messageType,
                  deletedAt: null,
                },
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
