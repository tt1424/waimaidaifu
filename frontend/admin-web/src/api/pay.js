import http from "./http";

export async function createCheckoutApi(data) {
  return http.post("/api/pay/checkout", data);
}

export async function createWechatJsapiPayApi(data) {
  return http.post("/api/pay/wechat/jsapi", data);
}

export async function getPayOrderApi(orderNo) {
  return http.get(`/api/pay/orders/${orderNo}`);
}

export async function mockPaySuccessApi(orderNo) {
  return http.post(`/api/pay/orders/${orderNo}/mock/success`);
}
