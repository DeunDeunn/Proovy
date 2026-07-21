// 카테고리 id(운동=1, 루틴=2, 식습관=3, 취미=4, 기타=5) 기준 — 이름 문자열이 바뀌어도 색상이 깨지지 않도록 id로 매칭
export const categoryGradientMap = {
  1: "from-blue-100 via-white to-sky-100",
  2: "from-amber-100 via-orange-50 to-stone-100",
  3: "from-emerald-100 via-white to-teal-100",
  4: "from-violet-100 via-white to-purple-100",
  5: "from-slate-100 via-white to-gray-100",
};

export const defaultGradient = categoryGradientMap[5];

export const statusBadgeMap = {
  RECRUITING: { label: "모집중", className: "bg-primary" },
  IN_PROGRESS: { label: "진행중", className: "bg-orange-500" },
  COMPLETED: { label: "종료", className: "bg-gray-400" },
  CANCELLED: { label: "취소됨", className: "bg-gray-400" },
};
