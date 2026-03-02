<template>
  <div>
    <div class="page-header">
      <h3>用户管理</h3>
      <div>
        <el-input v-model="query.username" placeholder="用户名" style="width: 160px; margin-right: 8px" />
        <el-input v-model="query.phone" placeholder="手机号" style="width: 160px; margin-right: 8px" />
        <el-select v-model="query.role" clearable placeholder="角色" style="width: 120px; margin-right: 8px">
          <el-option label="管理员" :value="1" />
          <el-option label="普通用户" :value="2" />
        </el-select>
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 120px; margin-right: 8px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button @click="loadData">查询</el-button>
        <el-button type="primary" @click="openCreate">新增用户</el-button>
      </div>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="180" />
      <el-table-column prop="phone" label="手机号" width="160" />
      <el-table-column prop="role" label="角色" width="120">
        <template #default="scope">{{ scope.row.role === 1 ? "管理员" : "普通用户" }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">{{ scope.row.status === 1 ? "启用" : "禁用" }}</el-tag>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="500px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="管理员" :value="1" />
            <el-option label="普通用户" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" show-password :placeholder="isEdit ? '不填则不修改' : '请输入密码'" />
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
import { createUserApi, deleteUserApi, listUsersApi, updateUserApi } from "../api/user";

const list = ref([]);
const total = ref(0);
const query = reactive({ pageNum: 1, pageSize: 10, username: "", phone: "", role: null, status: null });
const dialogVisible = ref(false);
const isEdit = ref(false);
const form = reactive({ id: null, username: "", phone: "", role: 2, status: 1, password: "123456" });

const loadData = async () => {
  const res = await listUsersApi(query);
  list.value = res.records || [];
  total.value = Number(res.total || 0);
};

const openCreate = () => {
  isEdit.value = false;
  Object.assign(form, { id: null, username: "", phone: "", role: 2, status: 1, password: "123456" });
  dialogVisible.value = true;
};

const openEdit = (row) => {
  isEdit.value = true;
  Object.assign(form, {
    id: row.id,
    username: row.username,
    phone: row.phone || "",
    role: Number(row.role),
    status: Number(row.status),
    password: ""
  });
  dialogVisible.value = true;
};

const submit = async () => {
  if (!form.username) {
    ElMessage.warning("请输入用户名");
    return;
  }
  if (!isEdit.value && !form.password) {
    ElMessage.warning("请输入密码");
    return;
  }
  if (isEdit.value) {
    await updateUserApi(form.id, {
      username: form.username,
      phone: form.phone,
      role: form.role,
      status: form.status,
      password: form.password || undefined
    });
  } else {
    await createUserApi({
      username: form.username,
      phone: form.phone,
      role: form.role,
      status: form.status,
      password: form.password
    });
  }
  dialogVisible.value = false;
  await loadData();
  ElMessage.success("操作成功");
};

const remove = async (row) => {
  await ElMessageBox.confirm(`确认删除用户 ${row.username} 吗？`, "提示", { type: "warning" });
  await deleteUserApi(row.id);
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
