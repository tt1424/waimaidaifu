<template>
  <div>
    <div class="page-header">
      <h3>人员管理</h3>
      <div>
        <el-input v-model="query.keyword" placeholder="账号/姓名" style="width: 180px; margin-right: 8px" />
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 120px; margin-right: 8px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button @click="loadData">查询</el-button>
        <el-button type="primary" @click="openCreate">新增人员</el-button>
      </div>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="登录账号" width="160" />
      <el-table-column prop="name" label="姓名" width="140" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="role" label="角色" width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">{{ scope.row.status === 1 ? "启用" : "禁用" }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" min-width="180" />
      <el-table-column label="操作" width="260">
        <template #default="scope">
          <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button link @click="changeStatus(scope.row)">{{ scope.row.status === 1 ? "禁用" : "启用" }}</el-button>
          <el-button link type="warning" @click="resetPwd(scope.row)">重置密码</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑人员' : '新增人员'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="登录账号">
          <el-input v-model="form.username" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="角色"><el-input v-model="form.role" /></el-form-item>
        <el-form-item v-if="!isEdit" label="初始密码"><el-input v-model="form.password" show-password /></el-form-item>
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
import {
  createStaffApi,
  listStaffApi,
  resetStaffPasswordApi,
  updateStaffApi,
  updateStaffStatusApi
} from "../api/staff";

const list = ref([]);
const query = reactive({ pageNum: 1, pageSize: 10, keyword: "", status: null });
const dialogVisible = ref(false);
const isEdit = ref(false);
const form = reactive({ id: null, username: "", name: "", phone: "", role: "运营", password: "123456" });

const loadData = async () => {
  const res = await listStaffApi(query);
  list.value = res.records || [];
};

const openCreate = () => {
  isEdit.value = false;
  Object.assign(form, { id: null, username: "", name: "", phone: "", role: "运营", password: "123456" });
  dialogVisible.value = true;
};

const openEdit = (row) => {
  isEdit.value = true;
  Object.assign(form, { id: row.id, username: row.username, name: row.name, phone: row.phone, role: row.role, password: "" });
  dialogVisible.value = true;
};

const submit = async () => {
  if (!form.username || !form.name) {
    ElMessage.warning("请填写完整信息");
    return;
  }
  if (isEdit.value) {
    await updateStaffApi(form.id, { name: form.name, phone: form.phone, role: form.role });
  } else {
    await createStaffApi({ username: form.username, name: form.name, phone: form.phone, role: form.role, password: form.password });
  }
  dialogVisible.value = false;
  await loadData();
  ElMessage.success("操作成功");
};

const changeStatus = async (row) => {
  await updateStaffStatusApi(row.id, { status: row.status === 1 ? 0 : 1 });
  await loadData();
  ElMessage.success("状态更新成功");
};

const resetPwd = async (row) => {
  await ElMessageBox.confirm(`确认将 ${row.username} 的密码重置为 123456 吗？`, "提示", { type: "warning" });
  await resetStaffPasswordApi(row.id, { password: "123456" });
  ElMessage.success("密码已重置为 123456");
};

onMounted(loadData);
</script>
