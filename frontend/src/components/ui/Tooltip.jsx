const POSITION_STYLES = {
  top: {
    bubble: "bottom-full mb-2",
    arrow: "top-full border-t-gray-900",
  },
  bottom: {
    bubble: "top-full mt-2",
    arrow: "bottom-full border-b-gray-900",
  },
};

const ALIGN_STYLES = {
  center: { bubble: "left-1/2 -translate-x-1/2", arrow: "left-1/2 -translate-x-1/2" },
  start: { bubble: "left-0", arrow: "left-3" },
};

const Tooltip = ({ text, children, position = "top", align = "center" }) => {
  const { bubble: positionBubble, arrow: positionArrow } = POSITION_STYLES[position];
  const { bubble: alignBubble, arrow: alignArrow } = ALIGN_STYLES[align];

  return (
    <span className="group relative inline-flex">
      {children}
      <span
        className={`pointer-events-none absolute z-10 w-56 rounded-lg bg-gray-900 px-3 py-2 text-xs font-normal leading-relaxed text-white opacity-0 shadow-lg transition-opacity group-hover:opacity-100 ${positionBubble} ${alignBubble}`}
      >
        {text}
        <span className={`absolute border-4 border-transparent ${positionArrow} ${alignArrow}`} />
      </span>
    </span>
  );
};

export default Tooltip;
