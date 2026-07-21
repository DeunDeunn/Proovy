const getAvatarInitial = (nickname) => Array.from(nickname?.trim() || "?")[0];

const ProfileAvatar = ({ nickname, profileImageUrl }) =>
  profileImageUrl ? (
    // eslint-disable-next-line @next/next/no-img-element -- S3 외부 이미지 URL이라 next/image 대상 아님
    <img
      src={profileImageUrl}
      alt={`${nickname ?? "사용자"} 프로필 이미지`}
      className="h-9 w-9 shrink-0 rounded-full border border-gray-200 object-cover"
    />
  ) : (
    <span
      aria-label={`${nickname ?? "사용자"} 프로필 이미지`}
      className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary-light text-sm font-bold text-primary"
    >
      {getAvatarInitial(nickname)}
    </span>
  );

export default ProfileAvatar;
