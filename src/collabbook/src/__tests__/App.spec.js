import { mount, RouterLinkStub } from '@vue/test-utils';
import { describe, it, expect, vi } from 'vitest';
import { createTestingPinia } from '@pinia/testing';
import App from '../App.vue';

describe('App', () => {
  it('mounts renders properly', () => {
    const wrapper = mount(App, {
      global: {
        plugins: [
          createTestingPinia({ createSpy: vi.fn }),
        ],
        // Mock the router components so they don't break
        stubs: {
          RouterView: true,
          RouterLink: RouterLinkStub
        }
      },
    });
    console.log("hi");
    console.log(wrapper.text());
    expect(wrapper.exists()).toBe(true);
  });
});
