import Badge from "@/components/ui/Badge";
import Button from "@/components/ui/Button";
import ErrorMessage from "@/components/ui/ErrorMessage";
import Loading from "@/components/ui/Loading";

const HomePage = () => {
  return (
    <div>
      <Loading /> <ErrorMessage /> <Button>챌린지 둘러보기</Button>{" "}
      <Button variant="outline">상점에서 포인트 충전</Button>
      <Badge variant="success">모집중</Badge>
      <Badge variant="gray">종료</Badge>
    </div>
  );
};

export default HomePage;
