// el GlobalExceptionHandler del back responde { error: "mensaje" }
export const getErrorMessage = (error) => {
  if (error.response?.data?.error) return error.response.data.error;
  if (error.response) return `Error ${error.response.status}`;
  return "No se pudo conectar con el servidor";
};
