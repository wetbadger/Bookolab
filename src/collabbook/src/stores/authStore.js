// src/stores/authStore.js
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { jwtDecode } from 'jwt-decode';
import api from '@/api/axios'; // Import your global axios instance

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token'));
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
      // Hits your Spring Boot @PostMapping("/login")
      const response = await api.post('/auth/login', credentials);
      // Your backend returns LoginResponse with { token, expiresIn }
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
      // Hits your Spring Boot @PostMapping("/signup")
      await api.post('/auth/signup', userDetails);
      return true;
    } catch (err) {
      error.value = err.response?.data?.message || 'Signup failed. Username may be taken.';
      return false;
    }
  }

  function setToken(newToken) {
    token.value = newToken;
    localStorage.setItem('token', newToken);
  }

  function logout() {
    token.value = null;
    localStorage.removeItem('token');
  }

  return { token, error, isAuthenticated, login, signup, logout };
});
