import api from "@/lib/api";

export const getChatMessages = (chatRoomId, { beforeMessageId, size = 30 } = {}) =>
  api.get(`/chats/rooms/${chatRoomId}/messages`, { params: { beforeMessageId, size } });
