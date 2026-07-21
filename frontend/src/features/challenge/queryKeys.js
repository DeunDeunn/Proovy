// React Query 캐시 키를 한 곳에서 관리 (오타 방지, invalidate 시 어디까지 무효화할지 명확하게)
export const challengeKeys = {
  all: ["challenges"],
  categories: () => [...challengeKeys.all, "categories"],
  lists: () => [...challengeKeys.all, "list"],
  list: (params) => [...challengeKeys.lists(), params],
  details: () => [...challengeKeys.all, "detail"],
  detail: (challengeId) => [...challengeKeys.details(), challengeId],
  participants: (challengeId) => [...challengeKeys.detail(challengeId), "participants"],
};
