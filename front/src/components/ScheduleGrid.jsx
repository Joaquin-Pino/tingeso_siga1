import { MODULES, WEEK_DAYS } from "../utils/schedule";

// blocks: [{ day: "L", module: 1 }]; cada bloque se pinta con su etiqueta opcional (ej. nombre de la asignatura)
const ScheduleGrid = ({ blocks = [] }) => {
  const findBlock = (day, module) =>
    blocks.find((b) => b.day === day && b.module === module);

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
                  <td key={d.code} className={block ? "table-primary" : ""}>
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
