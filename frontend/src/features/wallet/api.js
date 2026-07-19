import api from "@/lib/api";

export const getMyWallet = () => api.get("/wallets/me");

export const getWithdrawableAmount = () => api.get("/wallets/withdrawable-amount");

export const getMyTransactions = ({ type, page = 0 } = {}) =>
  api.get("/wallets/transactions", { params: { type, page } });

export const requestCharge = (amount) => api.post("/wallets/charge", { amount });

export const applyWithdrawal = (payload) => api.post("/withdrawals", payload);

export const getMyWithdrawals = ({ status, page = 0 } = {}) =>
  api.get("/withdrawals/me", { params: { status, page } });

export const getSettlementResult = (challengeId) =>
  api.get(`/challenge-rooms/${challengeId}/settlement`);

export const getSettlementHistory = ({ page = 0 } = {}) =>
  api.get("/settlements/me", { params: { page } });
