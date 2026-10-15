import httpClient from "../http-common";

const getAll = () => {
  return httpClient.get("/api/study-plans");
};

const get = (id) => {
  return httpClient.get(`/api/study-plans/${id}`);
};

const create = (data) => {
  return httpClient.post("/api/study-plans", data);
};

const update = (id, data) => {
  return httpClient.put(`/api/study-plans/${id}`, data);
};

const remove = (id) => {
  return httpClient.delete(`/api/study-plans/${id}`);
};

export default { getAll, get, create, update, remove };
