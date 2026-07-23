import { AlertTriangle, ArrowRight, BookOpen, Crown, Ticket } from "lucide-react";

import Card from "@/components/ui/Card";

const CARD_TONE = "rounded-2xl border-gray-100 p-6 shadow-xl shadow-black/5";

const RuleNumber = ({ number }) => (
  <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-primary-light text-sm font-bold text-primary">
    {number}
  </span>
);

const LockedPill = () => (
  <span className="shrink-0 rounded-full bg-gray-100 px-2.5 py-1 text-xs font-medium text-gray-500">
    변경불가
  </span>
);

const Rule = ({ number, children, locked }) => (
  <div className="flex items-center gap-3">
    <RuleNumber number={number} />
    <div className="flex flex-1 flex-wrap items-center gap-x-2 gap-y-1">
      <p className="text-sm text-gray-600">{children}</p>
      {locked && <LockedPill />}
    </div>
  </div>
);

const STEP_NAV = [
  { href: "#step-1", number: "1", label: "참가하기" },
  { href: "#step-2", number: "2", label: "인증하기" },
  { href: "#step-3", number: "3", label: "AI로 검수받기" },
];

// 실제 챌린지 상태 뱃지 색상은 연한 톤(challenge/categoryVisuals.js의 statusBadgeMap)이지만,
// 가이드 다이어그램에서는 흐름을 더 잘 읽히게 하기 위해 진한 색으로 강조한다.
const CHALLENGE_STATUS_FLOW = [
  { label: "모집중", caption: "참가 가능", className: "bg-primary text-white" },
  { label: "진행중", caption: "참가 불가", className: "bg-orange-500 text-white" },
  { label: "종료", caption: "성공/실패 확정", className: "bg-gray-200 text-gray-600" },
];

const StatusFlow = ({ steps, footnote }) => (
  <div className="rounded-2xl bg-gray-50 p-5">
    <div className="flex flex-col items-center gap-3 sm:flex-row sm:justify-center sm:gap-4">
      {steps.map((step, index) => (
        <div key={step.label} className="flex items-center gap-4">
          {index > 0 && <ArrowRight size={16} className="hidden shrink-0 text-gray-300 sm:block" />}
          <div className="flex flex-col items-center gap-1.5">
            <span className={`rounded-full px-4 py-1.5 text-sm font-semibold ${step.className}`}>
              {step.label}
            </span>
            <span className="text-xs text-gray-500">{step.caption}</span>
          </div>
        </div>
      ))}
    </div>
    {footnote && <p className="mt-4 text-center text-sm text-gray-500">{footnote}</p>}
  </div>
);

const SectionHeader = ({ number, title, intro }) => (
  <div>
    <div className="flex items-center gap-3">
      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary text-sm font-bold text-white">
        {number}
      </span>
      <h2 className="text-lg font-bold text-gray-900">{title}</h2>
    </div>
    <p className="mt-3 text-sm text-gray-500">{intro}</p>
  </div>
);

