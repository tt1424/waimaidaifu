import { USE_MOCK } from "../config";
import {
  mockCreateUser,
  mockDeleteUser,
  mockListUsers,
  mockUpdateUser
} from "../mock/db";
import http from "./http";

export async function listUsersApi(params) {
  if (USE_MOCK) return mockListUsers(params);
  return http.get("/api/users", { params });
}

export async function createUserApi(data) {
  if (USE_MOCK) return mockCreateUser(data);
  return http.post("/api/users", data);
}

export async function updateUserApi(id, data) {
  if (USE_MOCK) return mockUpdateUser(id, data);
  return http.put(`/api/users/${id}`, data);
}

export async function deleteUserApi(id) {
  if (USE_MOCK) return mockDeleteUser(id);
  return http.delete(`/api/users/${id}`);
}
