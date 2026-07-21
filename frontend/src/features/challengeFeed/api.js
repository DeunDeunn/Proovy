import api from "@/lib/api";

export const getChallengeFeed = (
  challengeId,
  { cursor, cursorLike, filter = "all", sort = "latest", size = 20 } = {}
) =>
  api.get(`/v1/challenge/${challengeId}/feed`, {
    params: {
      cursor,
      cursorLike,
      filter,
      size,
      sort,
    },
  });

export const getPendingCertifications = (challengeId, { cursor, size = 20 } = {}) =>
  api.get(`/v1/challenge/${challengeId}/pending-certifications`, {
    params: { cursor, size },
  });

export const approveCertificationPost = (postId) =>
  api.patch(`/v1/certification-post/${postId}/approve`);

export const rejectCertificationPost = (postId, reason) =>
  api.patch(`/v1/certification-post/${postId}/reject`, { reason });
