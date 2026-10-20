const STATUS_STYLES = {
  REGULAR: { label: "Regular", color: "success" },
  POSTPONED: { label: "Postergado", color: "warning" },
  TEMPORARY_WITHDRAWAL: { label: "Retiro temporal", color: "warning" },
  GRADUATED: { label: "Egresado", color: "primary" },
  ELIMINATED: { label: "Eliminado", color: "danger" },
  ACTIVE: { label: "Activo", color: "success" },
  INACTIVE: { label: "Inactivo", color: "secondary" },
  OPEN: { label: "Abierto", color: "success" },
  CLOSED: { label: "Cerrado", color: "secondary" },
  CURRENT: { label: "Vigente", color: "success" },
  NOT_CURRENT: { label: "No vigente", color: "secondary" },
  PASSED: { label: "Aprobado", color: "success" },
  FAILED: { label: "Reprobado", color: "danger" },
};

const StatusBadge = ({ status }) => {
  const style = STATUS_STYLES[status] || { label: status, color: "secondary" };
  return <span className={`badge text-bg-${style.color}`}>{style.label}</span>;
};

export default StatusBadge;
