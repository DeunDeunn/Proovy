export const formatChallengePeriod = (startDate, endDate) => {
  if (!startDate || !endDate) return "";

  const start = new Date(startDate);
  const end = new Date(endDate);
  const totalDays = Math.round((end - start) / (1000 * 60 * 60 * 24)) + 1;

  const format = (date) =>
    `${String(date.getMonth() + 1).padStart(2, "0")}.${String(date.getDate()).padStart(2, "0")}`;

  return `${format(start)} ~ ${format(end)} (${totalDays}일)`;
};

export const formatRelativeTime = (value) => {
  if (!value) return "";

  const diffMs = Date.now() - new Date(value).getTime();
  const diffMinutes = Math.floor(diffMs / (1000 * 60));

  if (diffMinutes < 1) return "방금 전";
  if (diffMinutes < 60) return `${diffMinutes}분 전`;

  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) return `${diffHours}시간 전`;

  const diffDays = Math.floor(diffHours / 24);
  return `${diffDays}일 전`;
};
