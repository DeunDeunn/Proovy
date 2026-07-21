"use client";

import { useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import { X } from "lucide-react";

import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";
import { useChatRooms, useShareCertificationToChatRoom } from "@/features/chat/hooks/chatHooks";
import { getAvatarColor, getRoomDisplayName } from "@/features/chat/mockData";

const FOCUSABLE_SELECTOR =
  'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

const ShareToChatDialog = ({ certificationId, onClose }) => {
  const router = useRouter();
  const { data: roomsData, isLoading, isError, error } = useChatRooms();
  const shareMutation = useShareCertificationToChatRoom();
  const rooms = roomsData?.content ?? [];
  const dialogRef = useRef(null);
  const triggerElementRef = useRef(null);

  // 열릴 때 포커스를 다이얼로그 안으로 옮기고(안 그러면 트리거 버튼에 남아있어 Escape/Tab이
  // 배경으로 전달됨), 닫힐 때는 원래 포커스였던 요소(공유 버튼 등)로 되돌린다.
  useEffect(() => {
    triggerElementRef.current = document.activeElement;
    dialogRef.current?.querySelector(FOCUSABLE_SELECTOR)?.focus();

    return () => {
      if (triggerElementRef.current instanceof HTMLElement) triggerElementRef.current.focus();
    };
  }, []);

  const handleKeyDown = (event) => {
    if (event.key === "Escape") {
      if (shareMutation.isPending) return;
      event.preventDefault();
      onClose();
      return;
    }

    if (event.key !== "Tab") return;

    const focusable = dialogRef.current?.querySelectorAll(FOCUSABLE_SELECTOR);
    if (!focusable || focusable.length === 0) return;

    const first = focusable[0];
    const last = focusable[focusable.length - 1];

    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  };

  const handleShare = (chatRoomId) => {
    shareMutation.mutate(
      { chatRoomId, certificationId },
      {
        onSuccess: () => {
          onClose();
          router.push(`/chat?roomId=${chatRoomId}`);
        },
      }
    );
  };

  return (
    <div role="presentation" className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4">
      <section
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="share-to-chat-dialog-title"
        onKeyDown={handleKeyDown}
        className="flex max-h-[70vh] w-full max-w-sm flex-col overflow-hidden rounded-xl bg-white shadow-xl"
      >
        <div className="flex shrink-0 items-center justify-between border-b border-gray-100 px-5 py-4">
          <h2 id="share-to-chat-dialog-title" className="text-base font-bold text-gray-900">
            인증글 공유하기
          </h2>
          <button
            type="button"
            onClick={onClose}
            disabled={shareMutation.isPending}
            aria-label="닫기"
            className="rounded-lg p-1 text-gray-400 hover:bg-gray-100 disabled:cursor-not-allowed"
          >
            <X size={18} />
          </button>
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto px-2 py-2">
          {isLoading && <Loading label="채팅방을 불러오는 중..." />}
          {isError && (
            <div className="px-3 py-2">
              <ErrorMessage error={error} />
            </div>
          )}
          {!isLoading && !isError && rooms.length === 0 && (
            <p className="px-3 py-6 text-center text-sm text-gray-400">공유할 수 있는 채팅방이 없습니다.</p>
          )}
          {rooms.map((room) => {
            const avatarColor = getAvatarColor(room.chatRoomId);
            const displayName = getRoomDisplayName(room);
            const isSharingHere = shareMutation.isPending && shareMutation.variables?.chatRoomId === room.chatRoomId;

            return (
              <button
                key={room.chatRoomId}
                type="button"
                onClick={() => handleShare(room.chatRoomId)}
                disabled={shareMutation.isPending}
                className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
              >
                <span
                  className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-sm font-semibold ${avatarColor.bg} ${avatarColor.text}`}
                >
                  {displayName.slice(0, 1)}
                </span>
                <span className="min-w-0 flex-1 truncate text-sm text-gray-800">{displayName}</span>
                {isSharingHere && <span className="shrink-0 text-xs text-gray-400">보내는 중...</span>}
              </button>
            );
          })}
        </div>

        {shareMutation.isError && (
          <div className="shrink-0 border-t border-gray-100 px-5 py-3">
            <ErrorMessage error={shareMutation.error} />
          </div>
        )}
      </section>
    </div>
  );
};

export default ShareToChatDialog;
