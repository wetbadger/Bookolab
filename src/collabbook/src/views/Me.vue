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

        <!-- Other Options button -->
        <button @click="showOtherOptions = !showOtherOptions" class="btn btn-secondary">
          Other Options ▼
        </button>
      </div>

      <!-- Other Options Menu -->
      <div v-if="showOtherOptions" class="other-options-menu">
        <div class="option-item">
          <router-link to="/delete-account" class="option-link delete-option">
            🗑️ Delete Account
          </router-link>
        </div>
        <!-- Future options can be added here -->
        <!--
        <div class="option-item">
          <router-link to="/settings" class="option-link">
            ⚙️ Settings
          </router-link>
        </div>
        <div class="option-item">
          <router-link to="/export-data" class="option-link">
            📥 Export Data
          </router-link>
        </div>
        -->
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
const showOtherOptions = ref(false);

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

/* Other Options Menu Styles */
.other-options-menu {
  margin-top: 15px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background-color: #f8f9fa;
  overflow: hidden;
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.option-item {
  border-bottom: 1px solid #e0e0e0;
}

.option-item:last-child {
  border-bottom: none;
}

.option-link {
  display: block;
  padding: 12px 16px;
  color: #333;
  text-decoration: none;
  transition: background-color 0.2s ease;
  font-size: 14px;
}

.option-link:hover {
  background-color: #e9ecef;
}

.delete-option {
  color: #dc3545;
  font-weight: 500;
}

.delete-option:hover {
  background-color: #fff0f0;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s ease;
}

.btn-secondary:hover {
  background-color: #5a6268;
}

.profile-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
