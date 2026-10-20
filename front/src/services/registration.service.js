import httpClient from "../http-common";

const getAll = (studentId, periodId) => {
  return httpClient.get(`/api/students/${studentId}/registrations`, {
    params: periodId ? { periodId } : {},
  });
};

const register = (studentId, sectionIds) => {
  return httpClient.post(`/api/students/${studentId}/registrations`, { sectionIds });
};

const cancel = (studentId, registrationId) => {
  return httpClient.delete(`/api/students/${studentId}/registrations/${registrationId}`);
};

export default { getAll, register, cancel };
