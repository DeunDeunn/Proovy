"use client";

import { useCallback, useRef, useState } from "react";
import { Calendar, ChevronLeft, ChevronRight } from "lucide-react";

import { useDismissable } from "./useDismissable";

const WEEKDAYS = ["일", "월", "화", "수", "목", "금", "토"];

const toKey = (year, month, day) =>
  `${year}-${String(month + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;

const formatDisplay = (dateStr) => {
  if (!dateStr) return "";
  const [y, m, d] = dateStr.split("-");
  return `${y}.${m}.${d}`;
};

// 시작일/종료일에 쓰는 커스텀 달력 - 네이티브 <input type="date">의 UI를 대체한다
const DateField = ({
  value,
  onChange,
  min,
  placeholder = "날짜 선택",
  ariaLabel,
  disabled = false,
}) => {
  const [open, setOpen] = useState(false);
  const [viewYear, setViewYear] = useState(() =>
    value ? Number(value.split("-")[0]) : new Date().getFullYear()
  );
  const [viewMonth, setViewMonth] = useState(() =>
    value ? Number(value.split("-")[1]) - 1 : new Date().getMonth()
  );
  const containerRef = useRef(null);
  const closePicker = useCallback(() => setOpen(false), []);
  useDismissable(open, containerRef, closePicker);

  const openPicker = () => {
    if (value) {
      setViewYear(Number(value.split("-")[0]));
      setViewMonth(Number(value.split("-")[1]) - 1);
    }
    setOpen((prev) => !prev);
  };

  const goPrevMonth = () => {
    if (viewMonth === 0) {
      setViewYear((y) => y - 1);
      setViewMonth(11);
    } else {
      setViewMonth((m) => m - 1);
    }
  };

  const goNextMonth = () => {
    if (viewMonth === 11) {
      setViewYear((y) => y + 1);
      setViewMonth(0);
    } else {
      setViewMonth((m) => m + 1);
    }
  };

  const firstWeekday = new Date(viewYear, viewMonth, 1).getDay();
  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();
  const cells = [
    ...Array.from({ length: firstWeekday }, () => null),
    ...Array.from({ length: daysInMonth }, (_, i) => i + 1),
  ];

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        aria-label={ariaLabel}
        onClick={openPicker}
        disabled={disabled}
        className="flex w-full items-center gap-2 rounded-xl border border-gray-200 px-3.5 py-2.5 text-left text-sm text-gray-900 outline-none transition-shadow hover:border-gray-300 focus:border-primary focus:ring-4 focus:ring-primary/10 disabled:cursor-not-allowed disabled:bg-gray-50 disabled:text-gray-400 disabled:hover:border-gray-200"
      >
        <Calendar size={16} className="shrink-0 text-gray-400" />
        <span className={value ? "" : "text-gray-400"}>
          {value ? formatDisplay(value) : placeholder}
        </span>
      </button>

      {open && (
        <div className="absolute z-20 mt-2 w-72 rounded-2xl border border-gray-100 bg-white p-4 shadow-lg">
          <div className="mb-3 flex items-center justify-between">
            <button
              type="button"
              onClick={goPrevMonth}
              className="rounded-lg p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
            >
              <ChevronLeft size={18} />
            </button>
            <span className="text-sm font-bold text-gray-900">
              {viewYear}년 {viewMonth + 1}월
            </span>
            <button
              type="button"
              onClick={goNextMonth}
              className="rounded-lg p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
            >
              <ChevronRight size={18} />
            </button>
          </div>

          <div className="grid grid-cols-7 gap-1 text-center text-xs text-gray-400">
            {WEEKDAYS.map((weekday) => (
              <span key={weekday} className="py-1">
                {weekday}
              </span>
            ))}
          </div>

          <div className="grid grid-cols-7 gap-1">
            {cells.map((day, index) => {
              if (day === null) return <span key={`empty-${index}`} />;
              const key = toKey(viewYear, viewMonth, day);
              const isSelected = key === value;
              const isDisabled = min ? key < min : false;
              return (
                <button
                  key={key}
                  type="button"
                  disabled={isDisabled}
                  onClick={() => {
                    onChange(key);
                    setOpen(false);
                  }}
                  className={`rounded-full py-1.5 text-sm transition-colors ${
                    isSelected
                      ? "bg-primary font-semibold text-white"
                      : isDisabled
                        ? "cursor-not-allowed text-gray-200"
                        : "text-gray-700 hover:bg-primary-light hover:text-primary"
                  }`}
                >
                  {day}
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};

export default DateField;
