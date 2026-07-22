import api from "@/lib/api";

export const getMe = ({ signal } = {}) => api.get("/auth/me", { signal });

export const checkNicknameDuplicate = (nickname) =>
  api.get("/auth/nickname/duplicate", { params: { nickname } });

export const updateNickname = (nickname) => api.patch("/auth/profile/nickname", { nickname });

export const updateProfileImage = (file) => {
  const formData = new FormData();
  formData.append("image", file);
  return api.patch("/auth/profile/image", formData);
};

export const logout = () => api.post("/auth/logout");

export const withdraw = () => api.delete("/auth/account");
