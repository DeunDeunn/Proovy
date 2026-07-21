import axios from "axios";

const api = axios.create({
  baseURL: "/api",
  withCredentials: true, // 요청에 쿠키 포함
  timeout: 10000,
});

// accessToken 만료 시 재발급 요청을 한 번만 보내고 나머지 401은 그 결과를 공유해서 기다림
let refreshPromise = null;

const refreshAccessToken = () => {
  if (!refreshPromise) {
    refreshPromise = axios
      .post("/api/auth/refresh", null, { withCredentials: true })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
};

// 응답 인터셉터
api.interceptors.response.use(
  (response) => response.data.data,
  async (error) => {
    const { config, response } = error;
    const body = response?.data;
    const toRejection = () => body ?? { code: "NETWORK", message: "서버에 연결할 수 없습니다." };

    // accessToken 만료(401)면 refreshToken으로 재발급받고 원래 요청을 한 번만 재시도
    if (response?.status === 401 && config && !config._retry) {
      config._retry = true;
      try {
        await refreshAccessToken();
        return api(config);
      } catch {
        // refreshToken도 만료/무효면 원래의 401 에러를 그대로 전달 (RequireAuth가 로그인 필요 처리)
        return Promise.reject(toRejection());
      }
    }

    return Promise.reject(toRejection());
  }
);

export default api;
