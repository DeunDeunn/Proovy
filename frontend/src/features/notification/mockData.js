import { FilePlus, FileCheck, FileX, Coins, BadgeCheck, BadgeX, HandCoins } from "lucide-react";

export const FILTER_GROUPS = ["전체", "인증", "정산", "기타"];

export const NOTIFICATION_TYPES = {
  VERIFICATION_SUBMITTED: {
    group: "인증",
    tagLabel: "인증 제출",
    badgeVariant: "primary",
    icon: FilePlus,
    iconClass: "bg-primary-light text-primary",
  },
  VERIFICATION_APPROVED: {
    group: "인증",
    tagLabel: "인증 승인",
    badgeVariant: "success",
    icon: FileCheck,
    iconClass: "bg-green-50 text-success",
  },
  VERIFICATION_REJECTED: {
    group: "인증",
    tagLabel: "인증 거절",
    badgeVariant: "danger",
    icon: FileX,
    iconClass: "bg-red-50 text-danger",
  },
  SETTLEMENT_COMPLETED: {
    group: "정산",
    tagLabel: "정산 완료",
    badgeVariant: "success",
    icon: Coins,
    iconClass: "bg-green-50 text-success",
  },
  HOST_REVENUE_PAID: {
    group: "정산",
    tagLabel: "정산 지급",
    badgeVariant: "success",
    icon: HandCoins,
    iconClass: "bg-green-50 text-success",
  },
  BADGE_APPROVED: {
    group: "기타",
    tagLabel: "뱃지 승인",
    badgeVariant: "success",
    icon: BadgeCheck,
    iconClass: "bg-green-50 text-success",
  },
  BADGE_REJECTED: {
    group: "기타",
    tagLabel: "뱃지 거절",
    badgeVariant: "danger",
    icon: BadgeX,
    iconClass: "bg-red-50 text-danger",
  },
};

const TEMPLATES = [
  {
    type: "VERIFICATION_SUBMITTED",
    title: "새 인증이 등록되었습니다.",
    description: "참여자가 인증글을 등록했습니다. 확인해주세요!",
  },
  {
    type: "VERIFICATION_APPROVED",
    title: "인증이 승인되었습니다.",
    description: "챌린지 인증이 승인되었습니다. 연속 인증을 응원해요!",
  },
  {
    type: "VERIFICATION_REJECTED",
    title: "인증이 거절되었습니다.",
    description: "챌린지 인증이 거절되었습니다. 사유를 확인해주세요.",
  },
  {
    type: "SETTLEMENT_COMPLETED",
    title: "챌린지 정산이 완료되어 수익이 지급되었습니다.",
    description: "챌린지 정산이 완료되었습니다. 결과를 확인해보세요.",
  },
  {
    type: "HOST_REVENUE_PAID",
    title: "방장 수수료가 지급되었습니다.",
    description: "챌린지 정산 후 방장 수수료가 지급되었습니다.",
  },
  {
    type: "BADGE_APPROVED",
    title: "뱃지 신청이 승인되었습니다.",
    description: "신청하신 뱃지가 승인되었습니다. 축하드려요!",
  },
  {
    type: "BADGE_REJECTED",
    title: "뱃지 신청이 거절되었습니다.",
    description: "신청하신 뱃지가 거절되었습니다. 사유를 확인해주세요.",
  },
];

const MINUTE = 60 * 1000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

const formatRelativeTime = (date) => {
  const diff = Date.now() - date.getTime();

  if (diff < HOUR) return `${Math.max(1, Math.round(diff / MINUTE))}분 전`;
  if (diff < DAY) return `${Math.round(diff / HOUR)}시간 전`;
  return `${Math.round(diff / DAY)}일 전`;
};

const isSameDay = (a, b) =>
  a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();

export const formatDateLabel = (date) => {
  const now = new Date();
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);

  if (isSameDay(date, now)) return "오늘";
  if (isSameDay(date, yesterday)) return "어제";
  return `${date.getMonth() + 1}월 ${date.getDate()}일`;
};

const TOTAL_MOCK_COUNT = 26;
const UNREAD_COUNT = 6;

// 최근 6개는 몇 분~몇 시간 단위로, 그 이후는 날짜 구분선 데모를 위해 며칠씩 벌려서 생성한다.
const offsetFor = (index) =>
  index < UNREAD_COUNT ? index * 20 * MINUTE : UNREAD_COUNT * 20 * MINUTE + (index - UNREAD_COUNT) * 7 * HOUR;

export const createMockNotifications = () => {
  return Array.from({ length: TOTAL_MOCK_COUNT }, (_, index) => {
    const template = TEMPLATES[index % TEMPLATES.length];
    const createdAt = new Date(Date.now() - offsetFor(index));

    return {
      id: index + 1,
      ...template,
      group: NOTIFICATION_TYPES[template.type].group,
      createdAt,
      timeAgo: formatRelativeTime(createdAt),
      read: index >= UNREAD_COUNT,
    };
  });
};
