// 연속 공백을 하나로 합쳐서, 스페이스만 잔뜩 채워 글자수를 늘리는 걸 막는다
export const normalizeText = (value) => value.replace(/\s+/g, " ").trim();

// 최소 글자수를 채워도 "..........." 처럼 문자/숫자가 하나도 없으면 의미 있는 입력이 아니므로 함께 검사한다
export const hasMeaningfulContent = (value, minLength) => {
  const normalized = normalizeText(value);
  return normalized.length >= minLength && /[\p{L}\p{N}]/u.test(normalized);
};
