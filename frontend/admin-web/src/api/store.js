import http from "./http";

export async function listStoresApi(params) {
  return http.get("/api/stores", { params });
}

export async function allStoresApi() {
  return http.get("/api/stores/all");
}

export async function createStoreApi(data) {
  return http.post("/api/stores", data);
}

export async function updateStoreApi(id, data) {
  return http.put(`/api/stores/${id}`, data);
}

export async function deleteStoreApi(id) {
  return http.delete(`/api/stores/${id}`);
}
