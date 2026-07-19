"use client";

import { useQuery } from "@tanstack/react-query";

import { getMe } from "./api";

export const useMe = () =>
  useQuery({
    queryKey: ["auth", "me"],
    queryFn: getMe,
    retry: false,
  });
