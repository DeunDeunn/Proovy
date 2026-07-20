export const notificationKeys = {
  all: ["notifications"],
  lists: () => [...notificationKeys.all, "list"],
  list: (params) => [...notificationKeys.lists(), params],
  unreadCount: () => [...notificationKeys.all, "unread-count"],
};
