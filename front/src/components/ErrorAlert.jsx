const ErrorAlert = ({ message, onClose }) => {
  if (!message) return null;

  return (
    <div className="alert alert-danger d-flex justify-content-between align-items-start" role="alert">
      <span>{message}</span>
      {onClose && <button type="button" className="btn-close" aria-label="Cerrar" onClick={onClose} />}
    </div>
  );
};

export default ErrorAlert;
