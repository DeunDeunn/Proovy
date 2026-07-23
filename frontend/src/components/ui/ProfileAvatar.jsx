import { DEFAULT_PROFILE_IMAGE_URL } from "@/lib/constants";

const ProfileAvatar = ({ nickname, profileImageUrl, size = "h-9 w-9" }) => (
  // eslint-disable-next-line @next/next/no-img-element -- S3 외부 이미지 URL이라 next/image 대상 아님
  <img
    src={profileImageUrl || DEFAULT_PROFILE_IMAGE_URL}
    alt={`${nickname ?? "사용자"} 프로필 이미지`}
    className={`${size} shrink-0 rounded-full border border-gray-200 object-cover`}
  />
);

export default ProfileAvatar;
