import api from "@/lib/api";

export const getChatMessages = (chatRoomId, { beforeMessageId, size = 30 } = {}) =>
  api.get(`/chats/rooms/${chatRoomId}/messages`, { params: { beforeMessageId, size } });

export const createOrGetDirectRoom = (targetUserId) =>
  api.post("/chats/direct-rooms", { targetUserId });

export const getChatRooms = ({ page = 0, size = 20 } = {}) =>
  api.get("/chats/rooms", { params: { page, size } });

export const markChatRoomRead = (chatRoomId) => api.patch(`/chats/rooms/${chatRoomId}/read`);

export const deleteChatMessage = (messageId) => api.delete(`/chats/messages/${messageId}`);
