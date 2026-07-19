import { CURRENT_USER } from "@/features/chat/currentUser";

const MINUTE = 60 * 1000;

const at = (minutesAgo) => new Date(Date.now() - minutesAgo * MINUTE);

const PARTICIPANTS = {
  hostJae: { id: 201, nickname: "재이", badgeApproved: true },
  minji: { id: 202, nickname: "민지" },
  jungwoo: { id: 203, nickname: "정우" },
  jimin: { id: 204, nickname: "지민" },
  leeJihoon: { id: 101, nickname: "이지훈" },
};

const MESSAGE_TEMPLATES = {
  1: [
    {
      sender: PARTICIPANTS.hostJae,
      content: "여러분 오늘도 수고 많으셨어요! 💙",
      createdAt: at(75),
    },
    {
      sender: PARTICIPANTS.minji,
      content: "오늘 러닝 넘 상쾌했어요! 🏃‍♀️",
      createdAt: at(74),
    },
    {
      sender: PARTICIPANTS.minji,
      messageType: "IMAGE",
      createdAt: at(74),
    },
    {
      sender: PARTICIPANTS.jungwoo,
      content: "삭제된 메시지입니다.",
      createdAt: at(73),
      deleted: true,
    },
    {
      sender: CURRENT_USER,
      content: "저는 오늘 페이스 유지 성공했어요! 🎉",
      createdAt: at(72),
    },
    {
      sender: PARTICIPANTS.jimin,
      messageType: "CERTIFICATION_SHARE",
      createdAt: at(71),
      sharedCertification: {
        authorNickname: "민지",
        challengeTitle: "매일 30분 러닝",
        thumbnailUrl: null,
        certifiedAt: "2024.04.30 (화)",
      },
    },
  ],
  2: [
    {
      sender: PARTICIPANTS.leeJihoon,
      content: "안녕하세요! 😊",
      createdAt: at(62),
    },
    {
      sender: PARTICIPANTS.leeJihoon,
      content: "혹시 오늘 인증 사진 공유 가능할까요?",
      createdAt: at(62),
    },
    {
      sender: CURRENT_USER,
      content: "네! 방금 업로드했어요~",
      createdAt: at(61),
      read: true,
    },
    {
      sender: CURRENT_USER,
      messageType: "CERTIFICATION_SHARE",
      createdAt: at(61),
      sharedCertification: {
        authorNickname: "김프루",
        challengeTitle: "매일 30분 러닝",
        thumbnailUrl: null,
        certifiedAt: "2024.04.30 (화)",
      },
      read: true,
    },
    {
      sender: PARTICIPANTS.leeJihoon,
      content: "확인했어요! 멋져요 🔥",
      createdAt: at(61),
    },
    {
      sender: CURRENT_USER,
      content: "감사합니다! 좋은 하루 보내세요 😊",
      createdAt: at(60),
      read: true,
    },
  ],
  3: [
    {
      sender: { id: 102, nickname: "박민지" },
      content: "인증 사진 확인 부탁드려요~",
      createdAt: at(300),
    },
  ],
};

let messageIdSeq = 1;

const buildMessage = (chatRoomId, template) => ({
  messageId: messageIdSeq++,
  chatRoomId,
  senderId: template.sender.id,
  senderNickname: template.sender.nickname,
  senderBadgeApproved: template.sender.badgeApproved ?? false,
  content: template.deleted ? null : (template.content ?? null),
  messageType: template.messageType ?? "TEXT",
  sharedCertification: template.deleted ? null : (template.sharedCertification ?? null),
  deletedAt: template.deleted ? template.createdAt : null,
  createdAt: template.createdAt,
  read: template.read ?? false,
});

export const createMockMessages = () =>
  Object.fromEntries(
    Object.entries(MESSAGE_TEMPLATES).map(([chatRoomId, templates]) => [
      chatRoomId,
      templates.map((template) => buildMessage(Number(chatRoomId), template)),
    ]),
  );
