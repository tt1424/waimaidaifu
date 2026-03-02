<template>
  <section class="page-wrap">
    <div class="page-top">
      <div>
        <h2 class="page-title">数据统计</h2>
        <p class="page-desc">查看用户购物行为汇总，并生成每日统计报表。</p>
      </div>
      <el-button type="primary" @click="generateReport">生成日报</el-button>
    </div>

    <el-card class="block-card" shadow="hover">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="range"
            type="datetimerange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 360px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="12">
      <el-col :xs="24" :sm="8">
        <el-card class="metric-card" shadow="hover">
          <div class="metric-label">活跃用户数</div>
          <div class="metric-value">{{ summary.activeUserCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="metric-card" shadow="hover">
          <div class="metric-label">总商品数量</div>
          <div class="metric-value">{{ summary.totalQuantity || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="metric-card" shadow="hover">
          <div class="metric-label">总金额</div>
          <div class="metric-value">￥{{ summary.totalAmount || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="block-card" shadow="hover">
      <template #header>
        <div class="card-header">用户消费柱状图</div>
      </template>
      <el-empty v-if="!list.length" description="暂无统计数据" />
      <div v-else class="bar-chart">
        <div v-for="row in list" :key="row.userId" class="bar-row">
          <div class="bar-label">{{ row.username }}</div>
          <div class="bar-track">
            <div class="bar-value" :style="{ width: `${barWidth(row.totalAmount)}%` }"></div>
          </div>
          <div class="bar-num">￥{{ row.totalAmount }}</div>
        </div>
      </div>
    </el-card>

    <el-card class="block-card" shadow="hover">
      <template #header>
        <div class="card-header">用户消费折线图</div>
      </template>
      <svg viewBox="0 0 700 180" class="line-chart">
        <polyline fill="none" stroke="#409eff" stroke-width="3" :points="linePoints" />
      </svg>
    </el-card>

    <el-card class="block-card" shadow="hover">
      <div v-loading="loading">
        <el-empty v-if="!list.length && !loading" description="暂无列表数据" />
        <template v-else>
          <el-table :data="list" border row-class-name="table-row">
            <el-table-column prop="userId" label="用户ID" width="100" align="center" />
            <el-table-column prop="username" label="用户名" min-width="180" />
            <el-table-column prop="totalQuantity" label="商品数量" width="120" align="center" />
            <el-table-column prop="totalAmount" label="总金额" width="140" align="right">
              <template #default="{ row }">￥{{ row.totalAmount }}</template>
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
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { generateReportApi, statsSummaryApi, userStatsApi } from "../api/stats";

const range = ref([]);
const query = reactive({ pageNum: 1, pageSize: 10, startTime: "", endTime: "" });
const list = ref([]);
const total = ref(0);
const loading = ref(false);
const summary = reactive({ activeUserCount: 0, totalQuantity: 0, totalAmount: 0 });

const syncRange = () => {
  query.startTime = range.value?.[0] || "";
  query.endTime = range.value?.[1] || "";
};

const loadData = async () => {
  syncRange();
  loading.value = true;
  try {
    const [usersRes, summaryRes] = await Promise.all([
      userStatsApi(query),
      statsSummaryApi({ startTime: query.startTime, endTime: query.endTime })
    ]);
    list.value = usersRes.records || [];
    total.value = Number(usersRes.total || 0);
    Object.assign(summary, summaryRes || { activeUserCount: 0, totalQuantity: 0, totalAmount: 0 });
  } finally {
    loading.value = false;
  }
};

const resetFilters = async () => {
  range.value = [];
  Object.assign(query, { pageNum: 1, pageSize: 10, startTime: "", endTime: "" });
  await loadData();
};

const onPageChange = async (page) => {
  query.pageNum = page;
  await loadData();
};

const generateReport = async () => {
  syncRange();
  const statDate = query.startTime ? query.startTime.slice(0, 10) : undefined;
  const res = await generateReportApi({ statDate });
  ElMessage.success(`日报生成成功：${res.statDate}（${res.userCount}人）`);
};

const barWidth = (amount) => {
  const max = Math.max(...list.value.map((x) => Number(x.totalAmount || 0)), 1);
  return (Number(amount || 0) / max) * 100;
};

const linePoints = computed(() => {
  if (!list.value.length) return "0,160 700,160";
  const amounts = list.value.map((x) => Number(x.totalAmount || 0));
  const max = Math.max(...amounts, 1);
  const step = 700 / Math.max(amounts.length - 1, 1);
  return amounts
    .map((v, idx) => {
      const x = idx * step;
      const y = 160 - (v / max) * 140;
      return `${x},${y}`;
    })
    .join(" ");
});

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

.card-header {
  font-weight: 600;
}

.filter-form {
  margin-bottom: -18px;
}

.metric-card {
  border-radius: 14px;
}

.metric-label {
  color: #6b7280;
  font-size: 13px;
}

.metric-value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 600;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.bar-chart {
  display: grid;
  gap: 10px;
}

.bar-row {
  display: grid;
  grid-template-columns: 140px 1fr 140px;
  align-items: center;
  gap: 10px;
}

.bar-track {
  background: #edf3ff;
  border-radius: 8px;
  height: 12px;
  overflow: hidden;
}

.bar-value {
  height: 100%;
  background: linear-gradient(90deg, #79bbff, #409eff);
}

.line-chart {
  width: 100%;
  height: 180px;
  background: #fafcff;
  border: 1px solid #edf1f7;
  border-radius: 10px;
}

:deep(.table-row) {
  height: 50px;
}

@media (max-width: 960px) {
  .bar-row {
    grid-template-columns: 1fr;
  }
}
</style>
