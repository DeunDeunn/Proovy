const formatDate = (date) => {
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const dd = String(date.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
};

// 시작일은 최소 내일부터 선택 가능 (오늘 시작은 불가) - 로컬 타임존 기준으로 계산
export const getMinStartDate = () => {
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  return formatDate(tomorrow);
};

// 종료일은 시작일보다 하루 이상 뒤여야 함
export const getMinEndDate = (startDate) => {
  if (!startDate) return undefined;
  // "yyyy-mm-dd"를 new Date(string)으로 바로 넘기면 UTC 자정으로 해석돼
  // getDate()/setDate()의 로컬 기준과 어긋나 UTC보다 느린 시간대에서 하루 계산이 틀어진다
  const [year, month, day] = startDate.split("-").map(Number);
  const dayAfterStart = new Date(year, month - 1, day + 1);
  return formatDate(dayAfterStart);
};
