"use client";

import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { MessageCircle } from "lucide-react";

import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";
import LoginRequiredModal from "@/components/ui/LoginRequiredModal";
import { useMe } from "@/features/auth/hooks";
import ChatConversationPanel from "@/features/chat/components/ChatConversationPanel";
import ChatRoomList from "@/features/chat/components/ChatRoomList";
import { useChatRoomsSync, useMarkRoomRead } from "@/features/chat/hooks/chatHooks";
import { useChatStore } from "@/features/chat/store";

const LIST_WIDTH_REM = 24;
const PANEL_TRANSITION_MS = 300;

const ChatPageContent = () => {
  const router = useRouter();
  const searchParams = useSearchParams();
  const roomIdParam = searchParams.get("roomId");

  const { data: me, isLoading: isMeLoading, isError: isMeError, error: meError } = useMe();
  // 로그아웃 시 setQueryData(["auth","me"], null)로 me가 null이 되는데, 이건 에러가 아니라
  // 성공 상태라서 그 케이스도 명시적으로 잡아줘야 페이지를 보고 있는 도중 로그아웃해도 바로 막힌다.
  const isUnauthorized = (isMeError && meError?.status === 401) || (!isMeLoading && me === null);

  const {
    isLoading: isRoomsLoading,
    isError: isRoomsError,
    error: roomsError,
  } = useChatRoomsSync({ enabled: !!me });
  const rooms = useChatStore((state) => state.rooms);
  const messagesByRoomId = useChatStore((state) => state.messagesByRoomId);
  const sendMessage = useChatStore((state) => state.sendMessage);
  const { mutate: markRoomRead } = useMarkRoomRead();

  const [selectedRoomId, setSelectedRoomId] = useState(null);
  const [panelRoomId, setPanelRoomId] = useState(null);
  const [pendingOpenId, setPendingOpenId] = useState(null);

  const panelOpen = selectedRoomId != null;

  // 닫힌 상태에서 새로 열 때는 먼저 닫힌 위치로 마운트한 뒤 다음 프레임에 열어야 슬라이드인 트랜지션이 재생됨
  useEffect(() => {
    if (pendingOpenId == null) return;

    // 브라우저가 "닫힌 위치"로 한 번 페인트하도록 프레임을 한 번 더 기다린 뒤 연다
    let innerFrame;
    const outerFrame = requestAnimationFrame(() => {
      innerFrame = requestAnimationFrame(() => {
        setSelectedRoomId(pendingOpenId);
        setPendingOpenId(null);
      });
    });

    return () => {
      cancelAnimationFrame(outerFrame);
      cancelAnimationFrame(innerFrame);
    };
  }, [pendingOpenId]);

  // 패널이 닫히는 동안(슬라이드 아웃) 내용이 먼저 사라지지 않도록, 트랜지션 시간만큼 지연 후 언마운트
  useEffect(() => {
    if (panelOpen || panelRoomId == null) return;

    const timer = setTimeout(() => setPanelRoomId(null), PANEL_TRANSITION_MS);
    return () => clearTimeout(timer);
  }, [panelOpen, panelRoomId]);

  const handleSelectRoom = (chatRoomId) => {
    markRoomRead(chatRoomId);
    setPanelRoomId(chatRoomId);

    if (selectedRoomId != null) {
      // 이미 열려있는 상태에서 다른 방 클릭: 슬라이드 없이 내용만 즉시 교체
      setSelectedRoomId(chatRoomId);
    } else {
      // 닫힌 상태에서 여는 경우: 닫힌 위치로 먼저 마운트 후 애니메이션으로 열기
      setPendingOpenId(chatRoomId);
    }
  };

  // ?roomId= 로 진입한 경우, 목록 로딩 후 해당 방을 자동으로 연다
  useEffect(() => {
    if (!roomIdParam) return;

    const targetRoomId = Number(roomIdParam);
    if (!rooms.some((room) => room.chatRoomId === targetRoomId)) return;

    // URL(roomId)이라는 외부 상태와 동기화하는 효과라 setState가 필요함
    handleSelectRoom(targetRoomId); // eslint-disable-line react-hooks/set-state-in-effect
    router.replace("/chat");
  }, [roomIdParam, rooms]); // eslint-disable-line react-hooks/exhaustive-deps

  const panelRoom = rooms.find((room) => room.chatRoomId === panelRoomId) ?? null;
  const panelMessages = panelRoomId ? (messagesByRoomId[panelRoomId] ?? []) : [];

  if (isMeLoading) return null;
  if (isUnauthorized)
    return <LoginRequiredModal description="채팅은 로그인 후 이용할 수 있어요." />;
  if (isMeError) {
    return (
      <div className="flex h-[calc(100vh-4rem)] items-center justify-center px-4">
        <ErrorMessage error={meError} />
      </div>
    );
  }
  if (isRoomsLoading) {
    return (
      <div className="flex h-[calc(100vh-4rem)] items-center justify-center px-4">
        <Loading label="채팅방을 불러오는 중..." />
      </div>
    );
  }
  if (isRoomsError) {
    return (
      <div className="flex h-[calc(100vh-4rem)] items-center justify-center px-4">
        <ErrorMessage error={roomsError} />
      </div>
    );
  }

  return (
    <div className="mx-auto flex h-[calc(100vh-4rem)] max-w-[1440px] flex-col">
      <h1 className="flex shrink-0 items-center gap-2 text-2xl font-bold text-gray-900">
        <MessageCircle size={24} />
        채팅
      </h1>

      <div className="mt-5 flex min-h-0 flex-1 gap-4">
        <div className="shrink-0" style={{ width: `${LIST_WIDTH_REM}rem` }}>
          <ChatRoomList
            rooms={rooms}
            selectedRoomId={selectedRoomId}
            onSelectRoom={handleSelectRoom}
          />
        </div>

        <div className="relative min-w-0 flex-1 overflow-hidden">
          {!panelRoom && (
            <div className="flex h-full flex-col items-center justify-center gap-2 rounded-2xl border border-gray-100 bg-surface text-gray-400 shadow-xl shadow-black/5">
              <MessageCircle size={28} className="text-gray-300" />
              <p className="text-sm">채팅방을 선택해주세요</p>
            </div>
          )}

          {panelRoom && (
            <div
              className={`absolute inset-0 transition-[transform,opacity] duration-300 ease-out ${
                panelOpen ? "translate-x-0 opacity-100" : "translate-x-2 opacity-0"
              }`}
            >
              <ChatConversationPanel
                key={panelRoom.chatRoomId}
                room={panelRoom}
                messages={panelMessages}
                onSendMessage={(content) => sendMessage(panelRoom.chatRoomId, content)}
                onClose={() => setSelectedRoomId(null)}
              />
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

const ChatPage = () => (
  <Suspense fallback={null}>
    <ChatPageContent />
  </Suspense>
);

export default ChatPage;
