<template>
  <section class="cashier-page">
    <div class="backdrop"></div>
    <div class="cashier-shell" v-loading="loading">
      <div class="hero">
        <p class="eyebrow">DAIFU CASHIER</p>
        <h1>订单收银台</h1>
        <p class="hero-text">确认订单信息后完成支付。订单状态以后端回调结果为准。</p>
      </div>

      <el-card v-if="order" class="cashier-card" shadow="never">
        <div class="card-head">
          <div>
            <div class="label">订单号</div>
            <div class="order-no">{{ order.orderNo }}</div>
          </div>
          <el-tag :type="statusTagType(order.status)" effect="dark">{{ statusText(order.status) }}</el-tag>
        </div>

        <div class="amount-panel">
          <span>支付金额</span>
          <strong>￥{{ order.totalAmount }}</strong>
          <small>{{ remainingText }}</small>
        </div>

        <div class="meta-grid">
          <div class="meta-item">
            <span>支付模式</span>
            <strong>{{ order.mockMode ? "模拟模式" : "微信 JSAPI" }}</strong>
          </div>
          <div class="meta-item">
            <span>预支付 ID</span>
            <strong>{{ order.prepayId || "-" }}</strong>
          </div>
          <div class="meta-item">
            <span>支付时间</span>
            <strong>{{ order.payTime || "-" }}</strong>
          </div>
          <div class="meta-item">
            <span>过期时间</span>
            <strong>{{ order.expireTime || "-" }}</strong>
          </div>
        </div>

        <div class="items-block">
          <div class="section-title">商品明细</div>
          <div v-if="!order.items?.length" class="empty-text">暂无商品明细</div>
          <div v-else class="item-list">
            <div v-for="item in order.items" :key="`${item.productId}-${item.productName}`" class="item-row">
              <div>
                <div class="item-name">{{ item.productName }}</div>
                <div class="item-sub">单价 ￥{{ item.price }} × {{ item.quantity }}</div>
              </div>
              <div class="item-amount">￥{{ item.subTotal }}</div>
            </div>
          </div>
        </div>

        <div class="actions">
          <el-button @click="loadOrder">刷新状态</el-button>
          <el-button type="primary" :loading="paying" :disabled="!canPay" @click="startPay">
            {{ order.mockMode ? "模拟支付" : "立即支付" }}
          </el-button>
        </div>

        <p v-if="order.mockMode" class="hint">
          当前环境未接入真实公众号与商户号，页面会使用模拟支付成功流程验证订单、库存与回调后的状态更新。
        </p>
      </el-card>

      <el-empty v-else-if="!loading" description="订单不存在或已不可访问" class="cashier-card" />
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { createWechatJsapiPayApi, getPayOrderApi, mockPaySuccessApi } from "../api/pay";

const route = useRoute();
const order = ref(null);
const loading = ref(false);
const paying = ref(false);

let timer = null;

const statusText = (status) => {
  if (status === 0) return "待支付";
  if (status === 1) return "支付成功";
  if (status === 2) return "已关闭";
  if (status === 3) return "支付失败";
  return "未知状态";
};

const statusTagType = (status) => {
  if (status === 1) return "success";
  if (status === 2) return "info";
  if (status === 3) return "danger";
  return "warning";
};

const remainingText = computed(() => {
  if (!order.value?.expireTime) return "订单长期有效";
  if (Number(order.value.status) !== 0) return "订单已结束";

  const expireAt = new Date(order.value.expireTime).getTime();
  const diff = Math.max(0, Math.floor((expireAt - Date.now()) / 1000));
  if (diff <= 0) return "订单已过期";

  const minutes = String(Math.floor(diff / 60)).padStart(2, "0");
  const seconds = String(diff % 60).padStart(2, "0");
  return `剩余支付时间 ${minutes}:${seconds}`;
});

const canPay = computed(() => Number(order.value?.status) === 0 && remainingText.value !== "订单已过期");

const loadOrder = async () => {
  loading.value = true;
  try {
    order.value = await getPayOrderApi(route.params.orderNo);
  } finally {
    loading.value = false;
  }
};

const startPay = async () => {
  if (!order.value) return;

  paying.value = true;
  try {
    const jsapi = await createWechatJsapiPayApi({ orderNo: order.value.orderNo });
    order.value.prepayId = jsapi.prepayId || order.value.prepayId;

    if (order.value.mockMode) {
      await ElMessageBox.confirm(
        `当前为模拟支付环境，将直接把订单 ${order.value.orderNo} 标记为支付成功。`,
        "模拟支付确认",
        { type: "warning" }
      );
      await mockPaySuccessApi(order.value.orderNo);
      await loadOrder();
      ElMessage.success("模拟支付成功");
      return;
    }

    const inWechat = /MicroMessenger/i.test(window.navigator.userAgent || "");
    if (!inWechat) {
      await ElMessageBox.alert("当前不是微信环境，请在微信内打开本页面后继续支付。", "提示", {
        type: "warning"
      });
      return;
    }

    const payOk = await invokeWechatPay(jsapi);
    if (!payOk) return;

    const paid = await waitPaySuccess(order.value.orderNo);
    if (paid) {
      ElMessage.success("支付成功");
      await loadOrder();
      return;
    }

    ElMessage.warning("支付结果确认超时，请刷新页面查看最终状态。");
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error(error?.message || "支付发起失败");
    }
  } finally {
    paying.value = false;
  }
};

