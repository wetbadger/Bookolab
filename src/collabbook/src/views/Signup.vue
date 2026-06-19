<template>
  <div class="auth-container">
    <h2>Create Author Account</h2>

    <form @submit.prevent="handleSignup" class="auth-form">
      <div class="form-group">
        <label for="username">Username</label>
        <input
          v-model="form.username"
          type="text"
          id="username"
          required
          placeholder="Choose a username"
        />
      </div>

      <div class="form-group">
        <label for="password">Password</label>
        <input
          v-model="form.password"
          type="password"
          id="password"
          required
          placeholder="Create a password"
        />
      </div>

      <div class="form-group">
        <label for="confirm-password">Confirm Password</label>
        <input
          v-model="form.confirmPassword"
          type="password"
          id="confirm-password"
          required
          placeholder="Confirm your password"
        />
      </div>

      <div v-if="localError || authStore.error" class="error-alert">
        ⚠️ {{ localError || authStore.error }}
      </div>

      <button type="submit" :disabled="loading" class="btn btn-primary">
        {{ loading ? 'Creating account...' : 'Sign Up' }}
      </button>
    </form>

    <p class="auth-switch">
      Already have an account?
      <router-link :to="{ path: '/login', query: route.query }">Login here</router-link>
    </p>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useAuthStore } from '@/stores/authStore';
import { useRouter, useRoute } from 'vue-router';
import { usePageStore } from "@/stores/pageStore.js";

const authStore = useAuthStore();
const pageStore = usePageStore();
const router = useRouter();
const route = useRoute();

const loading = ref(false);
const localError = ref(''); // NEW: Holds front-end validation errors

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '' // NEW: Track the confirmation field locally
});

const handleSignup = async () => {
  // NEW: Validate match before hitting the store or backend
  if (form.password !== form.confirmPassword) {
    localError.value = "Passwords do not match.";
    return;
  }

  localError.value = ''; // Clear previous error if resolved
  loading.value = true;

  // NEW: Deconstruct to extract confirmPassword, leaving cleanCredentials behind
  const { confirmPassword, ...cleanCredentials } = form;

  // Pass only cleanCredentials (username, password) to the API operations
  const success = await authStore.signup(cleanCredentials);

  if (success) {
    // Automatically attempt log in with the clean credentials
    const loginSuccess = await authStore.login(cleanCredentials);
    loading.value = false; // Turn off loading after everything finishes

    if (loginSuccess) {
      pageStore.reconnectWebSocket();
      const redirectTo = route.query.redirectFrom || '/';
      router.push(redirectTo);
    } else {
      router.push({ path: '/login', query: route.query });
    }
  } else {
    loading.value = false; // Turn off loading if signup fails
  }
};
</script>

<style scoped>
@import '@/css/auth-styles.css';
</style>
