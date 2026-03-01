import http from "./http";
import { USE_MOCK } from "../config";
import { mockLogin } from "../mock/db";

export async function loginApi(data) {
  if (USE_MOCK) return mockLogin(data);
  return http.post("/api/auth/login", data);
}
