<template>
  <section class="page-wrap">
    <div class="page-top">
      <div>
        <h2 class="page-title">购物车管理</h2>
        <p class="page-desc">按用户查看购物车商品，支持数量调整和删除。</p>
      </div>
    </div>

    <el-card class="block-card" shadow="hover">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="用户">
          <el-select v-model="selectedUserId" placeholder="请选择用户" style="width: 230px" filterable>
            <el-option v-for="u in userOptions" :key="u.id" :label="`${u.username} (${u.phone || '-'})`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品">
          <el-select v-model="addForm.productId" placeholder="请选择商品" style="width: 240px" filterable>
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
      <div v-loading="loading">
        <el-empty v-if="!items.length && !loading" description="暂无购物车数据" />
        <template v-else>
          <el-table :data="items" border row-class-name="table-row">
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
      </div>
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { addCartItemApi, deleteCartItemApi, listCartByUserApi, updateCartItemApi } from "../api/cart";
import { allProductsApi } from "../api/product";
import { listUsersApi } from "../api/user";

const userOptions = ref([]);
const productOptions = ref([]);
const selectedUserId = ref(null);
const addForm = reactive({ productId: null, quantity: 1 });

const items = ref([]);
const loading = ref(false);
const summary = reactive({ itemCount: 0, totalQuantity: 0, totalAmount: 0 });

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

watch(selectedUserId, loadCart);

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

:deep(.table-row) {
  height: 50px;
}

.summary-card {
  background: linear-gradient(180deg, #ffffff 0%, #fafcff 100%);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  padding: 12px;
  border-radius: 12px;
  background: #f5f8ff;
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

@media (max-width: 960px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
