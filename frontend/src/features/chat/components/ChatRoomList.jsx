"use client";

import { useMemo, useState } from "react";
import { Search } from "lucide-react";

import ChatRoomItem from "@/features/chat/components/ChatRoomItem";
import { FILTER_GROUPS, getRoomDisplayName } from "@/features/chat/mockData";

const ChatRoomList = ({ rooms, selectedRoomId, onSelectRoom }) => {
  const [activeGroup, setActiveGroup] = useState("전체");
  const [query, setQuery] = useState("");

  const filteredRooms = useMemo(() => {
    return rooms.filter((room) => {
      const matchesGroup =
        activeGroup === "전체" ||
        (activeGroup === "챌린지방" && room.chatRoomType === "CHALLENGE") ||
        (activeGroup === "1:1 채팅" && room.chatRoomType === "DIRECT");

      const matchesQuery = getRoomDisplayName(room).toLowerCase().includes(query.trim().toLowerCase());

      return matchesGroup && matchesQuery;
    });
  }, [rooms, activeGroup, query]);

  return (
    <div className="flex h-full w-full flex-col">
      <div className="flex shrink-0 gap-2">
        {FILTER_GROUPS.map((group) => (
          <button
            key={group}
            type="button"
            onClick={() => setActiveGroup(group)}
            className={`rounded-full px-4 py-1.5 text-sm font-medium transition-colors ${
              activeGroup === group
                ? "bg-primary text-white"
                : "border border-gray-200 bg-white text-gray-600 hover:bg-gray-50"
            }`}
          >
            {group}
          </button>
        ))}
      </div>

      <div className="mt-4 flex shrink-0 items-center gap-2 rounded-xl border border-gray-100 bg-surface px-3 py-2.5 shadow-sm">
        <Search size={16} className="shrink-0 text-gray-400" />
        <input
          type="text"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="채팅방 검색"
          className="w-full bg-transparent text-sm text-gray-700 outline-none placeholder:text-gray-400"
        />
      </div>

      <div className="mt-3 flex min-h-0 flex-1 flex-col gap-1 overflow-y-auto">
        {filteredRooms.length === 0 && (
          <p className="py-12 text-center text-sm text-gray-400">채팅방이 없습니다.</p>
        )}
        {filteredRooms.map((room) => (
          <ChatRoomItem
            key={room.chatRoomId}
            room={room}
            selected={room.chatRoomId === selectedRoomId}
            onSelect={onSelectRoom}
          />
        ))}
      </div>
    </div>
  );
};

export default ChatRoomList;
