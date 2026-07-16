const ErrorMessage = ({ error }) => {
  const message = error?.message ?? "알 수 없는 오류가 발생했습니다.";

  return (
    <div role="alert" className="rounded-lg bg-red-50 px-4 py-3 text-sm text-danger">
      {message}
    </div>
  );
};
export default ErrorMessage;
