import http from "./http";

export async function loginApi(data) {
  return http.post("/api/auth/login", data);
}
