import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "../stores/auth";

const LoginView = () => import("../views/LoginView.vue");
const LayoutView = () => import("../views/LayoutView.vue");
const DashboardView = () => import("../views/DashboardView.vue");
const StaffView = () => import("../views/StaffView.vue");
const ProductView = () => import("../views/ProductView.vue");
const OrderView = () => import("../views/OrderView.vue");

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/login", component: LoginView },
    {
      path: "/",
      component: LayoutView,
      children: [
        { path: "", redirect: "/dashboard" },
        { path: "/dashboard", component: DashboardView },
        { path: "/staff", component: StaffView },
        { path: "/products", component: ProductView },
        { path: "/orders", component: OrderView }
      ]
    }
  ]
});

router.beforeEach((to) => {
  const authStore = useAuthStore();
  if (to.path !== "/login" && !authStore.token) {
    return "/login";
  }
  if (to.path === "/login" && authStore.token) {
    return "/dashboard";
  }
  return true;
});

export default router;
