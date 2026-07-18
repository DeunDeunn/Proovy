const Loading = ({ label = "불러오는 중..." }) => {
  return (
    <div className="flex items-center justify-center gap-2 py-12 text-gray-500">
      <div className="h-4 w-4 animate-spin rounded-full border-2 border-gray-300 border-t-primary" />
      <span className="text-sm">{label}</span>
    </div>
  );
};
export default Loading;
