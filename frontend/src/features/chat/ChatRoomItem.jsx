import Badge from "@/components/ui/Badge";
import {
  CHAT_ROOM_TYPES,
  formatChatTime,
  getAvatarColor,
  getRoomDisplayName,
} from "@/features/chat/mockData";

const ChatRoomItem = ({ room, selected, onSelect }) => {
  const meta = CHAT_ROOM_TYPES[room.chatRoomType];
  const displayName = getRoomDisplayName(room);
  const isChallenge = room.chatRoomType === "CHALLENGE";
  const avatarColor = getAvatarColor(room.chatRoomId);
  const unread = room.unreadCount > 0;

  const previewText = room.lastMessage?.content;

  return (
    <button
      type="button"
      onClick={() => onSelect(room.chatRoomId)}
      className={`flex w-full items-center gap-3 rounded-xl px-3 py-3 text-left transition-colors ${
        selected ? "bg-primary-light" : "hover:bg-gray-50"
      }`}
    >
      <div
        className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-full text-sm font-semibold ${avatarColor.bg} ${avatarColor.text}`}
      >
        {displayName.slice(0, 1)}
      </div>

      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <p className="truncate text-sm font-semibold text-gray-900">{displayName}</p>
          {isChallenge && <Badge variant="primary">{meta.tagLabel}</Badge>}
        </div>
        <p className="mt-0.5 truncate text-sm text-gray-500">{previewText}</p>
      </div>

      <div className="flex shrink-0 flex-col items-end gap-1.5 pl-2">
        <span className="text-xs text-gray-400">{formatChatTime(room.createdAt)}</span>
        {unread && (
          <span className="flex h-5 min-w-5 items-center justify-center rounded-full bg-primary px-1.5 text-xs font-semibold text-white">
            {room.unreadCount}
          </span>
        )}
      </div>
    </button>
  );
};

export default ChatRoomItem;