const GuidePage = () => {
  return (
    <div className="mx-auto max-w-[1440px]">
      <h1 className="flex items-center gap-2 text-2xl font-bold text-gray-900">
        <BookOpen size={24} />
        가이드
      </h1>
      <p className="mt-2 text-sm text-gray-500">
        Proovy를 처음 사용한다면, 이 3단계만 알면 충분해요.
      </p>

      <div className="mt-4 flex flex-wrap gap-2">
        {STEP_NAV.map((step, index) => (
          <a
            key={step.href}
            href={step.href}
            className={`flex items-center gap-2 rounded-full px-4 py-2 text-sm font-semibold ${
              index === 0 ? "bg-surface text-gray-900 shadow-sm" : "text-gray-600 hover:bg-gray-100"
            }`}
          >
            <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-primary text-xs text-white">
              {step.number}
            </span>
            {step.label}
          </a>
        ))}
      </div>

      <div className="mt-6 flex flex-col gap-4">
        {/* 1. 챌린지 참가하기 */}
        <Card id="step-1" className={CARD_TONE}>
          <SectionHeader
            number="1"
            title="챌린지 참가하기"
            intro="챌린지 방에 참가해서 함께 목표를 인증하는 첫 단계예요."
          />

          <div className="mt-5 flex flex-col gap-4">
            <StatusFlow
              steps={CHALLENGE_STATUS_FLOW}
              footnote="상태는 자동으로 전환되고, 방장도 이 흐름을 바꿀 수 없어요."
            />
            <Rule number="1">
              &apos;모집중&apos; 상태일 때만 참가할 수 있어요. 정원이 다 찼거나
              &apos;진행중&apos;으로 넘어간 챌린지는 참가할 수 없어요.
            </Rule>
            <Rule number="2">
              참가비는 보유 캐시에서 바로 결제돼요. 캐시가 부족하면 참가할 수 없으니 미리
              충전해두세요.
            </Rule>
            <Rule number="3">같은 챌린지에는 한 번만 참가할 수 있어요.</Rule>
            <Rule number="4" locked>
              챌린지 성공 기준은 인증 성공률 80%로 고정돼 있어요. 방장도 이 기준은 바꿀 수 없어요.
            </Rule>
            <Rule number="5" locked>
              참가자가 있는 챌린지는 참가비·기간·정원·인증 방법 같은 핵심 조건이 바뀌지 않아요.
              제목·설명 정도만 방장이 수정할 수 있어요.
            </Rule>
          </div>

          <div className="mt-5 rounded-2xl bg-primary-light p-5">
            <div className="flex items-center gap-3">
              <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-primary text-white">
                <Crown size={18} />
              </span>
              <p className="font-bold text-gray-900">챌린지를 개설하면 방장이 돼요</p>
            </div>
            <ul className="mt-3 space-y-2">
              {[
                "챌린지를 만든 사람은 자동으로 그 챌린지의 방장이 돼요.",
                "참가자가 올린 인증 게시글을 검수(승인/반려)하고, 필요하면 참가자를 강제 퇴장시킬 권한을 가져요.",
                "다만 본인이 개설한 챌린지에서는 스스로 탈퇴할 수 없어요.",
              ].map((text) => (
                <li key={text} className="flex gap-2.5">
                  <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-primary" />
                  <span className="text-sm text-gray-700">{text}</span>
                </li>
              ))}
            </ul>
          </div>
        </Card>

        {/* 2. 인증 게시글 올리고 검수받기 */}
        <Card id="step-2" className={CARD_TONE}>
          <SectionHeader
            number="2"
            title="인증 게시글 올리고 검수받기"
            intro="매일 정해진 시간에 인증 게시글을 올리고, 방장의 검수를 받아요."
          />

          <div className="mt-5 flex flex-col gap-4">
            <Rule number="1">
              한 챌린지에서 인증 게시글은 하루에 한 번만, 정해진 인증 가능 시간에만 올릴 수 있어요.
            </Rule>
            <Rule number="2">
              반려되면 인증 가능 시간 안에 수정해서 다시 제출할 수 있어요. 이미 승인된 게시글도
              수정하면 다시 &apos;검수 대기&apos; 상태로 돌아가요.
            </Rule>
            <Rule number="3">
              검수(승인/반려)는 해당 챌린지 방장 또는 사이트 관리자만 할 수 있어요.
            </Rule>
            <Rule number="4">
              방장도 참여자예요. 방장 본인의 인증 게시글은 스스로 검수할 수 없어서 자동으로 AI
              검수를 받아요.
            </Rule>
            <Rule number="5">게시글을 삭제하면 그날은 다시 올릴 수 없어요.</Rule>
          </div>

          <div className="mt-5 rounded-2xl bg-amber-50 p-5">
            <div className="flex items-center gap-3">
              <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-amber-500 text-white">
                <AlertTriangle size={18} />
              </span>
              <p className="font-bold text-gray-900">방장이 검수를 미루면 이렇게 돼요</p>
            </div>
            <ul className="mt-3 space-y-2">
              <li className="flex gap-2.5">
                <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-amber-500" />
                <span className="text-sm text-amber-900">
                  방장이 그날 자정(한국 시간)까지 검수하지 않으면 게시글은{" "}
                  <strong className="font-semibold">자동으로 승인</strong>돼요. 대신 방장에게 경고가
                  하나 쌓여요.
                </span>
              </li>
              <li className="flex gap-2.5">
                <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-amber-500" />
                <span className="text-sm text-amber-900">
                  경고가 3번 쌓이면 — 우수 사용자는 일반 사용자로 강등되고, 일반 사용자는 14일간
                  챌린지 개설이 제한돼요.
                </span>
              </li>
            </ul>
          </div>
        </Card>

        {/* 3. AI로 빠르게 검수받기 */}
        <Card id="step-3" className={CARD_TONE}>
          <SectionHeader
            number="3"
            title="AI로 빠르게 검수받기"
            intro="방장도 사람인지라 매번 직접 검수하기 바쁠 때가 있어요. 방장이 AI 티켓을 사용하면 참여자의 인증 게시글을 AI가 대신 검수해줘요."
          />

          <div className="mt-5 flex flex-col gap-4">
            <div className="rounded-2xl bg-gray-100 p-5">
              <div className="flex items-center gap-3">
                <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-gray-700 text-white">
                  <Ticket size={18} />
                </span>
                <p className="font-bold text-gray-900">AI 검수를 쓰려면 티켓이 필요해요</p>
              </div>
              <ul className="mt-3 space-y-2">
                <li className="flex gap-2.5">
                  <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-gray-400" />
                  <span className="text-sm text-gray-600">
                    방장이 <strong className="font-semibold text-gray-900">AI 티켓(구독권)</strong>
                    을 구매해야 AI 검수를 쓸 수 있어요.
                  </span>
                </li>
                <li className="flex gap-2.5">
                  <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-gray-400" />
                  <span className="text-sm text-gray-600">
                    방장은 한 번에 하나의 활성 구독만 가질 수 있고, 만료·취소·환불된 뒤에만 새
                    상품을 구매할 수 있어요.
                  </span>
                </li>
              </ul>
            </div>

            <Rule number="1">하나의 챌린지방에는 하나의 티켓만 활성화할 수 있어요.</Rule>
            <Rule number="2">인증 게시글 하나당 AI 검수는 최대 1번만 받을 수 있어요.</Rule>
            <Rule number="3">이미 검수가 끝난 게시글은 AI 검수를 다시 받을 수 없어요.</Rule>
          </div>
        </Card>
      </div>
    </div>
  );
};

export default GuidePage;
