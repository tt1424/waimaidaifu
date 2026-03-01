import http from "./http";
import { USE_MOCK } from "../config";
import { mockOverview } from "../mock/db";

export async function getOverviewApi() {
  if (USE_MOCK) return mockOverview();
  return http.get("/api/report/overview");
}
