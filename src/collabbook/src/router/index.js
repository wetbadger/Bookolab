import { createRouter, createWebHistory } from 'vue-router';
import Page from '@/views/Page.vue';

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
    props: route => ({ id: route.params.id, isEditMode: true, isDebugMode: isDebugMode })
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;