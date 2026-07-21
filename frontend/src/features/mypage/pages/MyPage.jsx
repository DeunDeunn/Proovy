"use client";

/* eslint-disable @next/next/no-img-element -- S3 외부 프로필 이미지 URL은 현재 next/image 설정 대상이 아니다. */

import { useRef, useState } from "react";
import Link from "next/link";
import { Award, Camera, Check, Pencil, X } from "lucide-react";

import Card from "@/components/ui/Card";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";
import {
  useCheckNicknameDuplicate,
  useUpdateNickname,
  useUpdateProfileImage,
} from "@/features/auth/hooks";
import { DEFAULT_PROFILE_IMAGE_URL } from "@/lib/constants";

import { useMyPage } from "../hooks";

const PREVIEW_SIZE = 3;
const NICKNAME_MIN = 2;
const NICKNAME_MAX = 10;

const isValidNicknameFormat = (nickname) =>
  nickname.trim().length >= NICKNAME_MIN && nickname.trim().length <= NICKNAME_MAX;

const formatJoinDate = (value) => {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}.${m}.${d}`;
};

const getDayCount = (startDate) => {
  if (!startDate) return null;
  const start = new Date(startDate);
  const today = new Date();
  start.setHours(0, 0, 0, 0);
  today.setHours(0, 0, 0, 0);
  const diffDays = Math.floor((today - start) / (1000 * 60 * 60 * 24));
  return Math.max(diffDays + 1, 1);
};

const VERIFICATION_STATUS_META = {
  PENDING: { text: "신청 완료 · 심사 중", action: "자세히 보기" },
  APPROVED: { text: "우수 사용자로 인증됐어요", action: "자세히 보기" },
  REJECTED: { text: "신청이 반려됐어요", action: "자세히 보기" },
  REVOKED: { text: "인증이 취소됐어요", action: "자세히 보기" },
};
const DEFAULT_VERIFICATION_META = { text: "우수 사용자 인증을 신청해보세요", action: "신청하기" };

const ChallengeSummaryCard = ({ title, count, challenges, emptyText, renderRight }) => (
  <Card>
    <h2 className="mb-4 text-sm font-semibold text-gray-900">
      {title} {count}
    </h2>

    {challenges.length === 0 ? (
      <p className="py-4 text-center text-sm text-gray-400">{emptyText}</p>
    ) : (
      <ul className="mb-4 flex flex-col gap-3">
        {challenges.slice(0, PREVIEW_SIZE).map((challenge) => (
          <li key={challenge.id} className="flex items-center justify-between text-sm">
            <span className="truncate text-gray-700">{challenge.title}</span>
            <span className="shrink-0 text-gray-500">{renderRight(challenge)}</span>
          </li>
        ))}
      </ul>
    )}

    <Link
      href="/my-challenges"
      className="block w-full cursor-pointer rounded-lg border border-gray-300 px-4 py-2 text-center text-sm font-semibold text-gray-700 transition-colors hover:bg-gray-50"
    >
      전체 보기
    </Link>
  </Card>
);

const MyPage = () => {
  const { data: me, isLoading, isError, error } = useMyPage();

  const fileInputRef = useRef(null);
  const updateProfileImage = useUpdateProfileImage();

  const [isEditingNickname, setIsEditingNickname] = useState(false);
  const [nicknameInput, setNicknameInput] = useState("");
  const [checkedNickname, setCheckedNickname] = useState(null);
  const checkDuplicate = useCheckNicknameDuplicate();
  const updateNickname = useUpdateNickname();

  if (isLoading) return <Loading />;
  if (isError) return <ErrorMessage error={error} />;
  if (!me) return null;

  const verificationMeta = VERIFICATION_STATUS_META[me.verificationStatus] ?? DEFAULT_VERIFICATION_META;
  const normalizedNicknameInput = nicknameInput.trim();
  const isNicknameChecked =
    checkedNickname === normalizedNicknameInput && checkDuplicate.data?.available === true;

  const handleAvatarClick = () => fileInputRef.current?.click();

  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    updateProfileImage.mutate(file);
  };

  const startEditingNickname = () => {
    setNicknameInput(me.nickname);
    setCheckedNickname(null);
    setIsEditingNickname(true);
  };

  const cancelEditingNickname = () => {
    setIsEditingNickname(false);
    checkDuplicate.reset();
    updateNickname.reset();
  };

  const handleNicknameInputChange = (e) => {
    setNicknameInput(e.target.value);
    setCheckedNickname(null);
  };

  const handleCheckDuplicate = () => {
    if (!isValidNicknameFormat(normalizedNicknameInput) || normalizedNicknameInput === me.nickname) return;
    checkDuplicate.mutate(normalizedNicknameInput, {
      onSuccess: () => setCheckedNickname(normalizedNicknameInput),
    });
  };

  const handleSaveNickname = () => {
    if (!isNicknameChecked) return;
    updateNickname.mutate(checkedNickname, {
      onSuccess: () => setIsEditingNickname(false),
    });
  };

  return (
    <div className="flex flex-col gap-4">
      {/* 프로필 요약 */}
      <div className="flex items-center gap-4">
        <div className="relative shrink-0">
          <img
            src={me.profileImageUrl || DEFAULT_PROFILE_IMAGE_URL}
            alt={`${me.nickname} 프로필 이미지`}
            className="h-16 w-16 rounded-full border border-gray-200 object-cover"
          />
          <button
            type="button"
            onClick={handleAvatarClick}
            disabled={updateProfileImage.isPending}
            title="프로필 사진 변경"
            className="absolute -bottom-1 -right-1 flex h-6 w-6 cursor-pointer items-center justify-center rounded-full border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <Camera size={12} />
          </button>
          <input ref={fileInputRef} type="file" accept="image/*" className="hidden" onChange={handleFileChange} />
        </div>

        <div className="flex flex-col gap-1">
          {isEditingNickname ? (
            <div className="flex flex-col gap-2">
              <div className="flex items-center gap-2">
                <input
                  type="text"
                  value={nicknameInput}
                  onChange={handleNicknameInputChange}
                  maxLength={NICKNAME_MAX}
                  autoFocus
                  aria-label="닉네임"
                  className="rounded-lg border border-gray-300 px-2 py-1 text-sm outline-none focus:border-primary"
                />
                <button
                  type="button"
                  onClick={handleCheckDuplicate}
                  disabled={
                    !isValidNicknameFormat(normalizedNicknameInput) ||
                    normalizedNicknameInput === me.nickname ||
                    checkDuplicate.isPending
                  }
                  className="cursor-pointer rounded-lg border border-gray-300 px-2 py-1 text-xs font-semibold text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  중복확인
                </button>
                <button
                  type="button"
                  onClick={handleSaveNickname}
                  disabled={!isNicknameChecked || updateNickname.isPending}
                  title="저장"
                  className="cursor-pointer rounded-lg p-1 text-primary hover:bg-primary-light disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <Check size={16} />
                </button>
                <button
                  type="button"
                  onClick={cancelEditingNickname}
                  title="취소"
                  className="cursor-pointer rounded-lg p-1 text-gray-400 hover:bg-gray-50"
                >
                  <X size={16} />
                </button>
              </div>
              {isNicknameChecked && <p className="text-xs text-primary">사용할 수 있는 닉네임이에요.</p>}
              {checkDuplicate.isError && <ErrorMessage error={checkDuplicate.error} />}
              {updateNickname.isError && <ErrorMessage error={updateNickname.error} />}
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <span className="text-lg font-bold text-gray-900">{me.nickname}</span>
              {me.verified && (
                <span className="flex items-center gap-1 rounded-full bg-amber-50 px-2 py-0.5 text-xs font-medium text-amber-600">
                  <Award size={12} />
                  우수 사용자
                </span>
              )}
              <button
                type="button"
                onClick={startEditingNickname}
                title="닉네임 수정"
                className="cursor-pointer rounded-lg p-1 text-gray-400 hover:bg-gray-50 hover:text-gray-600"
              >
                <Pencil size={14} />
              </button>
            </div>
          )}

          <span className="text-sm text-gray-500">가입일 {formatJoinDate(me.createdAt)}</span>
          <div className="flex gap-3 text-sm text-gray-700">
            <Link href={`/users/${me.userId}/followers`} className="hover:underline">
              <strong className="font-semibold">{me.followerCount}</strong> 팔로워
            </Link>
            <Link href={`/users/${me.userId}/following`} className="hover:underline">
              <strong className="font-semibold">{me.followingCount}</strong> 팔로잉
            </Link>
          </div>
        </div>
      </div>

      {updateProfileImage.isError && <ErrorMessage error={updateProfileImage.error} />}

      {/* 챌린지 요약 */}
      <div className="grid grid-cols-2 gap-4">
        <ChallengeSummaryCard
          title="참여 중인 챌린지"
          count={me.participatingChallenges.length}
          challenges={me.participatingChallenges}
          emptyText="참여 중인 챌린지가 없어요"
          renderRight={(challenge) => {
            const dayCount = getDayCount(challenge.startDate);
            return dayCount ? `${dayCount}일차` : null;
          }}
        />
        <ChallengeSummaryCard
          title="운영 중인 챌린지"
          count={me.hostingChallenges.length}
          challenges={me.hostingChallenges}
          emptyText="운영 중인 챌린지가 없어요"
          renderRight={(challenge) => `${challenge.currentParticipants ?? 0}명`}
        />
      </div>

      {/* 우수 사용자 인증 */}
      <Card className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Award size={18} className="text-amber-500" />
          <div className="flex flex-col">
            <span className="text-sm font-semibold text-gray-900">우수 사용자 인증</span>
            <span className="text-sm text-gray-500">{verificationMeta.text}</span>
          </div>
        </div>
        <Link
          href="/mypage/verification"
          className="cursor-pointer rounded-lg border border-gray-300 px-4 py-2 text-sm font-semibold text-gray-700 transition-colors hover:bg-gray-50"
        >
          {verificationMeta.action}
        </Link>
      </Card>
    </div>
  );
};

export default MyPage;
