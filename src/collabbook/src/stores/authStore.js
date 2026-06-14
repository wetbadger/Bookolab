// src/stores/authStore.js
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { jwtDecode } from 'jwt-decode';
import api from '@/api/axios'; // Import your global axios instance

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token'));
  const user = ref(null); // 👈 Added state to hold the backend Author model
  const error = ref(null);

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
      error.value = err.response?.data?.message || 'Login failed. Check your credentials.';
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

  // Fetch the /authors/me endpoint using our configured api instance
  async function fetchCurrentUser() {
    if (!isAuthenticated.value) return null;
    try {
      // Your api instance handles the interceptor / headers automatically!
      const response = await api.get('/authors/me');
      user.value = response.data;
      return user.value;
    } catch (err) {
      error.value = err.response?.data?.message || 'Failed to fetch user profile.';
      // If the token expired or is malformed, clean up
      logout();
      return null;
    }
  }

  function setToken(newToken) {
    token.value = newToken;
    localStorage.setItem('token', newToken);
  }

  function logout() {
    token.value = null;
    user.value = null; // 👈 Clean up user data on logout
    localStorage.removeItem('token');
  }

  // Expose user and fetchCurrentUser to components
  return { token, user, error, isAuthenticated, login, signup, fetchCurrentUser, logout };
});
