import httpClient from "../http-common";

const getSubjects = (id) => {
  return httpClient.get(`/api/study-plans/${id}/subjects`);
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

export default { getSubjects, create, update, remove };
