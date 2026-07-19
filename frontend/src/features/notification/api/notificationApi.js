import api from "@/lib/api";

export const getNotifications = ({ page = 0, size = 20, category } = {}) =>
  api.get("/notifications", { params: { page, size, category } });

export const getUnreadCount = () => api.get("/notifications/unread-count");

export const markNotificationAsRead = (notificationId) =>
  api.patch(`/notifications/${notificationId}/read`);

export const markAllNotificationsAsRead = () => api.patch("/notifications/read-all");

export const deleteNotification = (notificationId) =>
  api.delete(`/notifications/${notificationId}`);

export const deleteAllNotifications = () => api.delete("/notifications/all");
