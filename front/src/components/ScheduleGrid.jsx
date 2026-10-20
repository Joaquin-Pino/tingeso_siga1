import { MODULES, WEEK_DAYS } from "../utils/schedule";

// blocks: [{ day: "L", module: 1, label? }]. Con onCellClick(day, module) las celdas son seleccionables (editor de bloques).
const ScheduleGrid = ({ blocks = [], onCellClick }) => {
  const findBlock = (day, module) => {
    for (const b of blocks) {
      if (b.day === day && b.module === module) return b;
    }
    return null;
  };

  return (
    <div className="table-responsive">
      <table className="table table-bordered text-center align-middle">
        <thead>
          <tr>
            <th>Módulo</th>
            {WEEK_DAYS.map((d) => (
              <th key={d.code}>{d.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {MODULES.map((m) => (
            <tr key={m.number}>
              <th className="text-nowrap">
                {m.number} <small className="text-muted d-block">{m.time}</small>
              </th>
              {WEEK_DAYS.map((d) => {
                const block = findBlock(d.code, m.number);
                return (
                  <td
                    key={d.code}
                    className={block ? "table-primary" : ""}
                    style={onCellClick ? { cursor: "pointer" } : undefined}
                    onClick={onCellClick ? () => onCellClick(d.code, m.number) : undefined}
                  >
                    {block ? block.label || "●" : ""}
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default ScheduleGrid;
