<!-- src/components/TheFooter.vue -->
<script setup>
  import { ref } from 'vue'
  const currentYear = ref(new Date().getFullYear())
</script>

<template>
  <footer class="app-footer">
    <div class="footer-content">
      <p>&copy; {{ currentYear }} </p>
      <div class="footer-links">
        <a href="https://example.com">Visit Our Github</a>
        <router-link to="/privacy">Privacy Policy</router-link>
        <router-link to="/terms">Terms of Service</router-link>
      </div>
    </div>
  </footer>
</template>

<style scoped>

  .footer-content {
    max-width: 1200px;
    margin: 0 auto;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 20px;
  }

  .footer-links a {
    margin-left: 15px;
    text-decoration: none;
  }

  .footer-links a:hover {
    text-decoration: underline;
  }
</style>

