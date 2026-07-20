import api from "@/lib/api";

export const getMyPage = () => api.get("/mypage");

export const getVerificationStatus = () => api.get("/user-verifications/status");

export const applyVerification = () => api.post("/user-verifications");
