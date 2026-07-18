"use client";

import { useInfiniteQuery } from "@tanstack/react-query";

import { getChallengeFeed } from "./api";

const PAGE_SIZE = 20;

export const useChallengeFeed = (challengeId, filter) =>
  useInfiniteQuery({
    queryKey: ["challenge-feed", challengeId, { filter, sort: "latest" }],
    queryFn: ({ pageParam }) =>
      getChallengeFeed(challengeId, { cursor: pageParam, filter, size: PAGE_SIZE }),
    initialPageParam: null,
    enabled: !!challengeId,
    getNextPageParam: (lastPage) => {
      if (!lastPage || lastPage.length < PAGE_SIZE) return undefined;
      return lastPage[lastPage.length - 1]?.postId;
    },
  });
