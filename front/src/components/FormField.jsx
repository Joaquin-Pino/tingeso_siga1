const FormField = ({ label, name, value, onChange, type = "text", children, ...rest }) => {
  return (
    <div className="mb-3">
      <label htmlFor={name} className="form-label">
        {label}
      </label>
      {children ? (
        <select id={name} name={name} className="form-select" value={value} onChange={onChange} {...rest}>
          {children}
        </select>
      ) : (
        <input id={name} name={name} type={type} className="form-control" value={value} onChange={onChange} {...rest} />
      )}
    </div>
  );
};

export default FormField;
