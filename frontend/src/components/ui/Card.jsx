const Card = ({ children, className = "" }) => {
  return (
    <div className={`rounded-xl border border-gray-200 bg-surface p-6 ${className}`}>
      {children}
    </div>
  );
};

export default Card;
