import { USE_MOCK } from "../config";
import {
  mockCreateStaff,
  mockListStaff,
  mockResetStaffPassword,
  mockUpdateStaff,
  mockUpdateStaffStatus
} from "../mock/db";
import http from "./http";

export async function listStaffApi(params) {
  if (USE_MOCK) return mockListStaff(params);
  return http.get("/api/staff/list", { params });
}

export async function createStaffApi(data) {
  if (USE_MOCK) return mockCreateStaff(data);
  return http.post("/api/staff/create", data);
}

export async function updateStaffApi(id, data) {
  if (USE_MOCK) return mockUpdateStaff(id, data);
  return http.put(`/api/staff/${id}`, data);
}

export async function updateStaffStatusApi(id, data) {
  if (USE_MOCK) return mockUpdateStaffStatus(id, data.status);
  return http.put(`/api/staff/${id}/status`, data);
}

export async function resetStaffPasswordApi(id, data) {
  if (USE_MOCK) return mockResetStaffPassword(id, data.password);
  return http.put(`/api/staff/${id}/password`, data);
}
