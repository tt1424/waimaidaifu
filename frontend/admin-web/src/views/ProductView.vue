<template>
  <section class="page-wrap">
    <div class="page-top">
      <div>
        <h2 class="page-title">商品管理</h2>
        <p class="page-desc">按店铺维护商品信息、库存和上下架状态。</p>
      </div>
      <el-button type="primary" @click="openCreate">新增商品</el-button>
    </div>

    <el-card class="block-card" shadow="hover">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="店铺">
          <el-select v-model="query.storeId" clearable placeholder="全部店铺" style="width: 180px">
            <el-option v-for="s in storeOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品名称">
          <el-input v-model="query.name" placeholder="请输入商品名称" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 140px">
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
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
        <el-empty v-if="!list.length && !loading" description="暂无商品数据" />
        <template v-else>
          <el-table :data="list" border row-class-name="table-row">
            <el-table-column prop="id" label="ID" width="80" align="center" />
            <el-table-column prop="storeName" label="店铺" min-width="140" />
            <el-table-column prop="name" label="商品名称" min-width="160" />
            <el-table-column prop="price" label="单价" width="110" align="right">
              <template #default="{ row }">￥{{ row.price }}</template>
            </el-table-column>
            <el-table-column prop="stock" label="库存" width="90" align="center" />
            <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? "上架" : "下架" }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" min-width="170" />
            <el-table-column label="操作" width="180" align="center" fixed="right">
              <template #default="{ row }">
                <el-space>
                  <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                  <el-button link @click="changeStatus(row, row.status === 1 ? 0 : 1)">
                    {{ row.status === 1 ? "下架" : "上架" }}
                  </el-button>
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
      :title="isEdit ? '编辑商品' : '新增商品'"
      width="560px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form :model="form" label-width="100px">
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
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { createProductApi, listProductsApi, updateProductApi, updateProductStatusApi } from "../api/product";
import { allStoresApi } from "../api/store";

const list = ref([]);
const total = ref(0);
const loading = ref(false);
const storeOptions = ref([]);
const query = reactive({ pageNum: 1, pageSize: 10, storeId: null, name: "", status: null });
const dialogVisible = ref(false);
const isEdit = ref(false);
const form = reactive({ id: null, storeId: null, name: "", price: 0.01, stock: 0, description: "" });

const loadStores = async () => {
  storeOptions.value = await allStoresApi();
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await listProductsApi(query);
    list.value = res.records || [];
    total.value = Number(res.total || 0);
  } finally {
    loading.value = false;
  }
};

const resetQuery = async () => {
  Object.assign(query, { pageNum: 1, pageSize: 10, storeId: null, name: "", status: null });
  await loadData();
};

const resetForm = () => {
  Object.assign(form, { id: null, storeId: null, name: "", price: 0.01, stock: 0, description: "" });
};

const openCreate = async () => {
  await loadStores();
  if (!storeOptions.value.length) {
    ElMessage.warning("请先创建店铺");
    return;
  }
  isEdit.value = false;
  resetForm();
  form.storeId = storeOptions.value[0].id;
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
