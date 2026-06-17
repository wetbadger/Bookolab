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
/* Container Layout */
.profile-container {
  max-width: 550px;
  margin: 2rem auto;
  padding: 2rem;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  color: #333;
}

.brand-link {
  color: #4f46e5;
  text-decoration: none;
  font-weight: 600;
  font-size: 1.1rem;
  transition: color 0.2s ease;
}

.brand-link:hover {
  color: #3730a3;
}

.divider {
  border: 0;
  height: 1px;
  background: #e5e7eb;
  margin: 1.5rem 0;
}

/* Loading State */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  padding: 3rem 0;
  color: #6b7280;
  font-size: 0.95rem;
}

.spinner {
  width: 30px;
  height: 30px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #4f46e5;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* Error State */
.error-alert {
  display: flex;
  gap: 1rem;
  background-color: #fef2f2;
  border: 1px solid #fee2e2;
  border-radius: 12px;
  padding: 1.5rem;
}

.error-icon {
  font-size: 1.5rem;
}

.error-content h4 {
  margin: 0 0 0.5rem 0;
  color: #991b1b;
}

.error-content p {
  margin: 0 0 1rem 0;
  color: #b91c1c;
  font-size: 0.9rem;
}

/* Profile Card */
.profile-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
  overflow: hidden;
}

.profile-header {
  background: #f8fafc;
  padding: 1.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.profile-header h3 {
  margin: 0;
  color: #1e293b;
  font-size: 1.25rem;
}

/* Details Grid */
.profile-details {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 0.75rem;
  border-bottom: 1px dashed #f1f5f9;
}

.detail-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.detail-label {
  color: #64748b;
  font-size: 0.9rem;
  font-weight: 500;
}

.detail-value {
  color: #0f172a;
  font-weight: 600;
}

.detail-value.highlight {
  color: #4f46e5;
}

.detail-value.success {
  color: #16a34a;
  background: #f0fdf4;
  padding: 0.2rem 0.6rem;
  border-radius: 6px;
  font-size: 0.9rem;
}

.detail-value.muted {
  color: #475569;
}

/* Buttons and Actions */
.profile-actions {
  padding: 0 1.5rem 1.5rem 1.5rem;
  display: flex;
  justify-content: flex-end;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.625rem 1.25rem;
  font-size: 0.9rem;
  font-weight: 500;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
  text-decoration: none;
}

.btn-danger {
  background-color: #ef4444;
  color: white;
}

.btn-danger:hover {
  background-color: #dc2626;
}

.btn-retry {
  background-color: #ffffff;
  color: #374151;
  border: 1px solid #d1d5db;
}

.btn-retry:hover {
  background-color: #f9fafb;
  border-color: #9ca3af;
}
</style>
