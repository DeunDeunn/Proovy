import { X } from "lucide-react";

import Badge from "@/components/ui/Badge";
import { NOTIFICATION_TYPES } from "@/features/notification/mockData";

const NotificationCard = ({ notification, onRead, onDelete }) => {
  const meta = NOTIFICATION_TYPES[notification.type];
  const Icon = meta.icon;
  const unread = !notification.read;

  return (
    <div
      className={`group relative flex w-full items-start gap-4 rounded-xl border p-4 pr-9 transition-colors ${
        unread
          ? "border-primary-light bg-primary-light"
          : "border-gray-200 bg-surface hover:bg-gray-50"
      }`}
    >
      <button
        type="button"
        onClick={() => onRead(notification.id)}
        className="flex min-w-0 flex-1 items-start gap-4 text-left"
      >
        <div className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full ${meta.iconClass}`}>
          <Icon size={20} />
        </div>

        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <p className="truncate text-sm font-semibold text-gray-900">{notification.title}</p>
            <Badge variant={meta.badgeVariant}>{meta.tagLabel}</Badge>
          </div>
          <p className="mt-1 line-clamp-2 text-sm text-gray-500">{notification.description}</p>
        </div>

        <div className="flex shrink-0 flex-col items-end gap-2 pl-2">
          <span className="text-xs text-gray-400">{notification.timeAgo}</span>
          <span
            className={`h-2 w-2 rounded-full ${unread ? "bg-primary" : "border border-gray-300"}`}
          />
        </div>
      </button>

      <button
        type="button"
        onClick={(event) => {
          event.stopPropagation();
          onDelete(notification.id);
        }}
        aria-label="알림 삭제"
        className="absolute right-2 top-2 rounded-full p-1 text-gray-300 opacity-0 transition-opacity hover:bg-gray-100 hover:text-gray-500 group-hover:opacity-100"
      >
        <X size={14} />
      </button>
    </div>
  );
};

export default NotificationCard;
