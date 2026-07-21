import api from "@/lib/api";

export const getVerificationList = ({ status, page = 0, size = 20 } = {}) =>
  api.get("/admin/user-verifications", { params: { status, page, size } });

export const reviewVerification = (id, { status, rejectionReason }) =>
  api.patch(`/admin/user-verifications/${id}`, { status, rejectionReason });

export const getReportList = ({ targetType, page = 0, size = 20 } = {}) =>
  api.get("/v1/admin/reports", { params: { targetType, page, size } });

export const processReport = (reportId) => api.patch(`/v1/admin/reports/${reportId}/process`);

export const rejectReport = (reportId) => api.patch(`/v1/admin/reports/${reportId}/reject`);
