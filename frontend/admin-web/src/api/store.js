import http from "./http";
import { USE_MOCK } from "../config";
import { mockAllStores, mockCreateStore, mockDeleteStore, mockListStores, mockUpdateStore } from "../mock/db";

export async function listStoresApi(params) {
  if (USE_MOCK) return mockListStores(params);
  return http.get("/api/stores", { params });
}

export async function allStoresApi() {
  if (USE_MOCK) return mockAllStores();
  return http.get("/api/stores/all");
}

export async function createStoreApi(data) {
  if (USE_MOCK) return mockCreateStore(data);
  return http.post("/api/stores", data);
}

export async function updateStoreApi(id, data) {
  if (USE_MOCK) return mockUpdateStore(id, data);
  return http.put(`/api/stores/${id}`, data);
}

export async function deleteStoreApi(id) {
  if (USE_MOCK) return mockDeleteStore(id);
  return http.delete(`/api/stores/${id}`);
}
