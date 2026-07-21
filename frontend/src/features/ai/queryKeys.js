export const aiTicketKeys = {
  all: ["ai-tickets"],
  plans: () => [...aiTicketKeys.all, "plans"],
  active: () => [...aiTicketKeys.all, "active"],
  history: (params) => [...aiTicketKeys.all, "history", params],
};
