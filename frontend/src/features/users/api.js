import api from "@/lib/api";

export const getUserProfile = (userId) => api.get(`/users/${userId}`);

export const followUser = (userId) => api.post(`/users/${userId}/follow`);

export const unfollowUser = (userId) => api.delete(`/users/${userId}/follow`);

export const getFollowers = (userId, { page = 0, size = 20 } = {}) =>
  api.get(`/users/${userId}/followers`, { params: { page, size } });

export const getFollowing = (userId, { page = 0, size = 20 } = {}) =>
  api.get(`/users/${userId}/following`, { params: { page, size } });

export const getUserFeed = (userId, { cursor, size = 20 } = {}) =>
  api.get(`/v1/users/${userId}/certification-posts`, { params: { cursor, size } });
