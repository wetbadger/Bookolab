<script setup>
import { RouterView } from 'vue-router';
import { onMounted } from 'vue';
import { useAuthStore } from '@/stores/authStore';

const authStore = useAuthStore();

onMounted(async () => {
  // Fire the verification network call immediately when the app mounts
  await authStore.fetchCurrentUser();
});
</script>

<template>
  <main class="app-layout">
    <RouterView />
  </main>
</template>

<style scoped>
.app-layout {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  font-family: sans-serif;
}
</style>
