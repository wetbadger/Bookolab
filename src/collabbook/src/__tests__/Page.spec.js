import { mount } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { createTestingPinia } from '@pinia/testing';
import { createRouter, createWebHistory } from 'vue-router';
import { nextTick } from 'vue';

import Page from '../views/Page.vue';
import { usePageStore } from '@/stores/pageStore';
import { useAuthStore } from '@/stores/authStore';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/pages/:id', component: { template: '<div>View</div>' } },
    { path: '/pages/:id/edit', component: { template: '<div>Edit</div>' } },
    { path: '/login', component: { template: '<div>Login</div>' } },
  ],
});

describe('Page.vue Component Tests', () => {
  let pinia;
  let pageStore;
  let authStore;

  beforeEach(async () => {
    router.push('/pages/1');
    await router.isReady();

    // 3. Setup the Testing Pinia instance with spy integration
    pinia = createTestingPinia({
      createSpy: vi.fn,
      initialState: {
        authStore: { isAuthenticated: false },
        pageStore: {
          records: {
            firstWord: { id: 101, content: 'Hello', nextWord: { id: 102, content: 'World', nextWord: null } },
            lastWordIdOfPreviousPage: null
          },
          loading: false,
          error: null,
          totalPages: 5,
          truncationEventTrigger: 0
        }
      }
    });

    pageStore = usePageStore();
    authStore = useAuthStore();
  });

  it('renders loading state when store is loading and there are no displayed words', async () => {
    pageStore.loading = true;
    pageStore.records = null;

    const wrapper = mount(Page, {
      props: { id: 1, isEditMode: false },
      global: { plugins: [pinia, router] }
    });

    expect(wrapper.text()).toContain('Loading database values...');
  });

  it('renders db error alert message if an error occurs in the store', async () => {
    pageStore.error = 'Database Connection Failed';

    const wrapper = mount(Page, {
      props: { id: 1, isEditMode: false },
      global: { plugins: [pinia, router] }
    });

    expect(wrapper.find('.error-alert').exists()).toBe(true);
    expect(wrapper.text()).toContain('Error: Database Connection Failed');
  });

  it('renders displayed words from the pageStore records', async () => {
    const wrapper = mount(Page, {
      props: { id: 1, isEditMode: false },
      global: { plugins: [pinia, router] }
    });

    // Await layout setup inside initializePage() / loadWords()
    await nextTick();
    await new Promise(resolve => setTimeout(resolve, 100));
    expect(wrapper.text()).toContain('Hello');
    expect(wrapper.text()).toContain('World');
  });

  it('hides edit buttons and does not display edit options if unauthorized', async () => {
    authStore.isAuthenticated = false;

    const wrapper = mount(Page, {
      props: { id: 1, isEditMode: false },
      global: { plugins: [pinia, router] }
    });

    await nextTick();

    // Edit page button is present but plus components shouldn't exist
    expect(wrapper.find('.btn').text()).toContain('Edit Page');
    expect(wrapper.findComponent({ name: 'Plus' }).exists()).toBe(false);
  });

  it('routes unauthorized users to login when clicking edit page button', async () => {
    authStore.isAuthenticated = false;
    const spyRouterPush = vi.spyOn(router, 'push');

    const wrapper = mount(Page, {
      props: { id: 1, isEditMode: false },
      global: { plugins: [pinia, router] }
    });

    await wrapper.find('button.btn').trigger('click');

    expect(spyRouterPush).toHaveBeenCalledWith({
      path: '/login',
      query: { redirectFrom: '/pages/1/edit' }
    });
  });

  it('allows authorized users to access edit paths on click', async () => {
    authStore.isAuthenticated = true;
    const spyRouterPush = vi.spyOn(router, 'push');

    const wrapper = mount(Page, {
      props: { id: 1, isEditMode: false },
      global: { plugins: [pinia, router] }
    });

    await wrapper.find('button.btn').trigger('click');

    expect(spyRouterPush).toHaveBeenCalledWith('/pages/1/edit');
  });
});
