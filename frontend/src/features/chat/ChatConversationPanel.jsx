"use client";

import { useEffect, useRef, useState } from "react";
import { ImagePlus, MoreVertical, Plus, Search, Send, Smile, Users, X } from "lucide-react";

import MessageBubble from "@/features/chat/MessageBubble";
import { CURRENT_USER } from "@/features/chat/currentUser";
import { getAvatarColor, getRoomDisplayName } from "@/features/chat/mockData";

const ChatConversationPanel = ({ room, messages, onSendMessage, onClose }) => {
  const [draft, setDraft] = useState("");
  const listEndRef = useRef(null);

  const isChallenge = room.chatRoomType === "CHALLENGE";
  const displayName = getRoomDisplayName(room);
  const avatarColor = getAvatarColor(room.chatRoomId);

  useEffect(() => {
    listEndRef.current?.scrollIntoView({ block: "end" });
  }, [messages, room.chatRoomId]);

  const handleSubmit = (event) => {
    event.preventDefault();
    const trimmed = draft.trim();
    if (!trimmed) return;

    onSendMessage(trimmed);
    setDraft("");
  };

  return (
    <div className="flex h-full w-full flex-col overflow-hidden rounded-xl border border-gray-200 bg-surface">
      <div className="flex shrink-0 items-center gap-3 border-b border-gray-100 px-5 py-4">
        <div
          className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-sm font-semibold ${avatarColor.bg} ${avatarColor.text}`}
        >
          {displayName.slice(0, 1)}
        </div>

        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <p className="truncate text-sm font-semibold text-gray-900">{displayName}</p>
            {isChallenge && (
              <span className="rounded-full bg-primary-light px-2 py-0.5 text-xs font-medium text-primary">
                챌린지방
              </span>
            )}
          </div>
          {isChallenge && <p className="mt-0.5 text-xs text-gray-400">참여자 {room.participantCount}명</p>}
        </div>

        <div className="flex shrink-0 items-center gap-1 text-gray-400">
          <button type="button" aria-label="대화 검색" className="rounded-lg p-2 hover:bg-gray-100">
            <Search size={18} />
          </button>
          {isChallenge && (
            <button type="button" aria-label="참여자 목록" className="rounded-lg p-2 hover:bg-gray-100">
              <Users size={18} />
            </button>
          )}
          <button type="button" aria-label="더보기" className="rounded-lg p-2 hover:bg-gray-100">
            <MoreVertical size={18} />
          </button>
          <button type="button" onClick={onClose} aria-label="채팅방 닫기" className="rounded-lg p-2 hover:bg-gray-100">
            <X size={18} />
          </button>
        </div>
      </div>

      <div className="min-h-0 flex-1 space-y-4 overflow-y-auto px-5 py-4">
        {messages.map((message, index) => {
          const isOwn = message.senderId === CURRENT_USER.id;
          const prevMessage = messages[index - 1];
          const showSenderInfo = !prevMessage || prevMessage.senderId !== message.senderId;

          return (
            <MessageBubble
              key={message.messageId}
              message={message}
              isOwn={isOwn}
              showSenderInfo={showSenderInfo}
              isChallenge={isChallenge}
            />
          );
        })}
        <div ref={listEndRef} />
      </div>

      <form onSubmit={handleSubmit} className="flex shrink-0 items-center gap-2 border-t border-gray-100 px-4 py-3">
        <button type="button" aria-label="파일 첨부" className="shrink-0 rounded-lg p-2 text-gray-400 hover:bg-gray-100">
          <Plus size={20} />
        </button>
        <input
          type="text"
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          placeholder="메시지 입력..."
          className="min-w-0 flex-1 bg-transparent text-sm text-gray-700 outline-none placeholder:text-gray-400"
        />
        <button type="button" aria-label="이모지" className="shrink-0 rounded-lg p-2 text-gray-400 hover:bg-gray-100">
          <Smile size={18} />
        </button>
        <button type="button" aria-label="이미지 첨부" className="shrink-0 rounded-lg p-2 text-gray-400 hover:bg-gray-100">
          <ImagePlus size={18} />
        </button>
        <button
          type="submit"
          disabled={!draft.trim()}
          aria-label="전송"
          className="flex shrink-0 items-center justify-center rounded-full bg-primary p-2 text-white transition-colors hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-50"
        >
          <Send size={16} />
        </button>
      </form>
    </div>
  );
};

export default ChatConversationPanel;
