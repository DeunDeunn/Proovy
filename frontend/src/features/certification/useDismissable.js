"use client";

import { useEffect } from "react";

export const useDismissable = (isOpen, elementRef, onClose) => {
  useEffect(() => {
    if (!isOpen) return undefined;

    const closeOnOutsidePointerDown = (event) => {
      if (!elementRef.current?.contains(event.target)) {
        onClose();
      }
    };
    const closeOnEscape = (event) => {
      if (event.key === "Escape") {
        onClose();
      }
    };

    document.addEventListener("pointerdown", closeOnOutsidePointerDown);
    document.addEventListener("keydown", closeOnEscape);
    return () => {
      document.removeEventListener("pointerdown", closeOnOutsidePointerDown);
      document.removeEventListener("keydown", closeOnEscape);
    };
  }, [elementRef, isOpen, onClose]);
};
