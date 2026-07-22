import api from "@/lib/api";

export const getPublicFeed = ({
  cursor,
  cursorLike,
  filter = "all",
  sort = "latest",
  size = 20,
} = {}) =>
  api.get("/v1/feed", {
    params: {
      cursor,
      cursorLike,
      filter,
      size,
      sort,
    },
  });
