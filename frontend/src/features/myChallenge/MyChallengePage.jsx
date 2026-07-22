"use client";

import { useState } from "react";

import Loading from "@/components/ui/Loading";
import ErrorMessage from "@/components/ui/ErrorMessage";
import ChallengeCard from "@/features/challenge/ChallengeCard";
import { useMyPage } from "@/features/mypage/hooks";

const TABS = [
  { key: "participating", label: "참여 중인 챌린지" },
  { key: "hosting", label: "운영 중인 챌린지" },
  { key: "completed", label: "종료된 챌린지" },
];

const EMPTY_MESSAGE = {
  participating: "참여 중인 챌린지가 없어요.",
  hosting: "운영 중인 챌린지가 없어요.",
  completed: "종료된 챌린지가 없어요.",
};

const TabButton = ({ label, count, selected, onClick }) => (
  <button
    type="button"
    onClick={onClick}
    className={`rounded-full px-4 py-2 text-sm font-medium transition-colors ${
      selected ? "bg-primary text-white" : "border border-gray-300 text-gray-600 hover:bg-gray-50"
    }`}
  >
    {label} {count}
  </button>
);

const MyChallengePage = ({ initialTab }) => {
  const [tab, setTab] = useState(initialTab === "hosting" ? "hosting" : "participating");
  const { data, isLoading, isError, error } = useMyPage();

  const participatingChallenges = data?.participatingChallenges ?? [];
  const hostingChallenges = data?.hostingChallenges ?? [];
  const completedChallenges = data?.completedChallenges ?? [];
  const challengesByTab = {
    participating: participatingChallenges,
    hosting: hostingChallenges,
    completed: completedChallenges,
  };
  const challenges = challengesByTab[tab];

  return (
    <div className="mx-auto max-w-[1440px] space-y-6 pb-2">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">내 챌린지</h1>
        <p className="mt-2 text-sm text-gray-500">
          참여 중이거나 운영 중이거나 종료된 챌린지를 한눈에 확인해보세요!
        </p>
      </div>

      <div className="flex gap-2">
        {TABS.map(({ key, label }) => (
          <TabButton
            key={key}
            label={label}
            count={challengesByTab[key].length}
            selected={tab === key}
            onClick={() => setTab(key)}
          />
        ))}
      </div>

      {isLoading ? (
        <Loading label="챌린지 불러오는 중..." />
      ) : isError ? (
        <ErrorMessage error={error} />
      ) : challenges.length === 0 ? (
        <p className="py-12 text-center text-sm text-gray-400">{EMPTY_MESSAGE[tab]}</p>
      ) : (
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {challenges.map((challenge) => (
            <ChallengeCard
              key={challenge.id}
              challenge={challenge}
              showPendingCertificationBadge={tab === "hosting"}
            />
          ))}
        </div>
      )}
    </div>
  );
};

export default MyChallengePage;
