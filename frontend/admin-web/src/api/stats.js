import http from "./http";
import { USE_MOCK } from "../config";
import { mockGenerateStatsReport, mockStatsSummary, mockUserStats } from "../mock/db";

export async function userStatsApi(params) {
  if (USE_MOCK) return mockUserStats(params);
  return http.get("/api/stats/users", { params });
}

export async function statsSummaryApi(params) {
  if (USE_MOCK) return mockStatsSummary(params);
  return http.get("/api/stats/summary", { params });
}

export async function generateReportApi(params) {
  if (USE_MOCK) return mockGenerateStatsReport(params);
  return http.post("/api/stats/report/generate", null, { params });
}
