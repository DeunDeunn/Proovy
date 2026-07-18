"use client";

import { useEffect, useState } from "react";
import { MessageCircle } from "lucide-react";

import ChatConversationPanel from "@/features/chat/ChatConversationPanel";
import ChatRoomList from "@/features/chat/ChatRoomList";
import { useChatStore } from "@/features/chat/store";

const LIST_WIDTH_OPEN_REM = 20;
const PANEL_TRANSITION_MS = 300;

const ChatPage = () => {
  const rooms = useChatStore((state) => state.rooms);
  const messagesByRoomId = useChatStore((state) => state.messagesByRoomId);
  const markRoomRead = useChatStore((state) => state.markRoomRead);
  const sendMessage = useChatStore((state) => state.sendMessage);

  const [selectedRoomId, setSelectedRoomId] = useState(null);
  const [panelRoomId, setPanelRoomId] = useState(null);
  const [pendingOpenId, setPendingOpenId] = useState(null);

  const panelOpen = selectedRoomId != null;
  const isRoomMode = panelRoomId != null;

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

  const panelRoom = rooms.find((room) => room.chatRoomId === panelRoomId) ?? null;
  const panelMessages = panelRoomId ? (messagesByRoomId[panelRoomId] ?? []) : [];

  return (
    <div className="flex h-[calc(100vh-4rem)] flex-col">
      <h1 className="mx-auto flex w-full max-w-3xl shrink-0 items-center gap-2 text-xl font-bold text-gray-900">
        <MessageCircle size={22} />
        채팅
      </h1>

      <div className="mt-5 min-h-0 flex-1">
        <div className="relative h-full overflow-hidden">
          <div
            className="h-full transition-[width,margin-left] duration-300 ease-out"
            style={{
              width: isRoomMode ? `${LIST_WIDTH_OPEN_REM}rem` : "min(48rem, 100%)",
              marginLeft: isRoomMode ? "0px" : "max(0px, calc((100% - 48rem) / 2))",
            }}
          >
            <ChatRoomList rooms={rooms} selectedRoomId={selectedRoomId} onSelectRoom={handleSelectRoom} />
          </div>

          {panelRoom && (
            <div
              className={`absolute inset-y-0 right-0 pl-4 transition-transform duration-300 ease-out ${
                panelOpen ? "translate-x-0" : "translate-x-full"
              }`}
              style={{ width: `calc(100% - ${LIST_WIDTH_OPEN_REM}rem)` }}
            >
              <ChatConversationPanel
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

export default ChatPage;
