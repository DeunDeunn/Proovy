import api from "@/lib/api";

export const getMe = () => api.get("/auth/me");

export const checkNicknameDuplicate = (nickname) =>
  api.get("/auth/nickname/duplicate", { params: { nickname } });

export const updateNickname = (nickname) => api.patch("/auth/profile/nickname", { nickname });

export const logout = () => api.post("/auth/logout");
