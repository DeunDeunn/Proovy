import api from "@/lib/api";

export const getPopularFeed = () => api.get("/v1/feed", { params: { sort: "POPULAR", size: 3 } });
