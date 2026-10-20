import httpClient from "../http-common";

const get = (id) => {
  return httpClient.get(`/api/subjects/${id}`);
};

const create = (data) => {
  return httpClient.post("/api/subjects", data);
};

const update = (id, data) => {
  return httpClient.put(`/api/subjects/${id}`, data);
};

const remove = (id) => {
  return httpClient.delete(`/api/subjects/${id}`);
};

export default { get, create, update, remove };
