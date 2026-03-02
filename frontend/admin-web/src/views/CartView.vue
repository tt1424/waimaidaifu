<template>
  <div>
    <div class="page-header">
      <h3>购物车</h3>
      <div>
        <el-select v-model="selectedUserId" placeholder="选择用户" style="width: 220px; margin-right: 8px">
          <el-option v-for="u in userOptions" :key="u.id" :label="`${u.username} (${u.phone || '-'})`" :value="u.id" />
        </el-select>
        <el-select v-model="addForm.productId" placeholder="选择商品" style="width: 220px; margin-right: 8px">
          <el-option v-for="p in productOptions" :key="p.id" :label="`${p.name}（库存:${p.stock}）`" :value="p.id" />
        </el-select>
        <el-input-number v-model="addForm.quantity" :min="1" style="margin-right: 8px" />
        <el-button type="primary" @click="addItem">加入购物车</el-button>
      </div>
    </div>

    <el-table :data="items" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="productName" label="商品名称" min-width="180" />
      <el-table-column prop="unitPrice" label="单价" width="100" />
      <el-table-column label="数量" width="160">
        <template #default="scope">
          <el-input-number :model-value="scope.row.quantity" :min="1" @change="(v) => updateQty(scope.row, v)" />
        </template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="金额" width="120" />
      <el-table-column prop="updateTime" label="更新时间" min-width="180" />
      <el-table-column label="操作" width="100">
        <template #default="scope">
          <el-button link type="danger" @click="removeItem(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="summary">
      <el-card>
        购物车商品数：{{ summary.itemCount || 0 }}，
        总数量：{{ summary.totalQuantity || 0 }}，
        总金额：{{ summary.totalAmount || 0 }}
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { addCartItemApi, deleteCartItemApi, listCartByUserApi, updateCartItemApi } from "../api/cart";
import { allProductsApi } from "../api/product";
import { listUsersApi } from "../api/user";

const userOptions = ref([]);
const productOptions = ref([]);
const selectedUserId = ref(null);
const addForm = reactive({ productId: null, quantity: 1 });

const items = ref([]);
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
  const res = await listCartByUserApi({ userId: selectedUserId.value });
  items.value = res.items || [];
  Object.assign(summary, res.summary || { itemCount: 0, totalQuantity: 0, totalAmount: 0 });
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
  await ElMessageBox.confirm("确认删除该购物车商品吗？", "提示", { type: "warning" });
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
.summary {
  margin-top: 12px;
}
</style>
