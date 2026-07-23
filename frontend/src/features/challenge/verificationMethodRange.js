import { hasMeaningfulContent, normalizeText } from "./textValidation";

// AI 자동검수가 이 문구를 기준으로 판단하므로, 너무 짧으면 검수 기준이 모호해져 최소 글자수를 둔다
export const VERIFICATION_METHOD_MIN_LENGTH = 10;
export const VERIFICATION_METHOD_MAX_LENGTH = 200;

export const normalizeVerificationMethod = normalizeText;

export const isVerificationMethodValid = (value) =>
  hasMeaningfulContent(value, VERIFICATION_METHOD_MIN_LENGTH) &&
  value.length <= VERIFICATION_METHOD_MAX_LENGTH;
