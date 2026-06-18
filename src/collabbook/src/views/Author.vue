<template>
  <div class="profile-container">
    <a href="/" class="brand-link">bookolab.com</a>
    <hr class="divider" />

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      Fetching author details...
    </div>

    <div v-else-if="error" class="error-alert">
      <span class="error-icon">🔍</span>
      <div class="error-content">
        <h4>Author Not Found</h4>
        <p>{{ error }}</p>
        <router-link to="/" class="btn btn-retry">Back to Home</router-link>
      </div>
    </div>

    <div v-else-if="author" class="profile-card">
      <div class="profile-header">
        <h3>Author Profile</h3>
      </div>

      <div class="profile-details">
        <div class="detail-item">
          <span class="detail-label">Username</span>
          <span class="detail-value">{{ author.username }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">Score</span>
          <span class="detail-value highlight">{{ author.score }}</span>
        </div>
      </div>

      <div class="profile-actions">
        <router-link to="/" class="btn btn-secondary">
          Back
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import {useAuthStore} from "@/stores/authStore.js";

// 1. Declare your prop from the router configuration
const props = defineProps({
  username: {
    type: String,
    required: true
  }
});

const authStore = useAuthStore();

const author = ref(null);
const loading = ref(true);
const error = ref(null);

const fetchProfileData = async (name) => {
  loading.value = true;
  error.value = null;
  try {
    // Call your API helper here using 'name'
    author.value = await authStore.fetchPublicProfileByUsername(props.username);
  } catch (err) {
    error.value = err.message || "Could not retrieve profile.";
  } finally {
    loading.value = false;
  }
};

// 2. Fetch data when component initially loads
onMounted(() => {
  fetchProfileData(props.username);
});

// 3. Keep an eye on the prop in case the user hops directly to another author
watch(() => props.username, (newUsername) => {
  fetchProfileData(newUsername);
});
</script>

<style scoped>
/* Import shared structural layout & color palettes */
@import "@/css/profile-styles.css";

/* Component-specific overrides or additions */
.error-alert {
  display: flex;
  gap: 1rem;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 1.5rem;
}

.error-content h4 {
  margin: 0 0 0.5rem 0;
  color: #334155;
}

.error-content p {
  margin: 0 0 1rem 0;
  color: #64748b;
  font-size: 0.9rem;
}
</style>
