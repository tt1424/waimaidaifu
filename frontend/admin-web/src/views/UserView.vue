<template>
  <section class="page-wrap">
    <div class="page-top">
      <div>
        <h2 class="page-title">用户管理</h2>
        <p class="page-desc">管理平台账号、角色权限与启用状态。</p>
      </div>
      <el-button type="primary" @click="openCreate">新增用户</el-button>
    </div>

    <el-card class="block-card" shadow="hover">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="用户名">
          <el-input v-model="query.username" placeholder="请输入用户名" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="query.phone" placeholder="请输入手机号" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.role" clearable placeholder="全部" style="width: 140px">
            <el-option label="管理员" :value="1" />
            <el-option label="普通用户" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 140px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="block-card" shadow="hover">
      <div v-loading="loading">
        <el-empty v-if="!list.length && !loading" description="暂无用户数据" />
        <template v-else>
          <el-table :data="list" border row-class-name="table-row">
            <el-table-column prop="id" label="ID" width="80" align="center" />
            <el-table-column prop="username" label="用户名" min-width="180" />
            <el-table-column prop="phone" label="手机号" min-width="150" />
            <el-table-column prop="role" label="角色" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="row.role === 1 ? 'danger' : ''">{{ row.role === 1 ? "管理员" : "普通用户" }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? "启用" : "禁用" }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" min-width="170" />
            <el-table-column label="操作" width="170" align="center" fixed="right">
              <template #default="{ row }">
                <el-space>
                  <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                  <el-popconfirm title="确认删除该用户吗？" @confirm="remove(row)">
                    <template #reference>
                      <el-button link type="danger">删除</el-button>
                    </template>
                  </el-popconfirm>
                </el-space>
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
        </template>
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '新增用户'"
      width="520px"
      destroy-on-close
      @closed="resetForm"
    >
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
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { createUserApi, deleteUserApi, listUsersApi, updateUserApi } from "../api/user";

const list = ref([]);
const total = ref(0);
const loading = ref(false);
const query = reactive({ pageNum: 1, pageSize: 10, username: "", phone: "", role: null, status: null });
const dialogVisible = ref(false);
const isEdit = ref(false);
const form = reactive({ id: null, username: "", phone: "", role: 2, status: 1, password: "123456" });

const loadData = async () => {
  loading.value = true;
  try {
    const res = await listUsersApi(query);
    list.value = res.records || [];
    total.value = Number(res.total || 0);
  } finally {
    loading.value = false;
  }
};

const resetQuery = async () => {
  Object.assign(query, { pageNum: 1, pageSize: 10, username: "", phone: "", role: null, status: null });
  await loadData();
};

const resetForm = () => {
  Object.assign(form, { id: null, username: "", phone: "", role: 2, status: 1, password: "123456" });
};

const openCreate = () => {
  isEdit.value = false;
  resetForm();
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

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

:deep(.table-row) {
  height: 50px;
}
</style>
