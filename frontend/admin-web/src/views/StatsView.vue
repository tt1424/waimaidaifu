<template>
  <div>
    <div class="page-header">
      <h3>数据统计</h3>
      <div>
        <el-date-picker
          v-model="range"
          type="datetimerange"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 360px; margin-right: 8px"
        />
        <el-button @click="loadData">查询</el-button>
        <el-button type="primary" @click="generateReport">生成日报</el-button>
      </div>
    </div>

    <el-row :gutter="12">
      <el-col :span="8"><el-card>活跃用户数：{{ summary.activeUserCount || 0 }}</el-card></el-col>
      <el-col :span="8"><el-card>总商品数量：{{ summary.totalQuantity || 0 }}</el-card></el-col>
      <el-col :span="8"><el-card>总金额：{{ summary.totalAmount || 0 }}</el-card></el-col>
    </el-row>

    <el-card style="margin-top: 12px">
      <template #header>用户统计柱状图</template>
      <div class="bar-chart">
        <div v-for="row in list" :key="row.userId" class="bar-row">
          <div class="bar-label">{{ row.username }}</div>
          <div class="bar-track">
            <div class="bar-value" :style="{ width: `${barWidth(row.totalAmount)}%` }"></div>
          </div>
          <div class="bar-num">¥ {{ row.totalAmount }}</div>
        </div>
      </div>
    </el-card>

    <el-card style="margin-top: 12px">
      <template #header>用户统计折线图</template>
      <svg viewBox="0 0 700 180" class="line-chart">
        <polyline
          fill="none"
          stroke="#409eff"
          stroke-width="3"
          :points="linePoints"
        />
      </svg>
    </el-card>

    <el-table :data="list" border style="margin-top: 12px">
      <el-table-column prop="userId" label="用户ID" width="100" />
      <el-table-column prop="username" label="用户名" min-width="160" />
      <el-table-column prop="totalQuantity" label="商品数量" width="120" />
      <el-table-column prop="totalAmount" label="总金额" width="140" />
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { generateReportApi, statsSummaryApi, userStatsApi } from "../api/stats";

const range = ref([]);
const query = reactive({ pageNum: 1, pageSize: 10, startTime: "", endTime: "" });
const list = ref([]);
const total = ref(0);
const summary = reactive({ activeUserCount: 0, totalQuantity: 0, totalAmount: 0 });

const syncRange = () => {
  query.startTime = range.value?.[0] || "";
  query.endTime = range.value?.[1] || "";
};

const loadData = async () => {
  syncRange();
  const [usersRes, summaryRes] = await Promise.all([
    userStatsApi(query),
    statsSummaryApi({ startTime: query.startTime, endTime: query.endTime })
  ]);
  list.value = usersRes.records || [];
  total.value = Number(usersRes.total || 0);
  Object.assign(summary, summaryRes || { activeUserCount: 0, totalQuantity: 0, totalAmount: 0 });
};

const onPageChange = async (page) => {
  query.pageNum = page;
  await loadData();
};

const generateReport = async () => {
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
.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.bar-chart {
  display: grid;
  gap: 10px;
}

.bar-row {
  display: grid;
  grid-template-columns: 120px 1fr 120px;
  align-items: center;
  gap: 10px;
}

.bar-track {
  background: #f0f3f9;
  border-radius: 8px;
  height: 12px;
  overflow: hidden;
}

.bar-value {
  height: 100%;
  background: linear-gradient(90deg, #67c23a, #409eff);
}

.line-chart {
  width: 100%;
  height: 180px;
  background: #fafcff;
  border: 1px solid #edf1f7;
}
</style>
