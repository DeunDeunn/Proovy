"use client";

import { Suspense, useCallback, useEffect, useRef, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { Search } from "lucide-react";

import Loading from "@/components/ui/Loading";
import ErrorMessage from "@/components/ui/ErrorMessage";

import ChallengeCard from "./ChallengeCard";
import { useCategories, useInfiniteChallenges } from "./hooks";

const statusOptions = [
  { label: "전체", value: undefined },
  { label: "모집중", value: "RECRUITING" },
  { label: "진행중", value: "IN_PROGRESS" },
  { label: "종료", value: "COMPLETED" },
];

const sortOptions = [
  { value: undefined, defaultLabel: "최신순", reversedLabel: "오래된순", defaultDirection: "DESC" },
  {
    value: "ENTRY_FEE",
    defaultLabel: "참가비 낮은순",
    reversedLabel: "참가비 높은순",
    defaultDirection: "ASC",
  },
  {
    value: "PARTICIPANTS",
    defaultLabel: "참가자 많은순",
    reversedLabel: "참가자 적은순",
    defaultDirection: "DESC",
  },
];

const getSortChipState = (option, sort, direction) => {
  const isSelected = sort === option.value;
  const currentDirection = isSelected
    ? (direction ?? option.defaultDirection)
    : option.defaultDirection;
  const label =
    currentDirection === option.defaultDirection ? option.defaultLabel : option.reversedLabel;
  return { isSelected, currentDirection, label };
};

const getFiltersFromSearchParams = (searchParams) => {
  const rawCategoryId = searchParams.get("category");
  const categoryId = rawCategoryId ? Number(rawCategoryId) : undefined;

  return {
    categoryId: Number.isNaN(categoryId) ? undefined : categoryId,
    status: searchParams.get("status") ?? undefined,
    sort: searchParams.get("sort") ?? undefined,
    direction: searchParams.get("direction") ?? undefined,
    keyword: searchParams.get("keyword")?.trim() || undefined,
  };
};

const FilterChip = ({ label, selected, onClick }) => (
  <button
    type="button"
    aria-pressed={selected}
    onClick={onClick}
    className={`cursor-pointer rounded-full border px-3 py-1.5 text-sm font-medium transition-colors ${
      selected
        ? "border-primary bg-primary-light font-semibold text-primary"
        : "border-gray-300 bg-white text-gray-600 hover:bg-gray-50"
    }`}
  >
    {label}
  </button>
);

const KeywordSearchInput = ({ initialKeyword, onSearch }) => {
  const [keywordInput, setKeywordInput] = useState(initialKeyword);

  useEffect(() => {
    const timer = setTimeout(() => onSearch(keywordInput.trim() || undefined), 300);
    return () => clearTimeout(timer);
  }, [keywordInput, onSearch]);

  return (
    <div className="relative min-w-[240px] flex-1">
      <Search
        size={16}
        aria-hidden="true"
        className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
      />
      <input
        type="text"
        value={keywordInput}
        onChange={(e) => setKeywordInput(e.target.value)}
        placeholder="챌린지 제목 또는 키워드 검색"
        className="w-full max-w-lg rounded-lg border border-gray-300 py-2 pl-9 pr-3 text-sm focus:border-primary focus:outline-none"
      />
    </div>
  );
};

const ChallengePageContent = () => {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const { categoryId, status, sort, direction, keyword } = getFiltersFromSearchParams(searchParams);

  // URL을 필터의 단일 기준으로 둬서 브라우저 뒤로가기/앞으로가기와 공유 링크도 같은 상태를 보여준다.
  const updateFilters = useCallback((updates) => {
    const nextFilters = { categoryId, status, sort, direction, keyword, ...updates };
    const params = new URLSearchParams();
    if (nextFilters.categoryId !== undefined) params.set("category", nextFilters.categoryId);
    if (nextFilters.status !== undefined) params.set("status", nextFilters.status);
    if (nextFilters.sort !== undefined) params.set("sort", nextFilters.sort);
    if (nextFilters.direction !== undefined) params.set("direction", nextFilters.direction);
    if (nextFilters.keyword) params.set("keyword", nextFilters.keyword);

    const query = params.toString();
    if (query !== searchParams.toString()) {
      router.replace(query ? `${pathname}?${query}` : pathname, { scroll: false });
    }
  }, [categoryId, status, sort, direction, keyword, pathname, router, searchParams]);

  const { data: categories, isError: isCategoriesError, error: categoriesError } = useCategories();
  const { data, isLoading, isError, error, fetchNextPage, hasNextPage, isFetchingNextPage } =
    useInfiniteChallenges({ categoryId, status, sort, direction, keyword, size: 12 });

  const challenges = data?.pages.flatMap((page) => page.content) ?? [];

  const observerTarget = useRef(null);

  useEffect(() => {
    const target = observerTarget.current;
    if (!target) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && hasNextPage && !isFetchingNextPage) {
          fetchNextPage();
        }
      },
      { threshold: 1.0 }
    );

    observer.observe(target);
    return () => observer.disconnect();
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  return (
    <div className="mx-auto max-w-[1440px] space-y-6 pb-2">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">챌린지 찾기</h1>
        <p className="mt-2 text-sm text-gray-500">나에게 딱 맞는 챌린지를 찾아보세요!</p>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <KeywordSearchInput
          key={keyword ?? ""}
          initialKeyword={keyword ?? ""}
          onSearch={(nextKeyword) => updateFilters({ keyword: nextKeyword })}
        />

        <div className="flex flex-wrap gap-2" role="group" aria-label="정렬 기준">
          {sortOptions.map((option) => {
            const { isSelected, currentDirection, label } = getSortChipState(
              option,
              sort,
              direction
            );

            return (
              <FilterChip
                key={option.defaultLabel}
                label={label}
                selected={isSelected}
                onClick={() => {
                  if (isSelected) {
                    updateFilters({ direction: currentDirection === "ASC" ? "DESC" : "ASC" });
                  } else {
                    updateFilters({ sort: option.value, direction: undefined });
                  }
                }}
              />
            );
          })}
        </div>
      </div>

      <div className="space-y-3 rounded-xl bg-gray-50 p-4">
        <div>
          <p className="mb-2 text-sm font-bold text-gray-700">카테고리</p>
          {isCategoriesError ? (
            <ErrorMessage error={categoriesError} />
          ) : (
            <div className="flex flex-wrap gap-2">
              <FilterChip
                label="전체"
                selected={categoryId === undefined}
                onClick={() => updateFilters({ categoryId: undefined })}
              />
              {categories?.map((category) => (
                <FilterChip
                  key={category.id}
                  label={category.name}
                  selected={categoryId === category.id}
                  onClick={() => updateFilters({ categoryId: category.id })}
                />
              ))}
            </div>
          )}
        </div>

        <div className="border-t border-gray-200 pt-3">
          <p className="mb-2 text-sm font-bold text-gray-700">상태</p>
          <div className="flex flex-wrap gap-2">
            {statusOptions.map((option) => (
              <FilterChip
                key={option.label}
                label={option.label}
                selected={status === option.value}
                onClick={() => updateFilters({ status: option.value })}
              />
            ))}
          </div>
        </div>
      </div>

      {isLoading ? (
        <Loading label="챌린지 불러오는 중..." />
      ) : isError ? (
        <ErrorMessage error={error} />
      ) : challenges.length === 0 ? (
        <p className="py-12 text-center text-sm text-gray-400">조건에 맞는 챌린지가 없습니다.</p>
      ) : (
        <>
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {challenges.map((challenge) => (
              <ChallengeCard key={challenge.id} challenge={challenge} />
            ))}
          </div>

          {hasNextPage && (
            <div ref={observerTarget} className="mt-6 flex h-10 justify-center">
              {isFetchingNextPage && <Loading label="불러오는 중..." />}
            </div>
          )}
        </>
      )}
    </div>
  );
};

const ChallengePage = () => (
  <Suspense fallback={<Loading label="불러오는 중..." />}>
    <ChallengePageContent />
  </Suspense>
);

export default ChallengePage;
