import api from "@/lib/api";

export const getAiTicketPlans = () => api.get("/ai-tickets/plans");

export const getActiveAiTicket = () => api.get("/ai-tickets/active");

export const getAiTicketHistory = ({ type, page = 0, size = 10 } = {}) =>
  api.get("/ai-tickets/history", { params: { type, page, size } });

export const purchaseAiTicket = (planId) => api.post("/ai-tickets/purchases", { planId });
