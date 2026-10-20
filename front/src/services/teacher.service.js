import httpClient from "../http-common";

const getAll = () => {
  return httpClient.get("/api/teachers");
};

const getMe = () => {
  return httpClient.get("/api/teachers/me");
};

const get = (id) => {
  return httpClient.get(`/api/teachers/${id}`);
};

const getSections = (id) => {
  return httpClient.get(`/api/teachers/${id}/sections`);
};

const create = (data) => {
  return httpClient.post("/api/teachers", data);
};

const update = (id, data) => {
  return httpClient.put(`/api/teachers/${id}`, data);
};

const updateStatus = (id, status) => {
  return httpClient.patch(`/api/teachers/${id}/status`, { status });
};

const remove = (id) => {
  return httpClient.delete(`/api/teachers/${id}`);
};

export default { getAll, getMe, get, getSections, create, update, updateStatus, remove };
