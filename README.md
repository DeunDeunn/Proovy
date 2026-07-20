# Proovy

습관 인증 챌린지를 주제로 한 Spring Boot + Next.js 기반 웹 서비스입니다. 사용자가 챌린지를 만들어 참가비를 걸고, 정해진 기간 동안 인증 게시물을 올려 습관을 이어가면, 종료 후 성공/실패 여부에 따라 참가비가 정산되는 구조입니다.

## 기술 스택

**Backend**
- Java 21, Spring Boot 4
- Spring Security + OAuth2 Client (구글/카카오 소셜 로그인), JWT
- MyBatis, PostgreSQL, Flyway
- Redis
- WebSocket(STOMP) 기반 실시간 채팅
- AWS S3 (이미지 업로드)
- Spring AI + Gemini (챌린지 인증 게시물 AI 검수)
- 네이버페이 연동 (캐시 충전 결제)

**Frontend**
- Next.js 16 (App Router), React 19
- TanStack Query, Zustand
- Tailwind CSS 4
- axios (Next.js `rewrites`를 통해 백엔드와 same-origin으로 통신)

**Infra**
- Docker Compose (로컬 PostgreSQL/Redis)
- GitHub Actions → EC2 배포

## 주요 기능

- **챌린지**: 챌린지 생성/참가/탈퇴, 카테고리 분류, 카테고리별/피드 조회
- **인증 게시물**: 습관 인증 게시물 작성/수정, 댓글, 좋아요, 신고, AI 자동 검수
- **정산**: 챌린지 종료 후 성공/실패자 정산, 참가비 반환 및 수익 분배, 방장 수수료 지급, 정산 내역 조회
- **지갑/캐시**: 네이버페이 충전, 참가비 홀딩/환급, 리워드·충전 캐시 구분 출금, 거래 내역 조회
- **소셜 로그인**: 구글/카카오 OAuth2 로그인, 토큰 재발급, 재인증, 회원 탈퇴
- **채팅**: 챌린지별 채팅방, 실시간 메시지(WebSocket)
- **팔로우 / 알림 / 마이페이지**: 유저 팔로우, 알림, 프로필/활동 내역 관리

## 프로젝트 구조

```
.
├── proovy/                     # Spring Boot 백엔드
│   └── src/main/java/com/deundeun/
│       ├── auth/                # 인증/유저 프로필
│       ├── challenge/           # 챌린지/카테고리
│       ├── certification/       # 인증 게시물/댓글/신고
│       ├── pay/                 # 지갑/충전/출금/정산
│       ├── chat/                # 채팅
│       ├── follow/              # 팔로우
│       ├── notification/        # 알림
│       ├── mypage/              # 마이페이지
│       ├── ai/                  # AI 인증 검수
│       └── global/              # 공통 설정(Security, JWT, 예외처리 등)
├── frontend/                   # Next.js 프론트엔드
│   └── src/
│       ├── app/                 # 라우트(App Router)
│       ├── features/            # 기능별 페이지/훅/API
│       └── components/          # 공통 UI 컴포넌트
├── docker-compose.local.yml    # 로컬 PostgreSQL/Redis
└── .github/workflows/          # CI/CD (백엔드 EC2 배포)
```
