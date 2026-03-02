<template>
  <div>
    <div class="page-header">
      <h3>商品管理</h3>
      <div>
        <el-select v-model="query.storeId" clearable placeholder="店铺" style="width: 180px; margin-right: 8px">
          <el-option v-for="s in storeOptions" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-input v-model="query.name" placeholder="商品名称" style="width: 160px; margin-right: 8px" />
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 120px; margin-right: 8px">
          <el-option label="上架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
        <el-button @click="loadData">查询</el-button>
        <el-button type="primary" @click="openCreate">新增商品</el-button>
      </div>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="storeName" label="店铺" min-width="140" />
      <el-table-column prop="name" label="商品名称" min-width="160" />
      <el-table-column prop="price" label="价格" width="100" />
      <el-table-column prop="stock" label="库存" width="100" />
      <el-table-column prop="description" label="描述" min-width="200" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">{{ scope.row.status === 1 ? "上架" : "下架" }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="180" />
      <el-table-column label="操作" width="220">
        <template #default="scope">
          <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button link @click="changeStatus(scope.row, scope.row.status === 1 ? 0 : 1)">
            {{ scope.row.status === 1 ? "下架" : "上架" }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.pageSize"
        :current-page="query.pageNum"
        @current-change="onPageChange"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑商品' : '新增商品'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="店铺">
          <el-select v-model="form.storeId" style="width: 100%">
            <el-option v-for="s in storeOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="商品价格">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="form.stock" :min="0" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { createProductApi, listProductsApi, updateProductApi, updateProductStatusApi } from "../api/product";
import { allStoresApi } from "../api/store";

const list = ref([]);
const total = ref(0);
const storeOptions = ref([]);
const query = reactive({ pageNum: 1, pageSize: 10, storeId: null, name: "", status: null });
const dialogVisible = ref(false);
const isEdit = ref(false);
const form = reactive({ id: null, storeId: null, name: "", price: 0.01, stock: 0, description: "" });

const loadStores = async () => {
  storeOptions.value = await allStoresApi();
};

const loadData = async () => {
  const res = await listProductsApi(query);
  list.value = res.records || [];
  total.value = Number(res.total || 0);
};

const openCreate = async () => {
  await loadStores();
  if (!storeOptions.value.length) {
    ElMessage.warning("请先创建店铺");
    return;
  }
  const first = storeOptions.value[0];
  isEdit.value = false;
  Object.assign(form, { id: null, storeId: first.id, name: "", price: 0.01, stock: 0, description: "" });
  dialogVisible.value = true;
};

const openEdit = async (row) => {
  await loadStores();
  isEdit.value = true;
  Object.assign(form, {
    id: row.id,
    storeId: row.storeId,
    name: row.name,
    price: Number(row.price),
    stock: Number(row.stock),
    description: row.description || ""
  });
  dialogVisible.value = true;
};

const submit = async () => {
  if (!form.storeId || !form.name) {
    ElMessage.warning("请填写完整信息");
    return;
  }
  const payload = {
    storeId: form.storeId,
    name: form.name,
    price: form.price,
    stock: form.stock,
    description: form.description
  };
  if (isEdit.value) {
    await updateProductApi(form.id, payload);
  } else {
    await createProductApi(payload);
  }
  dialogVisible.value = false;
  await loadData();
  ElMessage.success("操作成功");
};

const changeStatus = async (row, status) => {
  await updateProductStatusApi(row.id, { status });
  await loadData();
  ElMessage.success("状态更新成功");
};

const onPageChange = async (page) => {
  query.pageNum = page;
  await loadData();
};

onMounted(async () => {
  await loadStores();
  await loadData();
});
</script>

<style scoped>
.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
