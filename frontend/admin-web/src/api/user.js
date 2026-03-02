import http from "./http";

export async function listUsersApi(params) {
  return http.get("/api/users", { params });
}

export async function createUserApi(data) {
  return http.post("/api/users", data);
}

export async function updateUserApi(id, data) {
  return http.put(`/api/users/${id}`, data);
}

export async function deleteUserApi(id) {
  return http.delete(`/api/users/${id}`);
}
