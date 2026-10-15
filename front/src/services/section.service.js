import httpClient from "../http-common";

const getAll = () => {
  return httpClient.get("/api/sections");
};

const get = (id) => {
  return httpClient.get(`/api/sections/${id}`);
};

const create = (data) => {
  return httpClient.post("/api/sections", data);
};

const update = (id, data) => {
  return httpClient.put(`/api/sections/${id}`, data);
};

const remove = (id) => {
  return httpClient.delete(`/api/sections/${id}`);
};

export default { getAll, get, create, update, remove };
