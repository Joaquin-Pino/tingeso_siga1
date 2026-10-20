export const ACADEMIC_DEGREES = [
  { value: "BACHELOR", label: "Licenciatura" },
  { value: "MASTER", label: "Magíster" },
  { value: "DOCTORATE", label: "Doctorado" },
];

export const degreeLabel = (value) => {
  for (const d of ACADEMIC_DEGREES) {
    if (d.value === value) return d.label;
  }
  return value;
};

// GRADUATED y ELIMINATED los asigna solo el cierre de período
export const MANUAL_STUDENT_STATUSES = [
  { value: "REGULAR", label: "Regular" },
  { value: "POSTPONED", label: "Postergado" },
  { value: "TEMPORARY_WITHDRAWAL", label: "Retiro temporal" },
];
