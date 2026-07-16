"use client";

import { useQuery } from "@tanstack/react-query";
import api from "@/lib/api";

export default function Home() {
  const {
    data: categories,
    isLoading,
    error,
  } = useQuery({
    queryKey: ["categories"],
    queryFn: () => api.get("/challenges/categories"),
  });

  if (isLoading) return <p className="p-8">불러오는 중...</p>;
  if (error) return <p className="p-8 text-red-500">{error.message}</p>;

  return (
    <main className="p-8">
      <h1 className="text-2xl font-bold mb-4">카테고리</h1>
      <ul className="flex gap-2">
        {categories.map((c) => (
          <li key={c.id} className="rounded-full bg-gray-200 px-4 py-1">
            {c.name}
          </li>
        ))}
      </ul>
    </main>
  );
}
