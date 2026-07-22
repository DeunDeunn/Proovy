import api from "@/lib/api";

export const getCategories = () => api.get("/challenges/categories");

export const getChallenges = (params) => api.get("/challenges", { params });

export const getChallenge = (challengeId) => api.get(`/challenges/${challengeId}`);

export const getChallengeParticipants = (challengeId) =>
  api.get(`/challenges/${challengeId}/participants`);

export const getChallengeParticipantsManage = (challengeId) =>
  api.get(`/challenges/${challengeId}/participants/manage`);

export const kickParticipant = (challengeId, userId) =>
  api.delete(`/challenges/${challengeId}/participants/${userId}`);

export const joinChallenge = (challengeId) => api.post(`/challenges/${challengeId}/participants`);

export const leaveChallenge = (challengeId) =>
  api.delete(`/challenges/${challengeId}/participants/me`);

export const createChallenge = (payload) => api.post("/challenges", payload);

export const updateChallenge = (challengeId, payload) =>
  api.patch(`/challenges/${challengeId}`, payload);

export const cancelChallenge = (challengeId) => api.delete(`/challenges/${challengeId}`);

export const updateChallengeThumbnail = (challengeId, file) => {
  const formData = new FormData();
  formData.append("image", file);
  return api.patch(`/challenges/${challengeId}/thumbnail`, formData);
};
