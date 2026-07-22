import api from "@/lib/api";

const UNAUTHENTICATED_CODE = "C004";
const EMPTY_TODAY_CERTIFICATION_PROGRESS = {
  certifiedChallengeCount: 0,
  inProgressChallengeCount: 0,
};

const isUnauthenticated = (error) => error?.code === UNAUTHENTICATED_CODE;

export const getPopularFeed = () => api.get("/v1/feed", { params: { sort: "POPULAR", size: 3 } });

// 비로그인 사용자는 홈의 오늘 인증을 0/0으로 표시한다.
export const getTodayCertificationProgress = async () => {
  try {
    return await api.get("/v1/certification-posts/today-progress");
  } catch (error) {
    if (isUnauthenticated(error)) return EMPTY_TODAY_CERTIFICATION_PROGRESS;
    throw error;
  }
};

// 진행 중인 내 챌린지들의 연속 성공일을 조회해 가장 큰 값만 홈에 보여준다.
export const getMyMaxCertificationStreak = async () => {
  try {
    const challenges = await api.get("/challenges/me");
    const inProgressChallenges = challenges.filter((challenge) => challenge.status === "IN_PROGRESS");

    if (inProgressChallenges.length === 0) return 0;

    const streaks = await Promise.all(
      inProgressChallenges.map((challenge) =>
        api.get(`/v1/challenge/${challenge.id}/my-certification-streak`)
      )
    );

    return Math.max(0, ...streaks.map((streak) => streak.currentStreakDays ?? 0));
  } catch (error) {
    if (isUnauthenticated(error)) return 0;
    throw error;
  }
};
