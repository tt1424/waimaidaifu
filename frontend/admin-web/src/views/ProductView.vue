<template>
  <div>
    <div class="page-header">
      <h3>商品管理</h3>
      <div>
        <el-input v-model="query.name" placeholder="商品名称" style="width: 180px; margin-right: 8px" />
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
      <el-table-column prop="name" label="商品名称" />
      <el-table-column prop="price" label="商品价格" />
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑商品' : '新增商品'" width="420px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="价格"><el-input-number v-model="form.price" :min="0.01" :precision="2" /></el-form-item>
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

const list = ref([]);
const query = reactive({ pageNum: 1, pageSize: 10, name: "", status: null });
const dialogVisible = ref(false);
const isEdit = ref(false);
const form = reactive({ id: null, name: "", price: 0.01 });

const loadData = async () => {
  const res = await listProductsApi(query);
  list.value = res.records || [];
};

const openCreate = () => {
  isEdit.value = false;
  form.id = null;
  form.name = "";
  form.price = 0.01;
  dialogVisible.value = true;
};

const openEdit = (row) => {
  isEdit.value = true;
  form.id = row.id;
  form.name = row.name;
  form.price = Number(row.price);
  dialogVisible.value = true;
};

const submit = async () => {
  if (!form.name) {
    ElMessage.warning("请输入商品名称");
    return;
  }
  if (isEdit.value) {
    await updateProductApi(form.id, { name: form.name, price: form.price });
  } else {
    await createProductApi({ name: form.name, price: form.price });
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

onMounted(loadData);
</script>
