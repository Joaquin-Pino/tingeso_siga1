import httpClient from "../http-common";

const getAll = () => {
  return httpClient.get("/api/academic-periods");
};

const get = (id) => {
  return httpClient.get(`/api/academic-periods/${id}`);
};

const create = (data) => {
  return httpClient.post("/api/academic-periods", data);
};

const update = (id, data) => {
  return httpClient.put(`/api/academic-periods/${id}`, data);
};

const remove = (id) => {
  return httpClient.delete(`/api/academic-periods/${id}`);
};

export default { getAll, get, create, update, remove };
