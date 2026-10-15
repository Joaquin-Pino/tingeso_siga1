import httpClient from "../http-common";

const getAll = () => {
  return httpClient.get("/api/students");
};

const get = (id) => {
  return httpClient.get(`/api/students/${id}`);
};

const create = (data) => {
  return httpClient.post("/api/students", data);
};

const update = (id, data) => {
  return httpClient.put(`/api/students/${id}`, data);
};

const remove = (id) => {
  return httpClient.delete(`/api/students/${id}`);
};

export default { getAll, get, create, update, remove };
