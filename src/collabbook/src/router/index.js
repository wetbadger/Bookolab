import { createRouter, createWebHistory } from 'vue-router';
import Page from '@/views/Page.vue';
import Login from '@/views/Login.vue';
import Signup from '@/views/Signup.vue';

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
    props: route => ({ id: route.params.id, isEditMode: false, isDebugMode: isDebugMode })
  },
  {
    path: '/pages/:id/edit',
    name: 'page-edit',
    component: Page,
    // Passes the URL id as a prop, and sets edit mode to true
    props: route => ({ id: route.params.id, isEditMode: true, isDebugMode: isDebugMode }),
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
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});
router.beforeEach((to, from) => {
  // Replace this with your actual auth checking logic (e.g., checking Pinia/Vuex or localStorage)
  const isAuthenticated = !!localStorage.getItem('token');

  if (to.meta.requiresAuth && !isAuthenticated) {
    // Instead of calling next({...}), you return the route object directly to redirect
    return {
      name: 'page-view',
      params: { id: to.params.id || '1' }
    };
  }

  // Explicitly returning nothing (or true) allows the navigation to proceed normally
  return true;
});

export default router;