const invokeWechatPay = (jsapi) => {
  return new Promise((resolve, reject) => {
    const invoke = () => {
      window.WeixinJSBridge.invoke(
        "getBrandWCPayRequest",
        {
          appId: jsapi.appId,
          timeStamp: jsapi.timeStamp,
          nonceStr: jsapi.nonceStr,
          package: jsapi.packageValue,
          signType: jsapi.signType,
          paySign: jsapi.paySign
        },
        (res) => {
          const msg = (res?.err_msg || "").toLowerCase();
          if (msg.includes("ok")) {
            resolve(true);
            return;
          }
          if (msg.includes("cancel")) {
            ElMessage.info("已取消支付");
            resolve(false);
            return;
          }
          reject(new Error(res?.err_msg || "wechat pay failed"));
        }
      );
    };

    if (typeof window.WeixinJSBridge === "undefined") {
      document.addEventListener("WeixinJSBridgeReady", invoke, { once: true });
    } else {
      invoke();
    }
  });
};

const waitPaySuccess = async (orderNo) => {
  for (let i = 0; i < 10; i += 1) {
    const latest = await getPayOrderApi(orderNo);
    order.value = latest;
    if (Number(latest.status) === 1) {
      return true;
    }
    await new Promise((resolve) => setTimeout(resolve, 1500));
  }
  return false;
};

onMounted(async () => {
  await loadOrder();
  timer = window.setInterval(() => {
    order.value = order.value ? { ...order.value } : order.value;
  }, 1000);
});

onBeforeUnmount(() => {
  if (timer) {
    window.clearInterval(timer);
  }
});
</script>

<style scoped>
.cashier-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(circle at top left, rgba(255, 214, 153, 0.45), transparent 32%),
    radial-gradient(circle at right center, rgba(113, 201, 206, 0.3), transparent 28%),
    linear-gradient(180deg, #f7f1e7 0%, #f3f6fb 100%);
  padding: 32px 16px 48px;
}

.backdrop {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(15, 23, 42, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(15, 23, 42, 0.04) 1px, transparent 1px);
  background-size: 28px 28px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.9), transparent);
}

.cashier-shell {
  position: relative;
  z-index: 1;
  width: min(920px, 100%);
  margin: 0 auto;
}

.hero {
  padding: 12px 4px 24px;
}

.eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.24em;
  color: #9a3412;
}

.hero h1 {
  margin: 0;
  font-size: clamp(34px, 6vw, 52px);
  line-height: 1;
  color: #172033;
  font-family: Georgia, "Times New Roman", serif;
}

.hero-text {
  width: min(560px, 100%);
  margin: 14px 0 0;
  color: #475569;
  line-height: 1.7;
}

.cashier-card {
  border: 0;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(18px);
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.12);
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.label {
  font-size: 13px;
  color: #64748b;
}

.order-no {
  margin-top: 8px;
  font-size: 22px;
  font-weight: 700;
  color: #172033;
  word-break: break-all;
}

.amount-panel {
  margin-top: 22px;
  padding: 24px;
  border-radius: 22px;
  background: linear-gradient(135deg, #172033 0%, #283b68 100%);
  color: #fff8ef;
  display: grid;
  gap: 8px;
}

.amount-panel span,
.amount-panel small {
  opacity: 0.78;
}

.amount-panel strong {
  font-size: clamp(34px, 6vw, 48px);
  line-height: 1;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 20px;
}

.meta-item {
  padding: 16px;
  border-radius: 18px;
  background: #fffaf1;
  border: 1px solid rgba(154, 52, 18, 0.08);
}

.meta-item span {
  display: block;
  font-size: 12px;
  color: #78716c;
}

.meta-item strong {
  display: block;
  margin-top: 8px;
  color: #1f2937;
  word-break: break-word;
}

.items-block {
  margin-top: 24px;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: #172033;
}

.item-list {
  margin-top: 14px;
  display: grid;
  gap: 12px;
}

.item-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  border-radius: 18px;
  background: #f8fafc;
}

.item-name {
  font-weight: 600;
  color: #172033;
}

.item-sub,
.empty-text,
.hint {
  color: #64748b;
}

.item-sub {
  margin-top: 6px;
  font-size: 13px;
}

.item-amount {
  font-weight: 700;
  color: #0f172a;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 28px;
}

.hint {
  margin: 16px 0 4px;
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 760px) {
  .cashier-page {
    padding-top: 20px;
  }

  .cashier-card {
    border-radius: 22px;
  }

  .card-head,
  .actions,
  .item-row {
    flex-direction: column;
    align-items: stretch;
  }

  .meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
