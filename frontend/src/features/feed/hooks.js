"use client";

import { useInfiniteQuery } from "@tanstack/react-query";

import { getPublicFeed } from "./api";
import { feedKeys } from "./queryKeys";

const PAGE_SIZE = 20;

export const usePublicFeed = (filter, sort) =>
  useInfiniteQuery({
    queryKey: feedKeys.public({ filter, sort }),
    queryFn: ({ pageParam }) =>
      getPublicFeed({
        cursor: pageParam?.cursor,
        cursorLike: pageParam?.cursorLike,
        filter,
        sort,
        size: PAGE_SIZE,
      }),
    initialPageParam: { cursor: null, cursorLike: null },
    getNextPageParam: (lastPage) => {
      if (!lastPage || lastPage.length < PAGE_SIZE) return undefined;
      const lastPost = lastPage[lastPage.length - 1];
      return {
        cursor: lastPost?.postId,
        cursorLike: sort === "popular" ? lastPost?.likeCount : null,
      };
    },
  });
