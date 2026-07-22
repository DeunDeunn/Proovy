export const aiTicketKeys = {
  all: ["ai-tickets"],
  plans: () => [...aiTicketKeys.all, "plans"],
  active: () => [...aiTicketKeys.all, "active"],
  history: (params) => [...aiTicketKeys.all, "history", params],
};

export const aiReviewKeys = {
  all: ["ai-reviews"],
  rules: () => [...aiReviewKeys.all, "rules"],
  rule: (challengeId) => [...aiReviewKeys.rules(), challengeId],
  results: () => [...aiReviewKeys.all, "results"],
  resultList: (challengeId, params) => [...aiReviewKeys.results(), challengeId, params],
};
