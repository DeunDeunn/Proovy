// 카테고리 ID는 시드/마이그레이션에 따라 달라질 수 있으므로 API 응답의 이름으로 시각 요소를 정한다.
const categoryGradientMap = {
  운동: "from-blue-100 via-white to-sky-100",
  루틴: "from-amber-100 via-orange-50 to-stone-100",
  식습관: "from-emerald-100 via-white to-teal-100",
  취미: "from-violet-100 via-white to-purple-100",
  기타: "from-slate-100 via-white to-gray-100",
};

const defaultGradient = "from-slate-100 via-white to-gray-100";

export const getCategoryGradient = (categoryName) =>
  categoryGradientMap[categoryName] ?? defaultGradient;

export const statusBadgeMap = {
  RECRUITING: { label: "모집중", className: "border border-primary bg-primary-light text-primary" },
  IN_PROGRESS: {
    label: "진행중",
    className: "border border-orange-500 bg-orange-50 text-orange-600",
  },
  COMPLETED: { label: "종료", className: "border border-gray-300 bg-gray-50 text-gray-500" },
  CANCELLED: { label: "취소됨", className: "border border-gray-300 bg-gray-50 text-gray-500" },
};
