// 백엔드 TargetType(com.deundeun.notification.domain.TargetType)별 이동 경로.
//
// SETTLEMENT/HOST_REVENUE는 알림의 targetId가 challengeId가 아니라 정산/방장수수료
// 레코드 자체의 id라서(Settlement.id, HostRevenue.id), 이 id만으로는 어떤 챌린지의
// 상세인지 알 수 없다. 조회 API도 challengeId 기준이라 역으로 찾을 방법이 없어,
// 개별 상세 대신 목록 페이지로 보낸다.
// CHALLENGE는 아직 백엔드가 이 타입으로 알림을 만들지 않고 상세 페이지도 없어 제외한다.
const TARGET_ROUTE_BUILDERS = {
  VERIFICATION_POST: (targetId) => `/certification-posts/${targetId}`,
  BADGE_APPLICATION: () => "/mypage/verification",
  SETTLEMENT: () => "/wallet/settlements",
  HOST_REVENUE: () => "/wallet/settlements",
};

export const getNotificationTargetHref = (targetType, targetId) => {
  const buildHref = TARGET_ROUTE_BUILDERS[targetType];
  if (!buildHref || targetId == null) return null;

  return buildHref(targetId);
};
