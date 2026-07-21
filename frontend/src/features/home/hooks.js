"use client";

import { useQuery } from "@tanstack/react-query";
import { getPopularFeed } from "./api";

export const usePopularFeed = () =>
  useQuery({ queryKey: ["home", "popularFeed"], queryFn: getPopularFeed });
