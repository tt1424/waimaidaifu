import http from "./http";
import { USE_MOCK } from "../config";
import { mockCreateOrder, mockListOrders, mockPayNotify } from "../mock/db";

export async function listOrdersApi(params) {
  if (USE_MOCK) return mockListOrders(params);
  return http.get("/api/order/list", { params });
}

export async function createOrderApi(data) {
  if (USE_MOCK) return mockCreateOrder(data);
  return http.post("/api/order/create", data);
}

export async function payNotifyApi(data) {
  if (USE_MOCK) return mockPayNotify(data);
  return http.post("/api/order/payNotify", data);
}
