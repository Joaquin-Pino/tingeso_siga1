import httpClient from "../http-common";

const getAll = () => {
  return httpClient.get("/api/academic-periods");
};

const get = (id) => {
  return httpClient.get(`/api/academic-periods/${id}`);
};

const getSections = (id) => {
  return httpClient.get(`/api/academic-periods/${id}/sections`);
};

const create = (data) => {
  return httpClient.post("/api/academic-periods", data);
};

const close = (id) => {
  return httpClient.post(`/api/academic-periods/${id}/close`);
};

const remove = (id) => {
  return httpClient.delete(`/api/academic-periods/${id}`);
};

export default { getAll, get, getSections, create, close, remove };
