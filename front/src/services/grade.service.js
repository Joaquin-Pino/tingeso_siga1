import httpClient from "../http-common";

const getRegistrations = (teacherId, sectionId) => {
  return httpClient.get(`/api/teachers/${teacherId}/sections/${sectionId}/registrations`);
};

const registerGrade = (teacherId, sectionId, registrationId, grade) => {
  return httpClient.put(
    `/api/teachers/${teacherId}/sections/${sectionId}/registrations/${registrationId}/grade`,
    { grade },
  );
};

export default { getRegistrations, registerGrade };
