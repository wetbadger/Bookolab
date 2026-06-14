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

      <div v-if="authStore.error" class="error-alert">
        ⚠️ {{ authStore.error }}
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
import { useRouter, useRoute } from 'vue-router'; // 1. Added useRoute here

const authStore = useAuthStore();
const router = useRouter();
const route = useRoute(); // 2. Initialized route hook

const loading = ref(false);

const form = reactive({
  username: '',
  password: ''
});

const handleSignup = async () => {
  loading.value = true;
  const success = await authStore.signup({ ...form });
  loading.value = false;

  if (success) {
    // Automatically attempt log in
    const loginSuccess = await authStore.login({ ...form });
    if (loginSuccess) {
      // 3. Implemented the redirect functionality exactly like Login.vue
      const redirectTo = route.query.redirectFrom || '/';
      router.push(redirectTo);
    } else {
      // If auto-login fails, send them to login page but preserve the redirect parameter
      router.push({ path: '/login', query: route.query });
    }
  }
};
</script>

<style scoped>
@import '@/css/auth-styles.css';
</style>
