const variants = {
  primary: "bg-primary-light text-primary",
  success: "bg-green-50 text-success",
  danger: "bg-red-50 text-danger",
  warning: "bg-amber-50 text-amber-700",
  gray: "bg-gray-100 text-gray-600",
};

const Badge = ({ children, variant = "gray" }) => {
  return (
    <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${variants[variant]}`}>
      {children}
    </span>
  );
};

export default Badge;
