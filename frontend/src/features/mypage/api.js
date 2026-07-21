import api from "@/lib/api";

export const getMyPage = () => api.get("/mypage");

export const getVerificationStatus = () => api.get("/user-verifications/status");

export const applyVerification = () => api.post("/user-verifications");

export const getMyFeed = ({ cursor, size = 20 } = {}) =>
  api.get("/v1/me/certification-posts", { params: { cursor, size } });
