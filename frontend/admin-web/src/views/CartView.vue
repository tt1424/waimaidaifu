<template>
  <section class="page-wrap">
    <div class="page-top">
      <div>
        <h2 class="page-title">购物车与结算</h2>
        <p class="page-desc">在后台维护用户购物车，生成订单后交给公开收银台完成支付。</p>
      </div>
      <el-button type="primary" :loading="creatingOrder" @click="createOrderFromSelection">创建订单</el-button>
    </div>

    <el-card class="block-card" shadow="hover">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="用户">
          <el-select v-model="selectedUserId" placeholder="请选择用户" style="width: 220px" filterable>
            <el-option v-for="u in userOptions" :key="u.id" :label="`${u.username} (${u.phone || '-'})`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品">
          <el-select v-model="addForm.productId" placeholder="请选择商品" style="width: 240px" filterable>
            <el-option
              v-for="p in productOptions"
              :key="p.id"
              :label="`${p.name}（库存 ${p.stock}）`"
              :value="p.id"
            />
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
          <span>最近创建的订单</span>
          <el-space v-if="latestOrderNo">
            <el-button @click="refreshOrderStatus">刷新状态</el-button>
            <el-button type="primary" plain @click="openCashier">打开收银台</el-button>
            <el-button plain @click="copyCashierLink">复制链接</el-button>
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
        <el-descriptions-item label="收银台链接">
          <span class="cashier-path">{{ latestCashierPath ? buildCashierUrl(latestCashierPath) : "-" }}</span>
        </el-descriptions-item>
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
                <el-popconfirm title="确认删除这条购物车商品吗？" @confirm="removeItem(row)">
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
import { ElMessage } from "element-plus";
import { addCartItemApi, deleteCartItemApi, listCartByUserApi, updateCartItemApi } from "../api/cart";
import { createCheckoutApi, getPayOrderApi } from "../api/pay";
import { allProductsApi } from "../api/product";
import { listUsersApi } from "../api/user";

const userOptions = ref([]);
const productOptions = ref([]);
const selectedUserId = ref(null);
const addForm = reactive({ productId: null, quantity: 1 });
const selectedRows = ref([]);

const items = ref([]);
const loading = ref(false);
const creatingOrder = ref(false);
const summary = reactive({ itemCount: 0, totalQuantity: 0, totalAmount: 0 });

const latestOrderNo = ref("");
const latestOrderAmount = ref("");
const latestOrderStatus = ref(null);
const latestCashierPath = ref("");

const selectedAmount = computed(() =>
  (selectedRows.value || [])
    .reduce((sum, row) => sum + Number(row.totalAmount || 0), 0)
    .toFixed(2)
);

const buildCashierUrl = (path) => (path ? `${window.location.origin}${path}` : "");

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
  latestCashierPath.value = order.cashierPath || latestCashierPath.value;
  if (latestOrderStatus.value === 1) {
    await loadCart();
  }
};

const createOrderFromSelection = async () => {
  if (!selectedUserId.value) {
    ElMessage.warning("请先选择用户");
    return;
  }
  if (!selectedRows.value.length) {
    ElMessage.warning("请先勾选要结算的购物车商品");
    return;
  }

  creatingOrder.value = true;
  try {
    const checkout = await createCheckoutApi({
      userId: selectedUserId.value,
      cartItemIds: selectedRows.value.map((row) => row.id)
    });
    latestOrderNo.value = checkout.orderNo;
    latestOrderAmount.value = checkout.totalAmount;
    latestOrderStatus.value = Number(checkout.status);
    latestCashierPath.value = checkout.cashierPath || "";
    ElMessage.success("订单已创建，请在收银台完成支付");
    openCashier();
  } finally {
    creatingOrder.value = false;
  }
};

const openCashier = () => {
  if (!latestCashierPath.value) {
    ElMessage.warning("当前没有可用的收银台链接");
    return;
  }
  window.open(buildCashierUrl(latestCashierPath.value), "_blank", "noopener,noreferrer");
};

const copyCashierLink = async () => {
  if (!latestCashierPath.value) {
    ElMessage.warning("当前没有可用的收银台链接");
    return;
  }

  const url = buildCashierUrl(latestCashierPath.value);
  try {
    await navigator.clipboard.writeText(url);
    ElMessage.success("收银台链接已复制");
  } catch {
    ElMessage.warning(`请手动复制：${url}`);
  }
};

watch(selectedUserId, () => {
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

.order-empty,
.cashier-path {
  color: #9ca3af;
  font-size: 13px;
}

.cashier-path {
  display: inline-block;
  max-width: 320px;
  word-break: break-all;
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
  .page-top,
  .order-head {
    flex-direction: column;
    align-items: stretch;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
