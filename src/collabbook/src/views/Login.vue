<template>
  <div class="auth-container">
    <h2>Author Login</h2>

    <form @submit.prevent="handleLogin" class="auth-form">
      <div class="form-group">
        <label for="username">Username</label>
        <input
          v-model="form.username"
          type="text"
          id="username"
          required
          placeholder="Enter your username"
        />
      </div>

      <div class="form-group">
        <label for="password">Password</label>
        <input
          v-model="form.password"
          type="password"
          id="password"
          required
          placeholder="Enter your password"
        />
      </div>

      <div v-if="authStore.error" class="error-alert">
        ⚠️ {{ authStore.error }}
      </div>

      <button type="submit" :disabled="loading" class="btn btn-primary">
        {{ loading ? 'Logging in...' : 'Login' }}
      </button>
    </form>

    <p class="auth-switch">
      Don't have an account?
      <router-link :to="{ path: '/signup', query: route.query }">Sign up here</router-link>
    </p>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useAuthStore } from '@/stores/authStore';
import { useRouter, useRoute } from 'vue-router';
import {usePageStore} from "@/stores/pageStore.js";

const authStore = useAuthStore();
const pageStore = usePageStore();
const router = useRouter();
const route = useRoute();

const loading = ref(false);
const form = reactive({
  username: '',
  password: ''
});

const handleLogin = async () => {
  loading.value = true;
  const success = await authStore.login({ ...form });
  loading.value = false;

  if (success) {
    pageStore.reconnectWebSocket();
    // Check if the user was intercepted trying to edit a page
    const redirectTo = route.query.redirectFrom || '/';
    router.push(redirectTo);
  }
};
</script>

<style scoped>
@import '@/css/auth-styles.css';
</style>
