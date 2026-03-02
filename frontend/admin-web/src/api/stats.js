import http from "./http";

export async function userStatsApi(params) {
  return http.get("/api/stats/users", { params });
}

export async function statsSummaryApi(params) {
  return http.get("/api/stats/summary", { params });
}

export async function generateReportApi(params) {
  return http.post("/api/stats/report/generate", null, { params });
}
