"use client";

import { useQuery } from "@tanstack/react-query";
import { getMyMaxCertificationStreak, getPopularFeed } from "./api";

export const usePopularFeed = () =>
  useQuery({ queryKey: ["home", "popularFeed"], queryFn: getPopularFeed });

export const useMyMaxCertificationStreak = () =>
  useQuery({ queryKey: ["home", "maxCertificationStreak"], queryFn: getMyMaxCertificationStreak });
