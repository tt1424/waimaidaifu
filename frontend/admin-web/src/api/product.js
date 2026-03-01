import http from "./http";
import { USE_MOCK } from "../config";
import { mockCreateProduct, mockListProducts, mockUpdateProduct, mockUpdateProductStatus } from "../mock/db";

export async function listProductsApi(params) {
  if (USE_MOCK) return mockListProducts(params);
  return http.get("/api/product/list", { params });
}

export async function createProductApi(data) {
  if (USE_MOCK) return mockCreateProduct(data);
  return http.post("/api/product/create", data);
}

export async function updateProductApi(id, data) {
  if (USE_MOCK) return mockUpdateProduct(id, data);
  return http.put(`/api/product/${id}`, data);
}

export async function updateProductStatusApi(id, data) {
  if (USE_MOCK) return mockUpdateProductStatus(id, data.status);
  return http.put(`/api/product/${id}/status`, data);
}
