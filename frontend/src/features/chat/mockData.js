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

const MINUTE = 60 * 1000;
const HOUR = 60 * MINUTE;

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

const ROOM_TEMPLATES = [
  {
    chatRoomType: "CHALLENGE",
    challengeTitle: "매일 30분 러닝",
    participantCount: 24,
    lastMessage: { senderNickname: "민지", content: "오늘도 수고하셨어요! 내일도 화이팅🔥" },
    unreadCount: 8,
    createdAt: new Date(Date.now() - 30 * MINUTE),
  },
  {
    chatRoomType: "DIRECT",
    directChatPartner: { userId: 101, nickname: "이지훈" },
    lastMessage: { senderNickname: "이지훈", content: "네! 감사합니다 🙏" },
    unreadCount: 1,
    createdAt: new Date(Date.now() - 3.5 * HOUR),
  },
  {
    chatRoomType: "DIRECT",
    directChatPartner: { userId: 102, nickname: "박민지" },
    lastMessage: { senderNickname: "박민지", content: "인증 사진 확인 부탁드려요~" },
    unreadCount: 1,
    createdAt: new Date(Date.now() - 5 * HOUR),
  },
];

export const createMockChatRooms = () =>
  ROOM_TEMPLATES.map((template, index) => ({
    chatRoomId: index + 1,
    ...template,
  }));
