import httpClient from "../http-common";

const getAll = () => {
  return httpClient.get("/api/careers");
};

const get = (id) => {
  return httpClient.get(`/api/careers/${id}`);
};

const getStudyPlans = (id) => {
  return httpClient.get(`/api/careers/${id}/study-plans`);
};

const create = (data) => {
  return httpClient.post("/api/careers", data);
};

const update = (id, data) => {
  return httpClient.put(`/api/careers/${id}`, data);
};

const updateStatus = (id, status) => {
  return httpClient.patch(`/api/careers/${id}/status`, { status });
};

const remove = (id) => {
  return httpClient.delete(`/api/careers/${id}`);
};

export default { getAll, get, getStudyPlans, create, update, updateStatus, remove };
