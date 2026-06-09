<script setup>
import { RouterView } from 'vue-router';
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