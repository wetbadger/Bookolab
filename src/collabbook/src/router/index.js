import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from "@/stores/authStore.js";
import Page from '@/views/Page.vue';
import Login from '@/views/Login.vue';
import Signup from '@/views/Signup.vue';
import Me from '@/views/Me.vue';
import Author from '@/views/Author.vue';
import DeleteAccount from '@/views/DeleteAccount.vue';
import Leaderboard from '@/views/Leaderboard.vue';

const isDebugMode = false;

const routes = [
  {
    path: '/',
    redirect: '/pages/1' // Automatically forwards the user to Page 1
  },
  {
    path: '/pages/:id',
    name: 'page-view',
    component: Page,
    // Passes the URL id as a prop, and sets edit mode to false
    props: route => ({ id: Number(route.params.id), isEditMode: false, isDebugMode: isDebugMode })
  },
  {
    path: '/pages/:id/edit',
    name: 'page-edit',
    component: Page,
    // Passes the URL id as a prop, and sets edit mode to true
    props: route => ({ id: Number(route.params.id), isEditMode: true, isDebugMode: isDebugMode }),
    meta: { requiresAuth: true }
  },
  {
    path: '/login',
    name: 'login',
    component: Login
  },
  {
    path: '/signup',
    name: 'signup',
    component: Signup
  },
  {
    path: '/me',
    name: 'me',
    component: Me
  },
  {
    path: '/authors/:name',
    name: 'author',
    component: Author,
    props: route => ({ username: route.params.name })
  },
  {
    path: '/delete-account',
    name: 'delete-account',
    component: DeleteAccount
  },
  {
    path: '/leaderboard',
    name: 'leaderboard',
    component: Leaderboard
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach(async (to, from) => {
  const authStore = useAuthStore();

  // If the route requires authentication
  if (to.meta.requiresAuth) {

    // If the store doesn't think we're authenticated but we have a raw token, verify it
    if (!authStore.isAuthenticated && authStore.token) {
      await authStore.fetchCurrentUser();
    }

    // Now make the definitive decision based on the updated state
    if (!authStore.isAuthenticated) {
      // 🛑 NOT Authenticated: Return the redirect object directly!
      return {
        path: '/login',
        query: { redirectFrom: to.fullPath }
      };
    }
  }

  // ✅ Authenticated or Public Page: Return nothing (or true) to allow the navigation
  return true;
});

export default router;
