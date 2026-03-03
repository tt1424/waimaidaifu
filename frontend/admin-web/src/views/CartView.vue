<template>
  <section class="page-wrap">
    <div class="page-top">
      <div>
        <h2 class="page-title">购物车管理</h2>
        <p class="page-desc">支持按用户维护购物车，并可选中一条或多条发起微信支付。</p>
      </div>
      <el-button type="success" :loading="paying" @click="paySelected">微信支付</el-button>
    </div>

    <el-card class="block-card" shadow="hover">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="用户">
          <el-select v-model="selectedUserId" placeholder="请选择用户" style="width: 220px" filterable>
            <el-option v-for="u in userOptions" :key="u.id" :label="`${u.username} (${u.phone || '-'})`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="OpenID">
          <el-input v-model="payForm.openid" placeholder="微信用户 OpenID" style="width: 280px" />
        </el-form-item>
        <el-form-item label="商品">
          <el-select v-model="addForm.productId" placeholder="请选择商品" style="width: 220px" filterable>
            <el-option v-for="p in productOptions" :key="p.id" :label="`${p.name}（库存:${p.stock}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="addForm.quantity" :min="1" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="addItem">加入购物车</el-button>
          <el-button @click="loadCart">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="block-card" shadow="hover">
      <template #header>
        <div class="order-head">
          <span>最近支付订单</span>
          <el-space v-if="latestOrderNo">
            <el-input :model-value="latestOrderNo" readonly style="width: 280px" />
            <el-button @click="refreshOrderStatus">查单</el-button>
          </el-space>
          <span v-else class="order-empty">暂无</span>
        </div>
      </template>
      <el-descriptions :column="4" border size="small">
        <el-descriptions-item label="订单号">{{ latestOrderNo || "-" }}</el-descriptions-item>
        <el-descriptions-item label="金额">￥{{ latestOrderAmount || "-" }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(latestOrderStatus)">{{ statusText(latestOrderStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="预支付ID">{{ latestPrepayId || "-" }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="block-card" shadow="hover">
      <div v-loading="loading">
        <el-empty v-if="!items.length && !loading" description="暂无购物车数据" />
        <template v-else>
          <el-table :data="items" border row-class-name="table-row" @selection-change="onSelectionChange">
            <el-table-column type="selection" width="48" />
            <el-table-column prop="id" label="ID" width="80" align="center" />
            <el-table-column prop="productName" label="商品名称" min-width="180" />
            <el-table-column prop="unitPrice" label="单价" width="120" align="right">
              <template #default="{ row }">￥{{ row.unitPrice }}</template>
            </el-table-column>
            <el-table-column label="数量" width="170" align="center">
              <template #default="{ row }">
                <el-input-number :model-value="row.quantity" :min="1" @change="(v) => updateQty(row, v)" />
              </template>
            </el-table-column>
            <el-table-column prop="totalAmount" label="金额" width="130" align="right">
              <template #default="{ row }">￥{{ row.totalAmount }}</template>
            </el-table-column>
            <el-table-column prop="updateTime" label="更新时间" min-width="170" />
            <el-table-column label="操作" width="120" align="center" fixed="right">
              <template #default="{ row }">
                <el-popconfirm title="确认删除该购物车商品吗？" @confirm="removeItem(row)">
                  <template #reference>
                    <el-button link type="danger">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </div>
    </el-card>

    <el-card class="block-card summary-card" shadow="hover">
      <div class="summary-grid">
        <div class="summary-item">
          <span class="summary-label">商品项数</span>
          <span class="summary-value">{{ summary.itemCount || 0 }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">总数量</span>
          <span class="summary-value">{{ summary.totalQuantity || 0 }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">总金额</span>
          <span class="summary-value">￥{{ summary.totalAmount || 0 }}</span>
        </div>
        <div class="summary-item summary-selected">
          <span class="summary-label">已选金额</span>
          <span class="summary-value">￥{{ selectedAmount }}</span>
        </div>
      </div>
    </el-card>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { addCartItemApi, deleteCartItemApi, listCartByUserApi, updateCartItemApi } from "../api/cart";
import { allProductsApi } from "../api/product";
import { createCheckoutApi, createWechatJsapiPayApi, getPayOrderApi } from "../api/pay";
import { listUsersApi } from "../api/user";

const OPENID_KEY = "wechat_openid_cache_by_user_v1";

const userOptions = ref([]);
const productOptions = ref([]);
const selectedUserId = ref(null);
const addForm = reactive({ productId: null, quantity: 1 });
const payForm = reactive({ openid: "" });
const selectedRows = ref([]);

const items = ref([]);
const loading = ref(false);
const paying = ref(false);
const summary = reactive({ itemCount: 0, totalQuantity: 0, totalAmount: 0 });

const latestOrderNo = ref("");
const latestOrderAmount = ref("");
const latestOrderStatus = ref(null);
const latestPrepayId = ref("");

const selectedAmount = computed(() =>
  (selectedRows.value || [])
    .reduce((sum, row) => sum + Number(row.totalAmount || 0), 0)
    .toFixed(2)
);

const getOpenidCache = () => {
  try {
    return JSON.parse(localStorage.getItem(OPENID_KEY) || "{}");
  } catch {
    return {};
  }
};

const setOpenidCache = (userId, openid) => {
  const cache = getOpenidCache();
  cache[String(userId)] = openid;
  localStorage.setItem(OPENID_KEY, JSON.stringify(cache));
};

const loadUsers = async () => {
  const res = await listUsersApi({ pageNum: 1, pageSize: 999, role: 2, status: 1 });
  userOptions.value = res.records || [];
  if (!selectedUserId.value && userOptions.value.length) {
    selectedUserId.value = userOptions.value[0].id;
  }
};

const loadProducts = async () => {
  productOptions.value = await allProductsApi();
  if (!addForm.productId && productOptions.value.length) {
    addForm.productId = productOptions.value[0].id;
  }
};

const loadCart = async () => {
  if (!selectedUserId.value) {
    items.value = [];
    Object.assign(summary, { itemCount: 0, totalQuantity: 0, totalAmount: 0 });
    return;
  }
  loading.value = true;
  try {
    const res = await listCartByUserApi({ userId: selectedUserId.value });
    items.value = res.items || [];
    selectedRows.value = [];
    Object.assign(summary, res.summary || { itemCount: 0, totalQuantity: 0, totalAmount: 0 });
  } finally {
    loading.value = false;
  }
};

const addItem = async () => {
  if (!selectedUserId.value || !addForm.productId) {
    ElMessage.warning("请选择用户和商品");
    return;
  }
  await addCartItemApi({
    userId: selectedUserId.value,
    productId: addForm.productId,
    quantity: addForm.quantity
  });
  await loadCart();
  ElMessage.success("已加入购物车");
};

const updateQty = async (row, value) => {
  const quantity = Number(value || 1);
  await updateCartItemApi(row.id, { quantity });
  await loadCart();
};

const removeItem = async (row) => {
  await deleteCartItemApi(row.id);
  await loadCart();
  ElMessage.success("删除成功");
};

const onSelectionChange = (rows) => {
  selectedRows.value = rows || [];
};

const statusText = (status) => {
  if (status === 0) return "待支付";
  if (status === 1) return "支付成功";
  if (status === 2) return "已关闭";
  if (status === 3) return "支付失败";
  return "未知";
};

const statusTagType = (status) => {
  if (status === 1) return "success";
  if (status === 2) return "info";
  if (status === 3) return "danger";
  return "warning";
};

const refreshOrderStatus = async () => {
  if (!latestOrderNo.value) return;
  const order = await getPayOrderApi(latestOrderNo.value);
  latestOrderStatus.value = Number(order.status);
  latestPrepayId.value = order.prepayId || latestPrepayId.value;
  if (latestOrderStatus.value === 1) {
    await loadCart();
  }
};

const paySelected = async () => {
  if (!selectedUserId.value) {
    ElMessage.warning("请先选择用户");
    return;
  }
  if (!selectedRows.value.length) {
    ElMessage.warning("请先勾选要支付的购物车数据");
    return;
  }
  if (!payForm.openid.trim()) {
    ElMessage.warning("请输入微信 OpenID");
    return;
  }

  await ElMessageBox.confirm(
    `确认发起支付？\n选中条数：${selectedRows.value.length}\n支付金额：￥${selectedAmount.value}`,
    "确认支付",
    { type: "warning" }
  );

  paying.value = true;
  try {
    const checkout = await createCheckoutApi({
      userId: selectedUserId.value,
      cartItemIds: selectedRows.value.map((x) => x.id),
      openid: payForm.openid.trim()
    });

    latestOrderNo.value = checkout.orderNo;
    latestOrderAmount.value = checkout.totalAmount;
    latestOrderStatus.value = Number(checkout.status);
    latestPrepayId.value = "";
    setOpenidCache(selectedUserId.value, payForm.openid.trim());

    const jsapi = await createWechatJsapiPayApi({ orderNo: checkout.orderNo });
    latestPrepayId.value = jsapi.prepayId || "";

    const inWechat = /MicroMessenger/i.test(window.navigator.userAgent || "");
    if (!inWechat) {
      await ElMessageBox.alert(
        `当前不是微信环境，请在微信内打开后支付。\n订单号：${checkout.orderNo}`,
        "提示",
        { type: "warning" }
      );
      return;
    }

    const payOk = await invokeWechatPay(jsapi);
    if (!payOk) return;

    const paid = await waitPaySuccess(checkout.orderNo);
    if (paid) {
      ElMessage.success("支付成功");
      await refreshOrderStatus();
    } else {
      ElMessage.warning("支付结果确认超时，请稍后点击“查单”");
    }
  } catch (error) {
    ElMessage.error(error?.message || "支付发起失败");
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
  const maxRetry = 10;
  for (let i = 0; i < maxRetry; i += 1) {
    const order = await getPayOrderApi(orderNo);
    if (Number(order.status) === 1) {
      latestOrderStatus.value = 1;
      return true;
    }
    await new Promise((resolve) => setTimeout(resolve, 1500));
  }
  return false;
};

watch(selectedUserId, (userId) => {
  const cache = getOpenidCache();
  payForm.openid = cache[String(userId)] || "";
  loadCart();
});

onMounted(async () => {
  await loadUsers();
  await loadProducts();
  await loadCart();
});
</script>

<style scoped>
.page-wrap {
  display: grid;
  gap: 16px;
}

.page-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  line-height: 30px;
}

.page-desc {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 14px;
}

.block-card {
  border-radius: 14px;
}

.filter-form {
  margin-bottom: -18px;
}

.order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.order-empty {
  color: #9ca3af;
  font-size: 13px;
}

:deep(.table-row) {
  height: 50px;
}

.summary-card {
  background: linear-gradient(180deg, #ffffff 0%, #fafcff 100%);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  padding: 12px;
  border-radius: 12px;
  background: #f5f8ff;
}

.summary-selected {
  background: #ecfdf3;
}

.summary-label {
  display: block;
  font-size: 13px;
  color: #6b7280;
}

.summary-value {
  display: block;
  margin-top: 6px;
  font-size: 20px;
  font-weight: 600;
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
