<template>
  <div class="profile-container">
    <a href="/">bookolab.com</a>
    <hr />

    <div v-if="loading" class="loading-state">
      Fetching your author credentials...
    </div>

    <div v-else-if="authStore.error" class="error-alert">
      ⚠️ {{ authStore.error }}
      <p><router-link to="/login">Click here to re-login</router-link></p>
    </div>

    <div v-else-if="authStore.user" class="profile-card">
      <div class="profile-header">
        <h3>Welcome back, {{ authStore.user.username }}!</h3>
      </div>

      <div class="profile-details">
        <p><strong>Username:</strong> {{ authStore.user.username }}</p>
        <p><strong>Score:</strong> {{ authStore.user.score }}</p>
        <p><strong>Credits spent:</strong> {{ authStore.user.creditsSpent }}</p>
      </div>

      <button @click="handleLogout" class="btn btn-danger">
        Log Out
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useAuthStore } from '@/stores/authStore';
import { useRouter } from 'vue-router';
import {usePageStore} from "@/stores/pageStore.js";

const authStore = useAuthStore();
const pageStore = usePageStore();
const router = useRouter();
const loading = ref(true);

onMounted(async () => {
  await authStore.fetchCurrentUser();
  loading.value = false;
});

const handleLogout = () => {
  authStore.logout();
  pageStore.reconnectWebSocket();
  router.push('/');
};
</script>
