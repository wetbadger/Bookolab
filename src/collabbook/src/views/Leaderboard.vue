<template>
  <Home />
  <div class="leaderboard-container">
    <h1 class="leaderboard-title">🏆 Leaderboard</h1>

    <!-- Loading State -->
    <div v-if="store.isLoading" class="status-message loading">
      <span class="spinner"></span>
      Loading leaderboard...
    </div>

    <!-- Error State -->
    <div v-else-if="store.error" class="status-message error">
      ⚠️ {{ store.error }}
      <button @click="store.fetchLeaderboard" class="retry-btn">
        Retry
      </button>
    </div>

    <!-- Empty State -->
    <div v-else-if="!store.topUsers.length" class="status-message empty">
      🕊️ No users yet. Be the first to score!
    </div>

    <!-- Leaderboard Table -->
    <div v-else class="leaderboard-table-wrapper">
      <table class="leaderboard-table">
        <thead>
        <tr>
          <th>Rank</th>
          <th>User</th>
          <th class="score-column">Score</th>
        </tr>
        </thead>
        <tbody>
        <tr
          v-for="(user, index) in store.topUsers"
          :key="user.id || index"
          :class="{
              'gold-row': index === 0,
              'silver-row': index === 1,
              'bronze-row': index === 2,
            }"
        >
          <td class="rank-cell">
              <span class="rank-badge" :class="{
                'gold-badge': index === 0,
                'silver-badge': index === 1,
                'bronze-badge': index === 2,
              }">
                {{ index + 1 }}
              </span>
          </td>
          <td class="user-cell">
            <div class="user-info">
              <div class="avatar-placeholder">
                {{ user.name ? user.name.charAt(0).toUpperCase() : '?' }}
              </div>
              <span class="user-name">{{ user.name || 'Anonymous' }}</span>
            </div>
          </td>
          <td class="score-cell">
            <span class="score-value">{{ formatScore(user.score) }}</span>
          </td>
        </tr>
        </tbody>
      </table>

      <!-- Last updated timestamp -->
      <div class="footer-meta">
        <span>Showing top {{ store.topUsers.length }} players</span>
        <span v-if="store.lastUpdated" class="update-time">
          Updated: {{ formatTime(store.lastUpdated) }}
        </span>
        <button @click="store.fetchLeaderboard" class="refresh-btn" title="Refresh">
          🔄
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useLeaderboardStore } from '@/stores/leaderboardStore'; // adjust import path as needed
import Home from '@/components/Home.vue';
// --- Store ---
const store = useLeaderboardStore();

// --- Lifecycle ---
onMounted(() => {
  // Fetch leaderboard when component mounts
  store.fetchLeaderboard();
});

// --- Helpers ---
function formatScore(score) {
  if (score === undefined || score === null) return '0';
  return Number(score).toLocaleString();
}

function formatTime(timestamp) {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
}
</script>

<style scoped>
/* --- Container --- */
.leaderboard-container {
  max-width: 720px;
  margin: 2rem auto;
  padding: 1.5rem 1.5rem 1rem;
  background: #f9fafc;
  border-radius: 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.leaderboard-title {
  font-size: 2rem;
  font-weight: 700;
  color: #1e293b;
  margin-top: 0;
  margin-bottom: 1.5rem;
  text-align: center;
  letter-spacing: -0.5px;
}

/* --- Status messages --- */
.status-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  text-align: center;
  border-radius: 16px;
  background: #ffffff;
  color: #475569;
  font-size: 1.1rem;
}

.status-message.loading {
  background: #f1f5f9;
  gap: 0.75rem;
}

.spinner {
  display: inline-block;
  width: 32px;
  height: 32px;
  border: 4px solid #e2e8f0;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.status-message.error {
  color: #b91c1c;
  background: #fee2e2;
  gap: 1rem;
}

