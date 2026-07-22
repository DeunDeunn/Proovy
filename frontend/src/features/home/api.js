import api from "@/lib/api";

export const getPopularFeed = () => api.get("/v1/feed", { params: { sort: "POPULAR", size: 3 } });

// 진행 중인 내 챌린지들의 연속 성공일을 조회해 가장 큰 값만 홈에 보여준다.
export const getMyMaxCertificationStreak = async () => {
  const challenges = await api.get("/challenges/me");
  const inProgressChallenges = challenges.filter((challenge) => challenge.status === "IN_PROGRESS");

  if (inProgressChallenges.length === 0) return 0;

  const streaks = await Promise.all(
    inProgressChallenges.map((challenge) =>
      api.get(`/v1/challenge/${challenge.id}/my-certification-streak`)
    )
  );

  return Math.max(0, ...streaks.map((streak) => streak.currentStreakDays ?? 0));
};
