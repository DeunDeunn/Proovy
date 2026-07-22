import api from "@/lib/api";

export const getAiTicketPlans = () => api.get("/ai-tickets/plans");

export const getActiveAiTicket = () => api.get("/ai-tickets/active");

export const getAiTicketHistory = ({ type, page = 0, size = 10 } = {}) =>
  api.get("/ai-tickets/history", { params: { type, page, size } });

export const purchaseAiTicket = (planId) => api.post("/ai-tickets/purchases", { planId });

export const getAiReviewRule = (challengeId) =>
  api.get(`/challenges/${challengeId}/ai-review-rule`);

export const upsertAiReviewRule = (challengeId, payload) =>
  api.put(`/challenges/${challengeId}/ai-review-rule`, payload);

export const updateAiReviewMode = (challengeId, reviewMode) =>
  api.patch(`/challenges/${challengeId}/ai-review-rule/review-mode`, { reviewMode });

export const deactivateAiReview = (challengeId) =>
  api.delete(`/challenges/${challengeId}/ai-review-rule`);

export const requestAiReview = (postId) =>
  api.post(`/certification-posts/${postId}/ai-review`);

export const getAiReviewResults = (challengeId, { page = 0, size = 10 } = {}) =>
  api.get(`/challenges/${challengeId}/ai-review-results`, { params: { page, size } });
