import api from "@/lib/api";

export const getPublicFeed = ({ cursor, filter = "all", size = 20 } = {}) =>
  api.get("/v1/feed", {
    params: {
      cursor,
      filter,
      size,
      sort: "latest",
    },
  });
