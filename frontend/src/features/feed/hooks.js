"use client";

import { useInfiniteQuery } from "@tanstack/react-query";

import { getPublicFeed } from "./api";
import { feedKeys } from "./queryKeys";

const PAGE_SIZE = 20;

export const usePublicFeed = (filter) =>
  useInfiniteQuery({
    queryKey: feedKeys.public({ filter, sort: "latest" }),
    queryFn: ({ pageParam }) => getPublicFeed({ cursor: pageParam, filter, size: PAGE_SIZE }),
    initialPageParam: null,
    getNextPageParam: (lastPage) => {
      if (!lastPage || lastPage.length < PAGE_SIZE) return undefined;
      return lastPage[lastPage.length - 1]?.postId;
    },
  });
