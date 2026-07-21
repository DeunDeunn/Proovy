"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

import Button from "@/components/ui/Button";
import ErrorMessage from "@/components/ui/ErrorMessage";

import { useCheckNicknameDuplicate, useUpdateNickname } from "./hooks";

const isValidNicknameFormat = (nickname) => nickname.length >= 2 && nickname.length <= 10;

const CompleteProfilePage = () => {
  const router = useRouter();
  const [nickname, setNickname] = useState("");
  const [checkedNickname, setCheckedNickname] = useState(null);

  const checkDuplicate = useCheckNicknameDuplicate();
  const updateNickname = useUpdateNickname();

  const normalizedNickname = nickname.trim();
  const isChecked = checkedNickname === normalizedNickname && checkDuplicate.data?.available === true;

  const handleNicknameChange = (e) => {
    setNickname(e.target.value);
    setCheckedNickname(null);
  };

  const handleCheckDuplicate = () => {
    if (!isValidNicknameFormat(normalizedNickname)) return;
    checkDuplicate.mutate(normalizedNickname, {
      onSuccess: () => setCheckedNickname(normalizedNickname),
    });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!isChecked) return;
    updateNickname.mutate(checkedNickname, {
      onSuccess: () => router.replace("/"),
    });
  };

  return (
    <div className="flex h-full flex-col items-center justify-center gap-8">
      <div className="flex flex-col items-center gap-2">
        <h1 className="text-xl font-bold text-gray-900">닉네임을 설정해주세요</h1>
        <p className="text-sm text-gray-500">Proovy에서 사용할 닉네임이에요 (2~10자)</p>
      </div>

      <form onSubmit={handleSubmit} className="flex w-full max-w-xs flex-col gap-3">
        <div className="flex gap-2">
          <input
            type="text"
            value={nickname}
            onChange={handleNicknameChange}
            placeholder="닉네임 입력"
            maxLength={10}
            aria-label="닉네임"
            className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none focus:border-primary"
          />
          <Button
            type="button"
            variant="outline"
            onClick={handleCheckDuplicate}
            disabled={!isValidNicknameFormat(normalizedNickname) || checkDuplicate.isPending}
          >
            중복확인
          </Button>
        </div>

        {isChecked && (
          <p className="text-sm text-primary">사용할 수 있는 닉네임이에요.</p>
        )}
        {checkDuplicate.isError && <ErrorMessage error={checkDuplicate.error} />}
        {updateNickname.isError && <ErrorMessage error={updateNickname.error} />}

        <Button type="submit" disabled={!isChecked || updateNickname.isPending}>
          {updateNickname.isPending ? "저장 중..." : "시작하기"}
        </Button>
      </form>
    </div>
  );
};

export default CompleteProfilePage;
