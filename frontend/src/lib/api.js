import axios from "axios";

const api = axios.create({
  baseURL: "/api",
  withCredentials: true, // 요청에 쿠키 포함
  timeout: 10000,
});

// 응답 인터셉터
api.interceptors.response.use(
  (response) => response.data.data,
  (error) => {
    const body = error.response?.data;
    // 백엔드가 내려준 에러가 아니면 네트워크 오류
    return Promise.reject(body ?? { code: "NETWORK", message: "서버에 연결할 수 없습니다." });
  }
);

export default api;
