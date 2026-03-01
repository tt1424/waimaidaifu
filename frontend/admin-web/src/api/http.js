import axios from "axios";
import { ElMessage } from "element-plus";

const http = axios.create({
  baseURL: "/",
  timeout: 10000
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res && res.code === 0) {
      return res.data;
    }
    ElMessage.error(res?.message || "request failed");
    return Promise.reject(new Error(res?.message || "request failed"));
  },
  (error) => {
    ElMessage.error(error.response?.data?.message || error.message || "network error");
    return Promise.reject(error);
  }
);

export default http;
