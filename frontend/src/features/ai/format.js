export const formatAiTicketPrice = (value) => `${Number(value ?? 0).toLocaleString()} 캐시`;

export const formatAiTicketPlanName = (value) =>
  String(value ?? "")
    .replace(/\s*AI\s*/i, " ")
    .replace(/\s+/g, " ")
    .trim();

export const formatAiDateTime = (value) => {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
};

export const getRemainingText = (expiredAt) => {
  if (!expiredAt) return "";
  const expiredAtTime = new Date(expiredAt).getTime();
  if (Number.isNaN(expiredAtTime)) return "";

  const remainingMs = expiredAtTime - Date.now();
  if (remainingMs <= 0) return "만료됨";

  const remainingHours = Math.ceil(remainingMs / (1000 * 60 * 60));
  if (remainingHours < 24) return `${remainingHours}시간 남음`;
  return `${Math.ceil(remainingHours / 24)}일 남음`;
};

export const AI_TICKET_HISTORY_META = {
  PURCHASED: { label: "구매", variant: "primary" },
  USED: { label: "AI 검수 사용", variant: "success" },
  EXPIRED: { label: "기간 만료", variant: "gray" },
};
