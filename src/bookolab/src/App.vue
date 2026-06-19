<script setup>
import { RouterView } from 'vue-router';
import { onMounted } from 'vue';
import { useAuthStore } from '@/stores/authStore';
import Footer from "@/components/Footer.vue";

const authStore = useAuthStore();

onMounted(async () => {
  // Fire the verification network call immediately when the app mounts
  await authStore.fetchCurrentUser();
});
</script>

<template>
  <div class="app-container">
    <main class="app-layout">
      <RouterView />
    </main>
    <Footer />
  </div>
</template>

<style scoped>
/* 1. Establish a full-height flex column */
.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh; /* Takes up at least the full viewport height */
}

/* 2. Tell the main content to grow and take up all available space */
.app-layout {
  flex: 1;
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  font-family: sans-serif;
}

/* 3. Optional: Ensure the footer doesn't shrink */
footer {
  flex-shrink: 0;
}
</style>
