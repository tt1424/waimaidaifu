<template>
  <el-container style="min-height: 100vh">
    <el-aside width="220px" style="background: #001529">
      <div class="logo">代付管理后台</div>
      <el-menu router :default-active="$route.path" background-color="#001529" text-color="#fff" active-text-color="#ffd04b">
        <el-menu-item index="/dashboard">数据概览</el-menu-item>
        <el-menu-item index="/staff">人员管理</el-menu-item>
        <el-menu-item index="/products">商品管理</el-menu-item>
        <el-menu-item index="/orders">订单与链接</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span>代付订单管理系统</span>
        <el-button type="danger" plain @click="logout">退出登录</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";

const router = useRouter();
const authStore = useAuthStore();

const logout = () => {
  authStore.logout();
  router.push("/login");
};
</script>

<style scoped>
.logo {
  height: 56px;
  color: #fff;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header {
  background: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eee;
}
</style>
