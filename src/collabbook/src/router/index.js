import { createRouter, createWebHistory } from 'vue-router';
import Page from '@/components/Page.vue';

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
    props: route => ({ id: route.params.id, isEditMode: false })
  },
  {
    path: '/pages/:id/edit',
    name: 'page-edit',
    component: Page,
    // Passes the URL id as a prop, and sets edit mode to true
    props: route => ({ id: route.params.id, isEditMode: true })
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;