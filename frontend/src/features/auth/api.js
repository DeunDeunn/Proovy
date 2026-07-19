import api from "@/lib/api";

export const getMe = () => api.get("/auth/me");
