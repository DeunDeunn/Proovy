export const walletKeys = {
  all: ["wallet"],
  me: () => [...walletKeys.all, "me"],
  withdrawableAmount: () => [...walletKeys.all, "withdrawable-amount"],
  transactions: (params) => [...walletKeys.all, "transactions", params],
};

export const withdrawalKeys = {
  all: ["withdrawals"],
  me: (params) => [...withdrawalKeys.all, "me", params],
};

export const settlementKeys = {
  all: ["settlement"],
  detail: (challengeId) => [...settlementKeys.all, challengeId],
};
