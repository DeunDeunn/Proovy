const Button = ({ children, variant = "primary", className = "", type = "button", ...props }) => {
  const base =
    "cursor-pointer rounded-lg px-4 py-2 text-sm font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-50";
  const variants = {
    primary: "bg-primary text-white hover:bg-primary-hover",
    outline: "border border-gray-300 text-gray-700 hover:bg-gray-50",
    danger: "bg-danger text-white hover:bg-red-700",
  };

  return (
    <button className={`${base} ${variants[variant]} ${className}`} {...props}>
      {children}
    </button>
  );
};
export default Button;
