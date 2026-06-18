<template>
  <div class="profile-container">
    <a href="/" class="brand-link">bookolab.com</a>
    <hr class="divider" />

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      Fetching your author credentials...
    </div>

    <div v-else-if="authStore.error" class="error-alert">
      <span class="error-icon">⚠️</span>
      <div class="error-content">
        <h4>Authentication Error</h4>
        <p>{{ authStore.error }}</p>
        <router-link to="/login" class="btn btn-retry">Click here to re-login</router-link>
      </div>
    </div>

    <div v-else-if="authStore.user" class="profile-card">
      <div class="profile-header">
        <h3>Welcome back, {{ authStore.user.username }}!</h3>
      </div>

      <div class="profile-details">
        <div class="detail-item">
          <span class="detail-label">Username</span>
          <span class="detail-value">{{ authStore.user.username }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">Score</span>
          <span class="detail-value highlight">{{ authStore.user.score }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">Available Credits</span>
          <span class="detail-value success">{{ authStore.user.score - authStore.user.creditsSpent }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">Credits Spent</span>
          <span class="detail-value muted">{{ authStore.user.creditsSpent }}</span>
        </div>
      </div>

      <div class="profile-actions">
        <button @click="handleLogout" class="btn btn-danger">
          Log Out
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useAuthStore } from '@/stores/authStore';
import { useRouter } from 'vue-router';
import { usePageStore } from "@/stores/pageStore.js";

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

<style scoped>
@import "@/css/profile-styles.css";
</style>
