<template>
  <div>
    <div class="page-header">
      <h3>数据概览</h3>
      <el-button @click="loadData">刷新</el-button>
    </div>
    <el-row :gutter="16">
      <el-col :span="6"><el-card>总支付金额：{{ data.totalPayAmount }}</el-card></el-col>
      <el-col :span="6"><el-card>今日支付金额：{{ data.todayPayAmount }}</el-card></el-col>
      <el-col :span="6"><el-card>支付笔数：{{ data.payCount }}</el-card></el-col>
      <el-col :span="6"><el-card>未支付订单：{{ data.unpaidCount }}</el-card></el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive } from "vue";
import { getOverviewApi } from "../api/report";

const data = reactive({
  totalPayAmount: 0,
  todayPayAmount: 0,
  payCount: 0,
  unpaidCount: 0
});

const loadData = async () => {
  const res = await getOverviewApi();
  Object.assign(data, res);
};

onMounted(loadData);
</script>
