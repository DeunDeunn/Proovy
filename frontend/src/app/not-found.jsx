import Link from "next/link";
import Button from "@/components/ui/Button";

const NotFound = () => {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-4 text-center">
      <p className="text-6xl font-bold text-primary">404</p>
      <p className="text-lg font-semibold text-gray-800">페이지를 찾을 수 없어요</p>
      <p className="text-sm text-gray-500">주소가 변경되었거나 존재하지 않는 페이지예요.</p>
      <Link href="/">
        <Button className="mt-2">홈으로 돌아가기</Button>
      </Link>
    </div>
  );
};

export default NotFound;
