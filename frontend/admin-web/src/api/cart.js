import http from "./http";

export async function listCartByUserApi(params) {
  return http.get("/api/carts", { params });
}

export async function addCartItemApi(data) {
  return http.post("/api/carts/items", data);
}

export async function updateCartItemApi(id, data) {
  return http.put(`/api/carts/items/${id}`, data);
}

export async function deleteCartItemApi(id) {
  return http.delete(`/api/carts/items/${id}`);
}
