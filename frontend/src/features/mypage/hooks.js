"use client";

import { useQuery } from "@tanstack/react-query";

import { getMyPage } from "./api";

export const useMyPage = () =>
  useQuery({
    queryKey: ["mypage"],
    queryFn: getMyPage,
  });
