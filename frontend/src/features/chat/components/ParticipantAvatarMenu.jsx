"use client";

import { useCallback, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { MessageSquare, User } from "lucide-react";

import ProfileAvatar from "@/components/ui/ProfileAvatar";
import { useStartDirectChat } from "@/features/chat/hooks/chatHooks";
import { useDismissable } from "@/features/chat/useDismissable";

// 채팅 참여자 아바타(children)를 감싸서, 클릭 시 프로필 이동/1:1 채팅 시작 드롭다운을 띄운다.
const ParticipantAvatarMenu = ({ userId, nickname, profileImageUrl, canStartChat, children }) => {
  const router = useRouter();
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef(null);
  const closeMenu = useCallback(() => setIsOpen(false), []);
  useDismissable(isOpen, menuRef, closeMenu);
  const { startChat } = useStartDirectChat();

  return (
    <div ref={menuRef} className="relative inline-flex shrink-0">
      <button
        type="button"
        onClick={() => setIsOpen((open) => !open)}
        aria-label="참여자 메뉴"
        aria-expanded={isOpen}
        className="rounded-full transition-opacity hover:opacity-80"
      >
        {children}
      </button>

      {isOpen && (
        <div
          role="menu"
          className="animate-[dropdown-in_120ms_ease-out] absolute left-0 top-full z-30 mt-2 w-56 origin-top-left overflow-hidden rounded-2xl border border-gray-100 bg-white p-1.5 shadow-xl shadow-black/5"
        >
          {nickname && (
            <>
              <div className="flex items-center gap-2.5 px-2.5 py-2">
                <ProfileAvatar nickname={nickname} profileImageUrl={profileImageUrl} size="h-9 w-9" />
                <p className="truncate text-sm font-semibold text-gray-900">{nickname}</p>
              </div>
              <div className="my-1 h-px bg-gray-100" />
            </>
          )}

          <button
            type="button"
            role="menuitem"
            onClick={() => {
              setIsOpen(false);
              router.push(`/users/${userId}`);
            }}
            className="flex w-full items-center gap-2.5 rounded-xl px-2.5 py-2 text-left text-sm text-gray-700 transition-colors hover:bg-gray-50"
          >
            <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-gray-100 text-gray-500">
              <User size={14} aria-hidden="true" />
            </span>
            프로필로 이동
          </button>
          {canStartChat && (
            <button
              type="button"
              role="menuitem"
              onClick={() => {
                setIsOpen(false);
                startChat(userId);
              }}
              className="flex w-full items-center gap-2.5 rounded-xl px-2.5 py-2 text-left text-sm text-gray-700 transition-colors hover:bg-gray-50"
            >
              <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-primary-light text-primary">
                <MessageSquare size={14} aria-hidden="true" />
              </span>
              1:1 채팅 시작
            </button>
          )}
        </div>
      )}
    </div>
  );
};

export default ParticipantAvatarMenu;
