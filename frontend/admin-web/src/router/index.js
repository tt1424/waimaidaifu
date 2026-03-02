import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "../stores/auth";

const LoginView = () => import("../views/LoginView.vue");
const LayoutView = () => import("../views/LayoutView.vue");
const UserView = () => import("../views/UserView.vue");
const StoreView = () => import("../views/StoreView.vue");
const ProductView = () => import("../views/ProductView.vue");
const CartView = () => import("../views/CartView.vue");
const StatsView = () => import("../views/StatsView.vue");

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/login", component: LoginView },
    {
      path: "/",
      component: LayoutView,
      children: [
        { path: "", redirect: "/users" },
        { path: "/users", component: UserView },
        { path: "/stores", component: StoreView },
        { path: "/products", component: ProductView },
        { path: "/cart", component: CartView },
        { path: "/stats", component: StatsView }
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
    return "/users";
  }
  return true;
});

export default router;
