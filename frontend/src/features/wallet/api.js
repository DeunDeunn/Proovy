import api from "@/lib/api";

export const getMyWallet = () => api.get("/wallets/me");

export const getWithdrawableAmount = () => api.get("/wallets/withdrawable-amount");

export const getMyTransactions = ({ type, page = 0 } = {}) =>
  api.get("/wallets/transactions", { params: { type, page } });

export const requestCharge = (amount) => api.post("/wallets/charge", { amount });

export const confirmNaverPayCharge = ({ merchantPayKey, paymentId }) => {
  if (!merchantPayKey || !paymentId) {
    return Promise.reject({ message: "결제 정보를 찾을 수 없어요. 캐시 충전 화면에서 다시 시도해주세요." });
  }
  return api.post("/payments/naverpay/callback", { merchantPayKey, paymentId });
};

export const applyWithdrawal = (payload) => api.post("/withdrawals", payload);

export const getMyWithdrawals = ({ status, page = 0 } = {}) =>
  api.get("/withdrawals/me", { params: { status, page } });

export const getSettlementResult = (challengeId) =>
  api.get(`/challenge-rooms/${challengeId}/settlement`);

export const getSettlementHistory = ({ page = 0 } = {}) =>
  api.get("/settlements/me", { params: { page } });
