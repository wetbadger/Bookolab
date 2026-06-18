// src/stores/authStore.js
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { jwtDecode } from 'jwt-decode';
import api from '@/api/axios';

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token'));
  const user = ref(null);
  const error = ref(null);

  // Initialize to true if a token exists (we need to verify it), otherwise false
  const isAuthLoading = ref(!!localStorage.getItem('token'));

  const isAuthenticated = computed(() => {
    if (!token.value || token.value === 'null' || token.value === 'undefined') return false;
    try {
      const decoded = jwtDecode(token.value);
      return decoded.exp > Date.now() / 1000;
    } catch (e) {
      return false;
    }
  });

  async function login(credentials) {
    error.value = null;
    try {
      const response = await api.post('/auth/login', credentials);
      setToken(response.data.token);

      return true;
    } catch (err) {
      error.value = 'Login failed. Check your credentials.';
      return false;
    }
  }

  async function signup(userDetails) {
    error.value = null;
    try {
      await api.post('/auth/signup', userDetails);
      return true;
    } catch (err) {
      error.value = err.response?.data?.message || 'Signup failed. Username may be taken.';
      return false;
    }
  }

  async function fetchCurrentUser() {
    if (!isAuthenticated.value) {
      isAuthLoading.value = false; // Safeguard: stop loading if token is invalid/missing
      return null;
    }

    isAuthLoading.value = true; // Ensure loading state turns on when fetching
    try {
      const response = await api.get('/api/authors/me');
      user.value = response.data;
      return user.value;
    } catch (err) {
      error.value = err.response?.data?.message || 'Failed to fetch author profile.';
      logout();
      return null;
    } finally {
      isAuthLoading.value = false; // Always turn off loading whether it succeeds or fails
    }
  }

  async function fetchPublicProfileByUsername(username) {
    try {
      const response = await api.get(`api/authors/profile/${username}`);
      return response.data;
    } catch(err) {
      error.value = err.response?.data?.message || 'Failed to fetch author profile.';
      return null;
    }
  }

  function getCredits() {
    if (user.value) {
      return (user.value.score - user.value.creditsSpent);
    } else {
      return 0;
    }
  }

  function setToken(newToken) {
    token.value = newToken;
    localStorage.setItem('token', newToken);
  }

  function logout() {
    token.value = null;
    user.value = null;
    isAuthLoading.value = false; // 🚀 Reset on logout
    localStorage.removeItem('token');
  }

  // Remember to expose 'isAuthLoading' to components
  return {
    token,
    user,
    error,
    isAuthenticated,
    isAuthLoading,
    login,
    signup,
    fetchCurrentUser,
    logout,
    getCredits,
    fetchPublicProfileByUsername
  };
});
