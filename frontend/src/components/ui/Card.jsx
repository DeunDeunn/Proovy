const Card = ({ children, className = "", ...props }) => {
  return (
    <div className={`rounded-xl border border-gray-200 bg-surface p-6 ${className}`} {...props}>
      {children}
    </div>
  );
};

export default Card;
