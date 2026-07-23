import localFont from "next/font/local";
import "./globals.css";
import Providers from "./providers";
import Sidebar from "@/components/layout/Sidebar";

// Pretendard Variable (SIL OFL 1.1) — https://github.com/orioncactus/pretendard
// 한글 커버리지가 없는 Geist 대신 사용: OS 기본 폰트로 폴백되면 Windows에서 맑은 고딕으로 렌더링되는 문제를 막는다.
const pretendard = localFont({
  src: "./fonts/PretendardVariable.woff2",
  variable: "--font-pretendard",
  weight: "45 920",
  display: "swap",
});

export const metadata = {
  title: "Proovy",
  description: "습관인증 챌린지 서비스",
};

const RootLayout = ({ children }) => {
  return (
    <html lang="ko" className={`${pretendard.variable} h-full antialiased`}>
      <body className="h-full">
        <Providers>
          <div className="flex h-screen overflow-hidden">
            <Sidebar />
            <main className="flex-1 overflow-y-auto bg-canvas p-8">{children}</main>
          </div>
        </Providers>
      </body>
    </html>
  );
};

export default RootLayout;
