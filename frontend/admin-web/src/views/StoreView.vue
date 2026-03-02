<template>
  <div>
    <div class="page-header">
      <h3>店铺管理</h3>
      <div>
        <el-input v-model="query.name" placeholder="店铺名称" style="width: 180px; margin-right: 8px" />
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 120px; margin-right: 8px">
          <el-option label="营业中" :value="1" />
          <el-option label="已关闭" :value="0" />
        </el-select>
        <el-button @click="loadData">查询</el-button>
        <el-button type="primary" @click="openCreate">新增店铺</el-button>
      </div>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="店铺名称" />
      <el-table-column prop="contactName" label="联系人" width="130" />
      <el-table-column prop="contactPhone" label="联系电话" width="150" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">{{ scope.row.status === 1 ? "营业中" : "已关闭" }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="180" />
      <el-table-column label="操作" width="220">
        <template #default="scope">
          <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button link type="danger" @click="remove(scope.row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑店铺' : '新增店铺'" width="500px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="店铺名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactName" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="营业中" :value="1" />
            <el-option label="已关闭" :value="0" />
          </el-select>
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
import { ElMessage, ElMessageBox } from "element-plus";
import { createStoreApi, deleteStoreApi, listStoresApi, updateStoreApi } from "../api/store";

const list = ref([]);
const total = ref(0);
const query = reactive({ pageNum: 1, pageSize: 10, name: "", status: null });
const dialogVisible = ref(false);
const isEdit = ref(false);
const form = reactive({ id: null, name: "", contactName: "", contactPhone: "", status: 1 });

const loadData = async () => {
  const res = await listStoresApi(query);
  list.value = res.records || [];
  total.value = Number(res.total || 0);
};

const openCreate = () => {
  isEdit.value = false;
  Object.assign(form, { id: null, name: "", contactName: "", contactPhone: "", status: 1 });
  dialogVisible.value = true;
};

const openEdit = (row) => {
  isEdit.value = true;
  Object.assign(form, {
    id: row.id,
    name: row.name,
    contactName: row.contactName || "",
    contactPhone: row.contactPhone || "",
    status: Number(row.status)
  });
  dialogVisible.value = true;
};

const submit = async () => {
  if (!form.name) {
    ElMessage.warning("请输入店铺名称");
    return;
  }
  const payload = {
    name: form.name,
    contactName: form.contactName,
    contactPhone: form.contactPhone,
    status: form.status
  };
  if (isEdit.value) {
    await updateStoreApi(form.id, payload);
  } else {
    await createStoreApi(payload);
  }
  dialogVisible.value = false;
  await loadData();
  ElMessage.success("操作成功");
};

const remove = async (row) => {
  await ElMessageBox.confirm(`确认删除店铺 ${row.name} 吗？`, "提示", { type: "warning" });
  await deleteStoreApi(row.id);
  await loadData();
  ElMessage.success("删除成功");
};

const onPageChange = async (page) => {
  query.pageNum = page;
  await loadData();
};

onMounted(loadData);
</script>

<style scoped>
.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
