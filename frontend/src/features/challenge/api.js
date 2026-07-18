import api from "@/lib/api";

export const getCategories = () => api.get("/challenges/categories");

export const getChallenges = (params) => api.get("/challenges", { params });

export const getChallenge = (challengeId) => api.get(`/challenges/${challengeId}`);

export const createChallenge = (payload) => api.post("/challenges", payload);

export const updateChallenge = (challengeId, payload) =>
  api.patch(`/challenges/${challengeId}`, payload);

export const cancelChallenge = (challengeId) => api.delete(`/challenges/${challengeId}`);
