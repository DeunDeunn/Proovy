"use client";

import { useQuery } from "@tanstack/react-query";
import {
  getMyMaxCertificationStreak,
  getPopularFeed,
  getTodayCertificationProgress,
} from "./api";

export const usePopularFeed = () =>
  useQuery({ queryKey: ["home", "popularFeed"], queryFn: getPopularFeed });

export const useMyMaxCertificationStreak = () =>
  useQuery({ queryKey: ["home", "maxCertificationStreak"], queryFn: getMyMaxCertificationStreak });

export const useTodayCertificationProgress = () =>
  useQuery({ queryKey: ["home", "todayCertificationProgress"], queryFn: getTodayCertificationProgress });
