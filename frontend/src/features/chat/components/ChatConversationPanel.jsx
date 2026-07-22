"use client";
/* eslint-disable @next/next/no-img-element -- S3 썸네일/프로필 이미지 URL은 현재 next/image 설정 대상이 아니다. */

import { useCallback, useEffect, useRef, useState } from "react";
import { BadgeCheck, ImagePlus, Plus, Send, Users, X } from "lucide-react";

import { useMe } from "@/features/auth/hooks";
import { getSocketClient } from "@/features/chat/api/chatSocket";
import MessageBubble from "@/features/chat/components/MessageBubble";
import ParticipantAvatarMenu from "@/features/chat/components/ParticipantAvatarMenu";
import ProfileAvatar from "@/components/ui/ProfileAvatar";
import {
  useChatRoomHistory,
  useChatRoomMembers,
  useChatRoomSubscription,
  useDeleteChatMessage,
  useMarkRoomRead,
  useSendChatAttachment,
} from "@/features/chat/hooks/chatHooks";
import { getAvatarColor, getRoomDisplayName } from "@/features/chat/mockData";
import { useChatStore } from "@/features/chat/store";
import { useDismissable } from "@/features/chat/useDismissable";

const SCROLL_TOP_THRESHOLD = 80;

const ChatConversationPanel = ({ room, messages, onSendMessage, onClose }) => {
  const { data: me } = useMe();
  const receiveMessage = useChatStore((state) => state.receiveMessage);
  const { loadMore, hasMore, isLoadingInitial, isLoadingMore, error } = useChatRoomHistory(
    room.chatRoomId
  );

  const [isConnected, setIsConnected] = useState(() => getSocketClient()?.connected ?? false);
  const handleDisconnect = useCallback(() => setIsConnected(false), []);
  const handleConnected = useCallback(() => setIsConnected(true), []);
  useChatRoomSubscription(room.chatRoomId, receiveMessage, {
    onDisconnect: handleDisconnect,
    onConnected: handleConnected,
  });

  const [draft, setDraft] = useState("");
  const [deleteError, setDeleteError] = useState(null);
  const [attachmentError, setAttachmentError] = useState(null);
  const listEndRef = useRef(null);
  const scrollContainerRef = useRef(null);
  const fileInputRef = useRef(null);
  const imageInputRef = useRef(null);
  const deleteMessageMutation = useDeleteChatMessage();
  const attachmentMutation = useSendChatAttachment(room.chatRoomId);
  const { mutate: markRoomRead } = useMarkRoomRead();

  const isChallenge = room.chatRoomType === "CHALLENGE";
  const { data: members } = useChatRoomMembers(room.chatRoomId, { enabled: isChallenge });
  const [isMemberListOpen, setIsMemberListOpen] = useState(false);
  const memberListRef = useRef(null);
  const closeMemberList = useCallback(() => setIsMemberListOpen(false), []);
  useDismissable(isMemberListOpen, memberListRef, closeMemberList);

  const displayName = getRoomDisplayName(room);
  const avatarColor = getAvatarColor(room.chatRoomId);
  const lastMessageId = messages[messages.length - 1]?.messageId;

  // 새 메시지가 맨 끝에 추가됐을 때만 바닥으로 스크롤 (이전 메시지를 위에 추가하는 경우는 제외)
  useEffect(() => {
    listEndRef.current?.scrollIntoView({ block: "end" });
  }, [lastMessageId]);

  // 방을 열어둔 채로 새 메시지가 도착해도 읽음 커서를 계속 따라가도록 다시 읽음 처리한다.
  // 입장 시 1회만 처리하면, 그 이후 들어오는 메시지에 대해 (1) 내 안 읽은 개수가 실제로는
  // 보고 있는데도 계속 올라가고 (2) 상대방 화면에 내 메시지의 "읽음" 표시가 실시간으로
  // 반영되지 않는 문제가 있었다.
  useEffect(() => {
    if (lastMessageId == null) return;
    markRoomRead(room.chatRoomId);
  }, [lastMessageId, room.chatRoomId, markRoomRead]);

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
    if (!trimmed || !isConnected) return;

    onSendMessage(trimmed);
    setDraft("");
  };

  const handleDeleteMessage = (messageId) => {
    setDeleteError(null);
    deleteMessageMutation.mutate(messageId, {
      onError: () => setDeleteError("메시지를 삭제하지 못했습니다."),
    });
  };

  const handleAttachmentSelected = (event) => {
    const file = event.target.files?.[0];
    event.target.value = ""; // 같은 파일을 다시 선택해도 onChange가 다시 발생하도록 초기화

    if (!file) return;

    // "파일 첨부" 버튼은 accept 제한이 없어 이미지도 고를 수 있으므로,
    // 버튼 종류가 아니라 실제 파일 MIME 타입으로 메시지 타입을 판별한다.
    const messageType = file.type.startsWith("image/") ? "IMAGE" : "FILE";

    setAttachmentError(null);
    attachmentMutation.mutate(
      { messageType, file },
      { onError: (error) => setAttachmentError(error?.message ?? "파일을 보내지 못했습니다.") }
    );
  };

  return (
    <div className="flex h-full w-full flex-col overflow-hidden rounded-xl border border-gray-200 bg-surface">
      <div className="flex shrink-0 items-center gap-3 border-b border-gray-100 px-5 py-4">
        {isChallenge ? (
          room.challengeThumbnailUrl ? (
            <img
              src={room.challengeThumbnailUrl}
              alt=""
              className="h-10 w-10 shrink-0 rounded-full object-cover"
            />
          ) : (
            <div
              className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-sm font-semibold ${avatarColor.bg} ${avatarColor.text}`}
            >
              {displayName.slice(0, 1)}
            </div>
          )
        ) : (
          <ProfileAvatar
            nickname={displayName}
            profileImageUrl={room.directChatPartner?.profileImageUrl}
            size="h-10 w-10"
          />
        )}

        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <p className="truncate text-sm font-semibold text-gray-900">{displayName}</p>
            {isChallenge && (
              <span className="rounded-full bg-primary-light px-2 py-0.5 text-xs font-medium text-primary">
                챌린지방
              </span>
            )}
          </div>
          {isChallenge && members && (
            <p className="mt-0.5 text-xs text-gray-400">참여자 {members.length}명</p>
          )}
        </div>

        <div className="flex shrink-0 items-center gap-1 text-gray-400">
          {isChallenge && (
            <div ref={memberListRef} className="relative">
              <button
                type="button"
                aria-label="참여자 목록"
                aria-expanded={isMemberListOpen}
                onClick={() => setIsMemberListOpen((open) => !open)}
                className="rounded-lg p-2 hover:bg-gray-100"
              >
                <Users size={18} />
              </button>

              {isMemberListOpen && (
                <div
                  role="menu"
                  className="animate-[dropdown-in_120ms_ease-out] absolute right-0 top-10 z-20 max-h-72 w-56 origin-top-right overflow-y-auto rounded-2xl border border-gray-100 bg-white p-1.5 shadow-xl shadow-black/5"
                >
                  {!members && <p className="px-2.5 py-2 text-xs text-gray-400">불러오는 중...</p>}
                  {members?.length === 0 && (
                    <p className="px-2.5 py-2 text-xs text-gray-400">참여자가 없습니다.</p>
                  )}
                  {members?.map((member) => {
                    const isMe = member.userId === me?.id;
                    return (
                      <div
                        key={member.userId}
                        className={`flex items-center gap-2.5 rounded-xl px-2.5 py-2 transition-colors ${
                          isMe ? "bg-primary-light" : "hover:bg-gray-50"
                        }`}
                      >
                        <ParticipantAvatarMenu
                          userId={member.userId}
                          nickname={member.nickname}
                          profileImageUrl={member.profileImageUrl}
                          canStartChat={!isMe}
                        >
                          <ProfileAvatar
                            nickname={member.nickname}
                            profileImageUrl={member.profileImageUrl}
                            size="h-7 w-7"
                          />
                        </ParticipantAvatarMenu>
                        <span className="flex min-w-0 flex-1 items-center gap-1">
                          <span className="truncate text-sm text-gray-700">{member.nickname}</span>
                          {isMe && <span className="shrink-0 text-xs text-gray-400">(나)</span>}
                          {member.badgeApproved && (
                            <BadgeCheck
                              size={14}
                              className="shrink-0 fill-primary stroke-white"
                              aria-label="우수 인증자"
                            />
                          )}
                        </span>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}
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

      {!isConnected && (
        <p className="shrink-0 bg-red-50 px-5 py-1.5 text-center text-xs text-red-500">
          연결되지 않았습니다. 메시지를 보낼 수 없습니다.
        </p>
      )}

      {deleteError && (
        <p className="shrink-0 bg-red-50 px-5 py-1.5 text-center text-xs text-red-500">
          {deleteError}
        </p>
      )}

      {attachmentError && (
        <p className="shrink-0 bg-red-50 px-5 py-1.5 text-center text-xs text-red-500">
          {attachmentError}
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
              onDelete={handleDeleteMessage}
              isDeletePending={
                deleteMessageMutation.isPending &&
                deleteMessageMutation.variables === message.messageId
              }
            />
          );
        })}
        <div ref={listEndRef} />
      </div>

      <form
        onSubmit={handleSubmit}
        className="flex shrink-0 items-center gap-2 border-t border-gray-100 px-4 py-3"
      >
        <input
          ref={fileInputRef}
          type="file"
          className="hidden"
          onChange={handleAttachmentSelected}
        />
        <input
          ref={imageInputRef}
          type="file"
          accept="image/*"
          className="hidden"
          onChange={handleAttachmentSelected}
        />
        <button
          type="button"
          aria-label="파일 첨부"
          disabled={!isConnected || attachmentMutation.isPending}
          onClick={() => fileInputRef.current?.click()}
          className="shrink-0 rounded-lg p-2 text-gray-400 hover:bg-gray-100 disabled:cursor-not-allowed disabled:opacity-50"
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
          aria-label="이미지 첨부"
          disabled={!isConnected || attachmentMutation.isPending}
          onClick={() => imageInputRef.current?.click()}
          className="shrink-0 rounded-lg p-2 text-gray-400 hover:bg-gray-100 disabled:cursor-not-allowed disabled:opacity-50"
        >
          <ImagePlus size={18} />
        </button>
        <button
          type="submit"
          disabled={!draft.trim() || !isConnected}
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
