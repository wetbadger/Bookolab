<!-- src/components/UserReactionStats.vue -->
<template>
  <div v-if="isAuthenticated" class="user-reaction-stats">
    <span class="like-stats">
      👍 <span class="count">{{ pageStore.userReactionCounts.likesReceived || 0 }}</span>
    </span>
    <span class="dislike-stats">
      👎 <span class="count">{{ pageStore.userReactionCounts.dislikesReceived || 0 }}</span>
    </span>
    <span class="credits-stats">
      🪙 <span class="count">{{ credits || 0 }}</span>
    </span>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { usePageStore } from '@/stores/pageStore';
import { useAuthStore } from '@/stores/authStore';

const pageStore = usePageStore();
const authStore = useAuthStore();
const credits = computed(() => {
  return pageStore.getCredits();
});

const isAuthenticated = computed(() => authStore.isAuthenticated);
</script>

<style scoped>
.user-reaction-stats {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 4px 12px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  font-size: 0.9rem;
}

.stats-label {
  color: #666;
  font-size: 0.8rem;
  margin-right: 4px;
}

.like-stats, .dislike-stats {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.count {
  font-weight: bold;
  min-width: 16px;
  display: inline-block;
  text-align: center;
}

.like-stats .count {
  color: #28a745;
}

.dislike-stats .count {
  color: #dc3545;
}
</style>
