"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { ImagePlus, MoreVertical, Plus, Search, Send, Smile, Users, X } from "lucide-react";

import { useMe } from "@/features/auth/hooks";
import MessageBubble from "@/features/chat/components/MessageBubble";
import { useChatRoomHistory, useChatRoomSubscription } from "@/features/chat/hooks/chatHooks";
import { getAvatarColor, getRoomDisplayName } from "@/features/chat/mockData";
import { useChatStore } from "@/features/chat/store";

const SCROLL_TOP_THRESHOLD = 80;

const ChatConversationPanel = ({ room, messages, onSendMessage, onClose }) => {
  const { data: me } = useMe();
  const receiveMessage = useChatStore((state) => state.receiveMessage);
  const { loadMore, hasMore, isLoadingInitial, isLoadingMore, error } = useChatRoomHistory(
    room.chatRoomId
  );

  const [isDisconnected, setIsDisconnected] = useState(false);
  const handleDisconnect = useCallback(() => setIsDisconnected(true), []);
  const handleConnected = useCallback(() => setIsDisconnected(false), []);
  useChatRoomSubscription(room.chatRoomId, receiveMessage, {
    onDisconnect: handleDisconnect,
    onConnected: handleConnected,
  });

  const [draft, setDraft] = useState("");
  const listEndRef = useRef(null);
  const scrollContainerRef = useRef(null);

  const isChallenge = room.chatRoomType === "CHALLENGE";
  const displayName = getRoomDisplayName(room);
  const avatarColor = getAvatarColor(room.chatRoomId);
  const lastMessageId = messages[messages.length - 1]?.messageId;

  // 새 메시지가 맨 끝에 추가됐을 때만 바닥으로 스크롤 (이전 메시지를 위에 추가하는 경우는 제외)
  useEffect(() => {
    listEndRef.current?.scrollIntoView({ block: "end" });
  }, [lastMessageId]);

  const handleScroll = () => {
    const container = scrollContainerRef.current;
    if (!container || !hasMore || isLoadingMore) return;
    if (container.scrollTop > SCROLL_TOP_THRESHOLD) return;

    const prevScrollHeight = container.scrollHeight;
    const prevScrollTop = container.scrollTop;

    loadMore().then(() => {
      requestAnimationFrame(() => {
        const target = scrollContainerRef.current;
        if (!target) return;
        target.scrollTop = prevScrollTop + (target.scrollHeight - prevScrollHeight);
      });
    });
  };

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
          {isChallenge && (
            <p className="mt-0.5 text-xs text-gray-400">참여자 {room.participantCount}명</p>
          )}
        </div>

        <div className="flex shrink-0 items-center gap-1 text-gray-400">
          <button type="button" aria-label="대화 검색" className="rounded-lg p-2 hover:bg-gray-100">
            <Search size={18} />
          </button>
          {isChallenge && (
            <button
              type="button"
              aria-label="참여자 목록"
              className="rounded-lg p-2 hover:bg-gray-100"
            >
              <Users size={18} />
            </button>
          )}
          <button type="button" aria-label="더보기" className="rounded-lg p-2 hover:bg-gray-100">
            <MoreVertical size={18} />
          </button>
          <button
            type="button"
            onClick={onClose}
            aria-label="채팅방 닫기"
            className="rounded-lg p-2 hover:bg-gray-100"
          >
            <X size={18} />
          </button>
        </div>
      </div>

      {isDisconnected && (
        <p className="shrink-0 bg-red-50 px-5 py-1.5 text-center text-xs text-red-500">
          연결이 끊겼습니다. 재연결 시도 중...
        </p>
      )}

      <div
        ref={scrollContainerRef}
        onScroll={handleScroll}
        className="min-h-0 flex-1 space-y-4 overflow-y-auto px-5 py-4"
      >
        {isLoadingInitial && (
          <p className="py-4 text-center text-xs text-gray-400">메시지를 불러오는 중...</p>
        )}
        {!isLoadingInitial && error && (
          <p className="py-4 text-center text-xs text-red-500">메시지를 불러오지 못했습니다.</p>
        )}
        {!isLoadingInitial && !error && messages.length === 0 && (
          <p className="py-4 text-center text-xs text-gray-400">아직 주고받은 메시지가 없습니다.</p>
        )}
        {isLoadingMore && (
          <p className="py-1 text-center text-xs text-gray-400">이전 메시지 불러오는 중...</p>
        )}
        {messages.map((message, index) => {
          const isOwn = message.senderId === me?.id;
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

      <form
        onSubmit={handleSubmit}
        className="flex shrink-0 items-center gap-2 border-t border-gray-100 px-4 py-3"
      >
        <button
          type="button"
          aria-label="파일 첨부"
          className="shrink-0 rounded-lg p-2 text-gray-400 hover:bg-gray-100"
        >
          <Plus size={20} />
        </button>
        <input
          type="text"
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          placeholder="메시지 입력..."
          className="min-w-0 flex-1 bg-transparent text-sm text-gray-700 outline-none placeholder:text-gray-400"
        />
        <button
          type="button"
          aria-label="이모지"
          className="shrink-0 rounded-lg p-2 text-gray-400 hover:bg-gray-100"
        >
          <Smile size={18} />
        </button>
        <button
          type="button"
          aria-label="이미지 첨부"
          className="shrink-0 rounded-lg p-2 text-gray-400 hover:bg-gray-100"
        >
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
