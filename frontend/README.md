# Proovy Frontend

습관인증 챌린지 서비스 Proovy의 프론트엔드입니다. Next.js로 만들었고, 백엔드(Spring Boot)와 한 저장소 안에서 같이 관리돼요 (모노레포).

이 문서는 "처음 이 프로젝트를 열었을 때 뭘 어떻게 하면 되는지"를 안내하는 문서예요. 순서대로 따라오시면 됩니다.

---

## 1. 처음 시작할 때 (한 번만 하면 됨)

### 1-1. Node 버전 맞추기

이 프로젝트는 Node 24(LTS)를 기준으로 만들었어요. `nvm`(Node 버전 관리 도구)이 설치되어 있다면:

```bash
nvm use
```

`.nvmrc` 파일을 보고 알아서 24 버전으로 맞춰줘요. nvm이 없다면 [nvm 설치](https://github.com/nvm-sh/nvm)부터 하거나, 그냥 본인 Node 버전으로 진행해도 대부분 문제없어요.

### 1-2. 환경변수 파일 만들기

`.env.local`이라는 파일에 로컬 개발용 설정값을 넣는데, 이 파일은 깃에 안 올라가요(각자 컴퓨터마다 있어야 하는 파일이라서). 대신 `.env.local.example`이라는 "견본"이 깃에 있으니, 그걸 복사해서 쓰면 돼요:

```bash
cp .env.local.example .env.local
```

### 1-3. 패키지 설치

```bash
npm install
```

`package.json`에 적힌 라이브러리들(Next.js, React Query 등)을 전부 내려받는 명령이에요.

---

## 2. 개발할 때마다 (매번 해야 함)

프론트엔드 혼자만 켜서는 화면이 제대로 안 나와요. **백엔드도 같이 켜져 있어야** 챌린지 목록 같은 실제 데이터를 볼 수 있어요.

1. **백엔드 실행** — IntelliJ에서 `ProovyApplication` 실행 (포트 8080)
2. **DB/Redis 실행** — 저장소 루트에서 `docker compose -f docker-compose.local.yml up -d`
3. **프론트엔드 실행** — `frontend` 폴더에서:

```bash
npm run dev
```

브라우저에서 `http://localhost:3000` 열면 됩니다.

> 왜 백엔드가 필요한가요? 프론트가 `/api/...`로 보내는 요청을, `next.config.mjs`에 설정된 프록시가 백엔드(8080)로 그대로 전달해주는 구조라서예요. 백엔드가 꺼져 있으면 그 전달이 실패해요.

---

## 3. 폴더 구조 — 어디에 뭘 만들어야 하나요

가장 중요한 규칙 한 줄: **"페이지 껍데기는 `app/`에, 실제 코드는 `features/`에"**

```
src/
├── app/            # 라우팅 전용. 각 page.jsx는 딱 한 줄짜리 "연결" 파일
├── features/       # 도메인별 진짜 코드가 여기 다 있음 (여러분이 작업할 곳!)
│   └── <도메인>/
│       ├── api.js       # 백엔드 API 호출 함수들
│       ├── hooks.js     # api.js를 React Query로 감싼 훅
│       └── XxxPage.jsx  # 실제 화면 컴포넌트
├── components/
│   ├── ui/         # 여러 페이지에서 재사용하는 공통 부품 (버튼, 로딩 등)
│   └── layout/     # 사이드바 등 레이아웃
└── lib/            # axios 설정 등 전역 설정 (거의 건드릴 일 없음)
```

### 왜 이렇게 나눴나요?

`app/challenges/page.jsx` 파일을 열어보면 내용이 딱 한 줄이에요:

```jsx
export { default } from "@/features/challenge/ChallengePage";
```

"이 주소(`/challenges`)로 오면 `features/challenge/ChallengePage`를 보여줘라"는 뜻이에요. **실제 화면을 만들 때는 `app/` 폴더는 건드릴 일이 거의 없고, `features/도메인이름/` 폴더 안에서만 작업하면 됩니다.**

이렇게 나눈 이유는, Next.js의 라우팅 규칙(폴더 구조 = URL 주소)과 실제 로직을 분리해서, 나중에 페이지 URL 구조가 조금 바뀌어도 실제 코드(`features/`)는 그대로 두고 `app/`의 연결 파일만 고치면 되게 하기 위해서예요.

---

## 4. API 연동하는 법 (예시: challenge 도메인)

`features/challenge/` 폴더를 그대로 참고하면 돼요. 이미 실제로 동작하는 코드가 들어있어요.

### 4-1. `api.js` — 백엔드에 요청 보내는 함수

```js
import api from "@/lib/api";

export const getChallenges = (params) => api.get("/challenges", { params });
```

`api`는 `lib/api.js`에 미리 설정해둔 axios예요. 성공하면 응답 데이터만 바로 돌려주고, 실패하면 `{code, message}` 형태의 에러를 던지도록 만들어져 있어서, 우리는 그냥 `api.get(...)`, `api.post(...)`만 부르면 됩니다.

### 4-2. `queryKeys.js` — 캐시 이름표 관리

```js
export const challengeKeys = {
  all: ["challenges"],
  lists: () => [...challengeKeys.all, "list"],
  list: (params) => [...challengeKeys.lists(), params],
};
```

React Query는 요청 결과를 캐시(임시 저장)해두는데, 그 캐시마다 이름표(키)가 필요해요. 이 이름표들을 여기저기 직접 타이핑하면 오타가 나기 쉬워서, 한 파일에 모아두고 함수로 꺼내 쓰는 방식이에요. **각자 도메인에도 이 파일을 그대로 복사해서 도메인 이름만 바꿔 쓰시면 됩니다.**

### 4-3. `hooks.js` — 화면에서 실제로 쓰는 훅

```js
"use client";
import { useQuery } from "@tanstack/react-query";
import { getChallenges } from "./api";
import { challengeKeys } from "./queryKeys";

export const useChallenges = (params) =>
  useQuery({ queryKey: challengeKeys.list(params), queryFn: () => getChallenges(params) });
```

### 4-4. 페이지 컴포넌트에서 사용

```jsx
"use client";
import { useChallenges } from "./hooks";
import Loading from "@/components/ui/Loading";
import ErrorMessage from "@/components/ui/ErrorMessage";

const ChallengePage = () => {
  const { data, isLoading, error } = useChallenges();

  if (isLoading) return <Loading label="챌린지 불러오는 중..." />;
  if (error) return <ErrorMessage error={error} />;

  return <div>{/* data로 화면 그리기 */}</div>;
};
```

`isLoading`(로딩 중인지), `error`(실패했는지), `data`(성공 시 실제 데이터) 세 가지만 확인하면 대부분의 화면을 만들 수 있어요.

---

## 5. 공통 컴포넌트 (`components/ui/`)

매번 새로 만들지 말고 아래 것들을 가져다 쓰세요. **홈 화면(`features/home/HomePage.jsx`)에 4개 전부 실제로 렌더링해둔 예시가 있으니, 직접 화면에서 보고 코드를 참고하세요.**
공통 컴포넌트로 더 필요한게 있으면 요청해주세요.

```jsx
import Loading from "@/components/ui/Loading";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Button from "@/components/ui/Button";
import Badge from "@/components/ui/Badge";

<Loading />                              {/* 로딩 스피너 */}
<ErrorMessage error={error} />           {/* API 에러 메시지 박스 */}
<Button>기본 버튼</Button>
<Button variant="outline">테두리만</Button>
<Button variant="danger">위험한 동작용</Button>
<Badge variant="success">모집중</Badge>
<Badge variant="gray">종료</Badge>
```

---

## 6. 색깔은 이렇게 써주세요

`bg-blue-600`처럼 Tailwind 기본 색을 직접 쓰지 마시고, 아래 우리 브랜드 색(`src/app/globals.css`에 정의됨)을 쓰세요:

| 클래스                        | 용도                                  |
| ----------------------------- | ------------------------------------- |
| `text-primary` / `bg-primary` | 브랜드 파란색 (버튼, 로고, 활성 메뉴) |
| `bg-primary-light`            | 연한 파랑 배경 (선택된 메뉴 배경 등)  |
| `text-success`                | 성공/모집중 같은 긍정적 표시          |
| `text-danger`                 | 에러, 삭제 같은 위험한 동작           |
| `bg-surface`                  | 카드나 사이드바 같은 흰 배경          |
| `bg-canvas`                   | 페이지 전체 배경 (연한 회색)          |

왜 그냥 `blue-600`을 쓰면 안 되나요? 나중에 브랜드 색이 바뀌면 `globals.css`의 색상 값 한 줄만 고치면 되는데, 다들 `blue-600`을 직접 썼다면 프로젝트 전체를 찾아다니면서 고쳐야 하거든요.

색 추가를 하고 싶다 => 요청해주세요.

---

## 7. 코드 작성 규칙

- 함수는 `function` 대신 `const` + 화살표 함수로 씁니다.
  ```js
  // ❌ 이렇게 안 씀
  function MyComponent() { ... }

  // ✅ 이렇게 씀
  const MyComponent = () => { ... };
  export default MyComponent;
  ```
- 파일 저장하면 Prettier가 자동으로 코드 스타일을 정리해줘요 (VSCode에 Prettier 확장 설치되어 있어야 함).
- 커밋하기 전에 아래 명령이 에러 없이 통과하는지 확인해주세요:
  ```bash
  npx eslint . --max-warnings=0
  ```

---

## 8. 막힐 때

- `/api/...` 요청이 계속 실패한다 → 백엔드(8080)가 켜져 있는지, Docker(DB/Redis)가 떠 있는지 확인
- 화면에 아무것도 안 뜬다 → 브라우저 개발자도구(F12) 콘솔에 에러가 있는지 확인
- 어떻게 짜야 할지 감이 안 잡힌다 → `features/challenge/` 폴더가 실제로 동작하는 예시니 그대로 참고하세요
- 그래도 모르겠다 → 다른 팀원에게 편하게 물어보세요!
