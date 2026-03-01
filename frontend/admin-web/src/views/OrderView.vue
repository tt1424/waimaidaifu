<template>
  <div>
    <div class="page-header">
      <h3>订单与链接</h3>
      <div>
        <el-input v-model="query.orderNo" placeholder="订单号" style="width: 220px; margin-right: 8px" />
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 120px; margin-right: 8px">
          <el-option label="未支付" :value="0" />
          <el-option label="已支付" :value="1" />
          <el-option label="已过期" :value="2" />
        </el-select>
        <el-button @click="loadData">查询</el-button>
        <el-button type="primary" @click="openCreate">生成代付链接</el-button>
      </div>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="orderNo" label="订单号" min-width="220" />
      <el-table-column prop="productName" label="商品名称" min-width="180" />
      <el-table-column prop="amount" label="订单金额" width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">{{ statusMap[scope.row.status] || scope.row.status }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="170" />
      <el-table-column prop="payTime" label="支付时间" min-width="170" />
      <el-table-column label="代付链接" min-width="300">
        <template #default="scope">
          <el-input :model-value="scope.row.payUrl" readonly>
            <template #append>
              <el-button @click="copyLink(scope.row.payUrl)">复制</el-button>
            </template>
          </el-input>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button v-if="scope.row.status === 0" link type="success" @click="mockPay(scope.row)">模拟支付</el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="createDialog" title="生成代付订单" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="选择商品">
          <el-select v-model="form.productId" placeholder="请选择商品" style="width: 100%" @change="onProductChange">
            <el-option v-for="item in productOptions" :key="item.id" :label="`${item.name}（${item.price}）`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品名称"><el-input v-model="form.productName" /></el-form-item>
        <el-form-item label="订单金额"><el-input-number v-model="form.amount" :min="0.01" :precision="2" /></el-form-item>
        <el-form-item label="过期分钟"><el-input-number v-model="form.expireMinutes" :min="1" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" @click="createOrder">生成链接</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { createOrderApi, listOrdersApi, payNotifyApi } from "../api/order";
import { listProductsApi } from "../api/product";

const statusMap = { 0: "未支付", 1: "已支付", 2: "已过期", 3: "已取消" };
const list = ref([]);
const productOptions = ref([]);

const query = reactive({ pageNum: 1, pageSize: 10, status: null, orderNo: "" });
const createDialog = ref(false);
const form = reactive({ productId: null, productName: "", amount: 0.01, expireMinutes: 30 });

const loadData = async () => {
  const res = await listOrdersApi(query);
  list.value = res.records || [];
};

const loadProducts = async () => {
  const res = await listProductsApi({ pageNum: 1, pageSize: 999, status: 1 });
  productOptions.value = res.records || [];
};

const openCreate = async () => {
  await loadProducts();
  if (!productOptions.value.length) {
    ElMessage.warning("请先在商品管理中创建并上架商品");
    return;
  }
  const first = productOptions.value[0];
  form.productId = first.id;
  form.productName = first.name;
  form.amount = Number(first.price);
  form.expireMinutes = 30;
  createDialog.value = true;
};

const onProductChange = (id) => {
  const row = productOptions.value.find((x) => x.id === id);
  if (!row) return;
  form.productName = row.name;
  form.amount = Number(row.price);
};

const createOrder = async () => {
  const created = await createOrderApi(form);
  createDialog.value = false;
  await loadData();
  ElMessage.success("代付链接生成成功");
  await copyLink(created.payUrl);
};

const mockPay = async (row) => {
  await payNotifyApi({
    orderNo: row.orderNo,
    payAmount: row.amount,
    payTime: new Date().toISOString().slice(0, 19).replace("T", " ")
  });
  await loadData();
  ElMessage.success("模拟支付成功");
};

const copyLink = async (url) => {
  if (!url) return;
  await navigator.clipboard.writeText(url);
  ElMessage.success("链接已复制");
};

onMounted(loadData);
</script>
