import { useEffect, useState } from "react";
import academicPeriodService from "../services/academicPeriod.service";
import { loadCatalog } from "../utils/catalog";
import { getErrorMessage } from "../utils/errors";

// secciones del período dado cuyas asignaturas pertenecen al plan del estudiante, con su asignatura adjunta
const usePlanOffering = (periodId, studyPlanId) => {
  const [offering, setOffering] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!periodId || !studyPlanId) return;
    Promise.all([academicPeriodService.getSections(periodId), loadCatalog()])
      .then(([sectionsResponse, catalog]) => {
        const result = [];
        for (const section of sectionsResponse.data) {
          const subject = catalog.subjectsById[section.subjectId];
          if (subject && subject.studyPlanId === studyPlanId) result.push({ ...section, subject });
        }
        result.sort((a, b) => a.subject.semester - b.subject.semester);
        setOffering(result);
      })
      .catch((e) => setError(getErrorMessage(e)));
  }, [periodId, studyPlanId]);

  return { offering, error };
};

export default usePlanOffering;
