"use client";

import { useEffect, useRef, useState } from "react";
import { Clock } from "lucide-react";

// min~max 사이를 30분 단위로 끊은 선택지 목록을 만든다
const buildOptions = (min, max) => {
  const [minH, minM] = min.split(":").map(Number);
  const [maxH, maxM] = max.split(":").map(Number);
  const start = minH * 60 + minM;
  const end = maxH * 60 + maxM;

  const options = [];
  for (let minutes = start; minutes <= end; minutes += 30) {
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    options.push(`${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}`);
  }
  return options;
};

const formatDisplay = (time) => {
  if (!time) return "";
  const [h, m] = time.split(":").map(Number);
  const period = h < 12 ? "오전" : "오후";
  const hour12 = h % 12 === 0 ? 12 : h % 12;
  return `${period} ${hour12}:${String(m).padStart(2, "0")}`;
};

// 인증 가능 시간에 쓰는 커스텀 시간 선택기 - 네이티브 <input type="time">의 UI를 대체한다
const TimeField = ({ value, onChange, min, max, ariaLabel, disabled = false }) => {
  const [open, setOpen] = useState(false);
  const containerRef = useRef(null);
  const options = buildOptions(min, max);

  useEffect(() => {
    if (!open) return undefined;
    const handleClickOutside = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [open]);

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        aria-label={ariaLabel}
        onClick={() => setOpen((prev) => !prev)}
        disabled={disabled}
        className="flex w-full items-center gap-2 rounded-xl border border-gray-200 px-3.5 py-2.5 text-left text-sm text-gray-900 outline-none transition-shadow hover:border-gray-300 focus:border-primary focus:ring-4 focus:ring-primary/10 disabled:cursor-not-allowed disabled:bg-gray-50 disabled:text-gray-400 disabled:hover:border-gray-200"
      >
        <Clock size={16} className="shrink-0 text-gray-400" />
        <span>{formatDisplay(value)}</span>
      </button>

      {open && (
        <div className="absolute z-20 mt-2 max-h-56 w-36 overflow-y-auto rounded-2xl border border-gray-100 bg-white p-1.5 shadow-lg">
          {options.map((option) => {
            const isSelected = option === value;
            return (
              <button
                key={option}
                type="button"
                onClick={() => {
                  onChange(option);
                  setOpen(false);
                }}
                className={`block w-full rounded-lg px-3 py-2 text-left text-sm transition-colors ${
                  isSelected
                    ? "bg-primary-light font-semibold text-primary"
                    : "text-gray-700 hover:bg-gray-50"
                }`}
              >
                {formatDisplay(option)}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default TimeField;
