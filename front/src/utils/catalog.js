import careerService from "../services/career.service";
import studyPlanService from "../services/studyPlan.service";

// el back no tiene un listado global de asignaturas: se recorren carreras → planes → asignaturas.
// Devuelve { careers, plans, subjects } (plans y subjects planos) y subjectsById.
export const loadCatalog = async () => {
  const careers = (await careerService.getAll()).data;
  const plans = [];
  const subjects = [];
  const subjectsById = {};
  for (const career of careers) {
    const careerPlans = (await careerService.getStudyPlans(career.id)).data;
    for (const plan of careerPlans) {
      plans.push(plan);
      const planSubjects = (await studyPlanService.getSubjects(plan.id)).data;
      for (const subject of planSubjects) {
        subjects.push(subject);
        subjectsById[subject.id] = subject;
      }
    }
  }
  return { careers, plans, subjects, subjectsById };
};

export const toMapById = (items, field) => {
  const map = {};
  for (const item of items) map[item.id] = item[field];
  return map;
};
