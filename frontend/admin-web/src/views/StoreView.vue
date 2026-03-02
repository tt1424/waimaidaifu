<template>
  <section class="page-wrap">
    <div class="page-top">
      <div>
        <h2 class="page-title">店铺管理</h2>
        <p class="page-desc">维护店铺基础信息与营业状态。</p>
      </div>
      <el-button type="primary" @click="openCreate">新增店铺</el-button>
    </div>

    <el-card class="block-card" shadow="hover">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="店铺名称">
          <el-input v-model="query.name" placeholder="请输入店铺名称" clearable style="width: 220px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 140px">
            <el-option label="营业中" :value="1" />
            <el-option label="已关闭" :value="0" />
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
        <el-empty v-if="!list.length && !loading" description="暂无店铺数据" />
        <template v-else>
          <el-table :data="list" border row-class-name="table-row">
            <el-table-column prop="id" label="ID" width="80" align="center" />
            <el-table-column prop="name" label="店铺名称" min-width="180" />
            <el-table-column prop="contactName" label="联系人" min-width="120" />
            <el-table-column prop="contactPhone" label="联系电话" min-width="140" />
            <el-table-column prop="status" label="状态" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? "营业中" : "已关闭" }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" min-width="170" />
            <el-table-column label="操作" width="170" align="center" fixed="right">
              <template #default="{ row }">
                <el-space>
                  <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                  <el-popconfirm title="确认删除该店铺吗？" @confirm="remove(row)">
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
      :title="isEdit ? '编辑店铺' : '新增店铺'"
      width="520px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form :model="form" label-width="100px">
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
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { createStoreApi, deleteStoreApi, listStoresApi, updateStoreApi } from "../api/store";

const list = ref([]);
const total = ref(0);
const loading = ref(false);
const query = reactive({ pageNum: 1, pageSize: 10, name: "", status: null });
const dialogVisible = ref(false);
const isEdit = ref(false);
const form = reactive({ id: null, name: "", contactName: "", contactPhone: "", status: 1 });

const loadData = async () => {
  loading.value = true;
  try {
    const res = await listStoresApi(query);
    list.value = res.records || [];
    total.value = Number(res.total || 0);
  } finally {
    loading.value = false;
  }
};

const resetQuery = async () => {
  Object.assign(query, { pageNum: 1, pageSize: 10, name: "", status: null });
  await loadData();
};

const resetForm = () => {
  Object.assign(form, { id: null, name: "", contactName: "", contactPhone: "", status: 1 });
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
