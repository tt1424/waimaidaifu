import http from "./http";
import { USE_MOCK } from "../config";
import { mockAddCartItem, mockDeleteCartItem, mockListCartByUser, mockUpdateCartItem } from "../mock/db";

export async function listCartByUserApi(params) {
  if (USE_MOCK) return mockListCartByUser(params.userId);
  return http.get("/api/carts", { params });
}

export async function addCartItemApi(data) {
  if (USE_MOCK) return mockAddCartItem(data);
  return http.post("/api/carts/items", data);
}

export async function updateCartItemApi(id, data) {
  if (USE_MOCK) return mockUpdateCartItem(id, data);
  return http.put(`/api/carts/items/${id}`, data);
}

export async function deleteCartItemApi(id) {
  if (USE_MOCK) return mockDeleteCartItem(id);
  return http.delete(`/api/carts/items/${id}`);
}
