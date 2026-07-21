export const FILTER_GROUPS = ["전체", "챌린지방", "1:1 채팅"];

export const CHAT_ROOM_TYPES = {
  CHALLENGE: { group: "챌린지방", tagLabel: "챌린지방" },
  DIRECT: { group: "1:1 채팅", tagLabel: "1:1 채팅" },
};

const AVATAR_PALETTE = [
  { bg: "bg-blue-100", text: "text-blue-700" },
  { bg: "bg-green-100", text: "text-green-700" },
  { bg: "bg-amber-100", text: "text-amber-700" },
  { bg: "bg-purple-100", text: "text-purple-700" },
  { bg: "bg-pink-100", text: "text-pink-700" },
  { bg: "bg-cyan-100", text: "text-cyan-700" },
];

export const getAvatarColor = (id) => AVATAR_PALETTE[id % AVATAR_PALETTE.length];

export const getRoomDisplayName = (room) =>
  room.chatRoomType === "CHALLENGE" ? room.challengeTitle : room.directChatPartner.nickname;

const isSameDay = (a, b) =>
  a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();

export const formatChatTime = (date) => {
  const now = new Date();
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);

  if (isSameDay(date, now)) {
    const hours = date.getHours();
    const period = hours < 12 ? "오전" : "오후";
    const displayHour = hours % 12 === 0 ? 12 : hours % 12;
    const minutes = String(date.getMinutes()).padStart(2, "0");
    return `${period} ${displayHour}:${minutes}`;
  }
  if (isSameDay(date, yesterday)) return "어제";
  return `${date.getMonth() + 1}월 ${date.getDate()}일`;
};
