<!-- <img width="602" height="229" alt="image" src="https://github.com/user-attachments/assets/1bc0ac2b-fef7-4096-abd2-1938aaa5b05d" /> -->

# Proovy
<img width="1706" height="743" alt="image" src="https://github.com/user-attachments/assets/0397fa26-95d9-4d8e-bbd8-b13ff65c4b85" />
습관 인증 챌린지를 주제로 한 Spring Boot + Next.js 기반 웹 서비스입니다. 사용자가 챌린지를 만들어 참가비를 걸고, 정해진 기간 동안 인증 게시물을 올려 습관을 이어가면, 종료 후 성공/실패 여부에 따라 참가비가 정산되는 구조입니다.

## 기술 스택

<img src="https://skillicons.dev/icons?i=java,spring,js,postgresql,redis,aws,nextjs,react,tailwind,docker,vercel" />

**Backend**
- Spring Security + OAuth2 Client (구글/카카오 소셜 로그인), JWT
- MyBatis, Flyway
- WebSocket(STOMP) 기반 실시간 채팅
- AWS S3 (이미지 업로드)
- Spring AI + Gemini (챌린지 인증 게시물 AI 검수)
- 네이버페이 연동 (캐시 충전 결제)

**Frontend**
- TanStack Query, Zustand
- axios (Next.js `rewrites`를 통해 백엔드와 same-origin으로 통신)

## 주요 기능

- **챌린지**: 챌린지 생성/참가/탈퇴, 카테고리 분류, 카테고리별/피드 조회
- **인증 게시물**: 습관 인증 게시물 작성/수정, 댓글, 좋아요, 신고, AI 자동 검수
- **정산**: 챌린지 종료 후 성공/실패자 정산, 참가비 반환 및 수익 분배, 방장 수수료 지급, 정산 내역 조회
- **지갑/캐시**: 네이버페이 충전, 참가비 홀딩/환급, 리워드·충전 캐시 구분 출금, 거래 내역 조회
- **소셜 로그인**: 구글/카카오 OAuth2 로그인, 토큰 재발급, 재인증, 회원 탈퇴
- **채팅**: 챌린지별 채팅방, 실시간 메시지(WebSocket)
- **팔로우 / 알림 / 마이페이지**: 유저 팔로우, 알림, 프로필/활동 내역 관리

## 프로젝트 구조
<img width="815" height="460" alt="image" src="https://github.com/user-attachments/assets/3eed37a8-f344-4da6-8160-40074066f642" />
