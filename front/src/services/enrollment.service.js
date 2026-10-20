import httpClient from "../http-common";

const getAll = (periodId) => {
  return httpClient.get(`/api/academic-periods/${periodId}/enrollments`);
};

const enroll = (periodId, studentId) => {
  return httpClient.post(`/api/academic-periods/${periodId}/enrollments`, { studentId });
};

export default { getAll, enroll };
