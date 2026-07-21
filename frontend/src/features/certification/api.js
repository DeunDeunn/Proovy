import api from "@/lib/api";

export const getCertificationPost = (postId) => api.get(`/v1/certification-post/${postId}`);

export const getComments = (postId, { cursor, size = 20 } = {}) =>
  api.get(`/v1/certification-post/${postId}/comments`, {
    params: { cursor, size },
  });

export const createComment = (postId, payload) =>
  api.post(`/v1/certification-post/${postId}/comments`, payload);

export const createReport = (payload) => api.post("/v1/reports", payload);

export const updateComment = (commentId, payload) => api.put(`/v1/comments/${commentId}`, payload);

export const deleteComment = (commentId) => api.delete(`/v1/comments/${commentId}`);

export const toggleCommentLike = (commentId) => api.post(`/v1/comments/${commentId}/like`);

export const createCertificationPost = (challengeId, { contents, thumbnail, images }) => {
  const formData = new FormData();

  // @RequestPart("request")가 JSON으로 역직렬화될 수 있도록 Blob으로 첨부한다.
  formData.append(
    "request",
    new Blob([JSON.stringify({ contents })], { type: "application/json" })
  );
  formData.append("thumbnail", thumbnail);
  images.forEach((image) => formData.append("images", image));

  // Content-Type을 직접 지정하지 않아야 브라우저가 multipart boundary를 함께 설정한다.
  return api.post(`/v1/challenge/${challengeId}/certification-post`, formData);
};

export const updateCertificationPost = (
  postId,
  { contents, thumbnail, keptImageUrls, newImages }
) => {
  const formData = new FormData();

  // 대표이미지를 유지하면 thumbnail 파일 없이 keepThumbnail=true만 보낸다.
  const keepThumbnail = !thumbnail;
  formData.append(
    "request",
    new Blob([JSON.stringify({ contents, keepThumbnail, keptImageUrls })], {
      type: "application/json",
    })
  );
  if (thumbnail) {
    formData.append("thumbnail", thumbnail);
  }
  // 새로 추가한 파일만 업로드한다(유지 이미지는 URL로만 전달).
  newImages.forEach((image) => formData.append("images", image));

  return api.put(`/v1/certification-post/${postId}`, formData);
};

export const deleteCertificationPost = (postId) => api.delete(`/v1/certification-post/${postId}`);

export const toggleCertificationPostLike = (postId) =>
  api.post(`/v1/certification-post/${postId}/like`);
