import { defineStore } from 'pinia';
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

export const useLeaderboardStore = defineStore('leaderboard', {
  state: () => ({
    users: [],
    isLoading: false,
    error: null,
    lastUpdated: null,
  }),

  getters: {
    // Returns the top 10 users sorted by score (highest first)
    topUsers: (state) => {
      return [...state.users]
        .sort((a, b) => (b.score || 0) - (a.score || 0))
        .slice(0, 10);
    },

    hasUsers: (state) => state.users.length > 0,
    totalUsers: (state) => state.users.length,
  },

  actions: {
    /**
     * Fetch leaderboard data from the API
     */
    async fetchLeaderboard() {
      if (this.isLoading) return;

      this.isLoading = true;
      this.error = null;

      try {
        const response = await axios.get(`${API_BASE_URL}/api/leaderboard`, {
          timeout: 10000,
          headers: {
            'Content-Type': 'application/json',
          },
        });

        let userData = [];
        if (Array.isArray(response.data)) {
          userData = response.data;
        } else if (response.data && Array.isArray(response.data.users)) {
          userData = response.data.users;
        } else if (response.data && Array.isArray(response.data.data)) {
          userData = response.data.data;
        } else {
          throw new Error('Unexpected API response format');
        }

        this.users = userData.map((user) => ({
          id: user.id || user._id || `user-${Math.random()}`,
          name: user.name || user.username || 'Anonymous',
          score: typeof user.score === 'number' ? user.score : parseInt(user.score) || 0,
          avatar: user.avatar || null,
          email: user.email || null,
          ...user,
        }));

        this.lastUpdated = new Date().toISOString();
        this.error = null;

      } catch (error) {
        console.error('Failed to fetch leaderboard:', error);

        if (error.code === 'ECONNABORTED') {
          this.error = 'Request timed out. Please check your connection.';
        } else if (error.response) {
          const status = error.response.status;
          if (status === 401) this.error = 'Unauthorized. Please log in again.';
          else if (status === 403) this.error = 'Access denied. You don\'t have permission.';
          else if (status === 404) this.error = 'Leaderboard endpoint not found.';
          else if (status >= 500) this.error = 'Server error. Please try again later.';
          else this.error = error.response.data?.message || 'Failed to load leaderboard.';
        } else if (error.request) {
          this.error = 'No response from server. Please check your network.';
        } else {
          this.error = error.message || 'An unexpected error occurred.';
        }
      } finally {
        this.isLoading = false;
      }
    },

    clearLeaderboard() {
      this.users = [];
      this.error = null;
      this.lastUpdated = null;
    },

    updateUserScore(userId, newScore) {
      const index = this.users.findIndex((user) => user.id === userId);
      if (index !== -1) {
        this.users[index].score = newScore;
        this.lastUpdated = new Date().toISOString();
      }
    },

    async refresh() {
      await this.fetchLeaderboard();
    },
  },
});