.retry-btn {
  background: #dc2626;
  color: white;
  border: none;
  padding: 0.5rem 1.5rem;
  border-radius: 40px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.retry-btn:hover {
  background: #b91c1c;
}

.status-message.empty {
  background: #f1f5f9;
  color: #64748b;
}

/* --- Table --- */
.leaderboard-table-wrapper {
  background: #ffffff;
  border-radius: 16px;
  padding: 0.25rem 0.25rem 0.5rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  overflow-x: auto;
}

.leaderboard-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 1rem;
}

.leaderboard-table thead th {
  text-align: left;
  padding: 1rem 0.75rem 0.75rem 0.75rem;
  font-weight: 600;
  color: #64748b;
  border-bottom: 2px solid #e9edf2;
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.leaderboard-table tbody tr {
  transition: background 0.15s;
  border-bottom: 1px solid #f1f4f9;
}

.leaderboard-table tbody tr:last-child {
  border-bottom: none;
}

.leaderboard-table tbody tr:hover {
  background: #f8fafc;
}

/* --- Rank column --- */
.rank-cell {
  width: 70px;
  padding: 0.75rem 0.5rem;
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 40px;
  background: #e9edf2;
  color: #334155;
  font-weight: 700;
  font-size: 0.9rem;
}

.gold-badge {
  background: #fbbf24;
  color: #78350f;
  box-shadow: 0 2px 8px rgba(251, 191, 36, 0.3);
}

.silver-badge {
  background: #d1d5db;
  color: #1f2937;
  box-shadow: 0 2px 8px rgba(209, 213, 219, 0.3);
}

.bronze-badge {
  background: #f59e0b;
  color: #7b2e00;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.25);
}

/* --- User column --- */
.user-cell {
  padding: 0.75rem 0.5rem;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 40px;
  background: #dbeafe;
  color: #1e4b8a;
  font-weight: 700;
  font-size: 1rem;
  flex-shrink: 0;
  text-transform: uppercase;
}

.user-name {
  font-weight: 500;
  color: #0f172a;
}

/* --- Score column --- */
.score-column {
  text-align: right;
}

.score-cell {
  text-align: right;
  padding: 0.75rem 0.5rem;
  font-weight: 600;
  color: #0f172a;
}

.score-value {
  background: #f1f5f9;
  padding: 0.25rem 0.75rem;
  border-radius: 40px;
  font-size: 0.95rem;
  display: inline-block;
  min-width: 60px;
}

/* --- Row highlights for top 3 --- */
.gold-row .score-value {
  background: #fef3c7;
  color: #92400e;
}

.silver-row .score-value {
  background: #f1f3f5;
  color: #1f2937;
}

.bronze-row .score-value {
  background: #ffedd5;
  color: #9a3412;
}

/* --- Footer --- */
.footer-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 0.75rem;
  padding: 0.25rem 0.5rem 0.25rem 0.5rem;
  font-size: 0.8rem;
  color: #94a3b8;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.update-time {
  color: #64748b;
}

.refresh-btn {
  background: transparent;
  border: none;
  font-size: 1.2rem;
  cursor: pointer;
  padding: 0.2rem 0.5rem;
  border-radius: 30px;
  transition: background 0.2s, transform 0.15s;
}

.refresh-btn:hover {
  background: #e9edf2;
  transform: rotate(30deg);
}

/* --- Responsive --- */
@media (max-width: 500px) {
  .leaderboard-container {
    margin: 1rem;
    padding: 1rem 0.75rem;
  }

  .leaderboard-title {
    font-size: 1.6rem;
  }

  .rank-cell {
    width: 50px;
  }

  .rank-badge {
    width: 28px;
    height: 28px;
    font-size: 0.75rem;
  }

  .avatar-placeholder {
    width: 30px;
    height: 30px;
    font-size: 0.8rem;
  }

  .user-name {
    font-size: 0.9rem;
  }

  .score-value {
    font-size: 0.8rem;
    padding: 0.15rem 0.5rem;
    min-width: 40px;
  }

  .footer-meta {
    font-size: 0.7rem;
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
