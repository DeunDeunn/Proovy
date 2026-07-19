import { create } from "zustand";

import { createMockNotifications } from "@/features/notification/mockData";

export const useNotificationStore = create((set) => ({
  notifications: createMockNotifications(),

  markRead: (id) =>
    set((state) => ({
      notifications: state.notifications.map((n) => (n.id === id ? { ...n, read: true } : n)),
    })),

  markAllRead: () =>
    set((state) => ({
      notifications: state.notifications.map((n) => ({ ...n, read: true })),
    })),

  remove: (id) =>
    set((state) => ({
      notifications: state.notifications.filter((n) => n.id !== id),
    })),

  clearAll: () => set({ notifications: [] }),
}));

export const useUnreadNotificationCount = () =>
  useNotificationStore((state) => state.notifications.filter((n) => !n.read).length);
