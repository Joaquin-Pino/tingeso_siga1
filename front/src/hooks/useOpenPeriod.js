import { useEffect, useState } from "react";
import academicPeriodService from "../services/academicPeriod.service";
import { getErrorMessage } from "../utils/errors";

// el período abierto más reciente (mayor id); null si no hay ninguno
const useOpenPeriod = () => {
  const [period, setPeriod] = useState(null);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    academicPeriodService
      .getAll()
      .then((response) => {
        let latest = null;
        for (const p of response.data) {
          if (p.status === "OPEN" && (latest === null || p.id > latest.id)) latest = p;
        }
        setPeriod(latest);
        setLoaded(true);
      })
      .catch((e) => setError(getErrorMessage(e)));
  }, []);

  return { period, loaded, error };
};

export default useOpenPeriod;
