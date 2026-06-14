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
      Already have an account? <router-link to="/login">Login here</router-link>
    </p>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useAuthStore } from '@/stores/authStore';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const router = useRouter();
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
    // Automatically attempt log in or push them to login page with a success signal
    const loginSuccess = await authStore.login({ ...form });
    if (loginSuccess) {
      router.push('/');
    } else {
      router.push('/login');
    }
  }
};
</script>

<style scoped>
@import '@/css/auth-styles.css';
</style>
