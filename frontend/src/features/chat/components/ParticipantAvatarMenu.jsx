"use client";

import { useCallback, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { MessageSquare, User } from "lucide-react";

import { useStartDirectChat } from "@/features/chat/hooks/chatHooks";
import { useDismissable } from "@/features/chat/useDismissable";

// 채팅 참여자 아바타(children)를 감싸서, 클릭 시 프로필 이동/1:1 채팅 시작 드롭다운을 띄운다.
const ParticipantAvatarMenu = ({ userId, canStartChat, children }) => {
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
        className="rounded-full"
      >
        {children}
      </button>

      {isOpen && (
        <div
          role="menu"
          className="absolute left-0 top-full z-30 mt-1 w-40 overflow-hidden rounded-lg border border-gray-200 bg-white py-1 shadow-lg"
        >
          <button
            type="button"
            role="menuitem"
            onClick={() => {
              setIsOpen(false);
              router.push(`/users/${userId}`);
            }}
            className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-gray-700 hover:bg-gray-50"
          >
            <User size={15} aria-hidden="true" />
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
              className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm text-gray-700 hover:bg-gray-50"
            >
              <MessageSquare size={15} aria-hidden="true" />
              1:1 채팅 시작
            </button>
          )}
        </div>
      )}
    </div>
  );
};

export default ParticipantAvatarMenu;
