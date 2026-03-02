import http from "./http";

export async function listProductsApi(params) {
  return http.get("/api/products", { params });
}

export async function allProductsApi(params) {
  return http.get("/api/products/all", { params });
}

export async function createProductApi(data) {
  return http.post("/api/products", data);
}

export async function updateProductApi(id, data) {
  return http.put(`/api/products/${id}`, data);
}

export async function updateProductStatusApi(id, data) {
  return http.put(`/api/products/${id}/status`, data);
}
