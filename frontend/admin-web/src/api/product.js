import http from "./http";
import { USE_MOCK } from "../config";
import { mockCreateProduct, mockListProducts, mockUpdateProduct, mockUpdateProductStatus, mockAllProducts } from "../mock/db";

export async function listProductsApi(params) {
  if (USE_MOCK) return mockListProducts(params);
  return http.get("/api/products", { params });
}

export async function allProductsApi(params) {
  if (USE_MOCK) return mockAllProducts(params);
  return http.get("/api/products/all", { params });
}

export async function createProductApi(data) {
  if (USE_MOCK) return mockCreateProduct(data);
  return http.post("/api/products", data);
}

export async function updateProductApi(id, data) {
  if (USE_MOCK) return mockUpdateProduct(id, data);
  return http.put(`/api/products/${id}`, data);
}

export async function updateProductStatusApi(id, data) {
  if (USE_MOCK) return mockUpdateProductStatus(id, data.status);
  return http.put(`/api/products/${id}/status`, data);
}
