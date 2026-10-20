import httpClient from "../http-common";

const getAll = () => {
  return httpClient.get("/api/students");
};

const getMe = () => {
  return httpClient.get("/api/students/me");
};

const get = (id) => {
  return httpClient.get(`/api/students/${id}`);
};

const getAcademicHistory = (id) => {
  return httpClient.get(`/api/students/${id}/academic-history`);
};

const getAcademicAverage = (id, periodId) => {
  return httpClient.get(`/api/students/${id}/academic-average`, {
    params: periodId ? { periodId } : {},
  });
};

const create = (data) => {
  return httpClient.post("/api/students", data);
};

const update = (id, data) => {
  return httpClient.put(`/api/students/${id}`, data);
};

const updateStatus = (id, status) => {
  return httpClient.patch(`/api/students/${id}/status`, { status });
};

const updateCareer = (id, data) => {
  return httpClient.patch(`/api/students/${id}/career`, data);
};

const updateStudyPlan = (id, data) => {
  return httpClient.patch(`/api/students/${id}/study-plan`, data);
};

const remove = (id) => {
  return httpClient.delete(`/api/students/${id}`);
};

export default {
  getAll,
  getMe,
  get,
  getAcademicHistory,
  getAcademicAverage,
  create,
  update,
  updateStatus,
  updateCareer,
  updateStudyPlan,
  remove,
};
