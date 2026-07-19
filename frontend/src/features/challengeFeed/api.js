import api from "@/lib/api";

export const getChallengeFeed = (challengeId, { cursor, filter = "all", size = 20 } = {}) =>
  api.get(`/v1/challenge/${challengeId}/feed`, {
    params: {
      cursor,
      filter,
      size,
      sort: "latest",
    },
  });
