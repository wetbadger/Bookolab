import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { jwtDecode } from 'jwt-decode'; // Make sure to: npm install jwt-decode

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token'));

  // Computed state: Dynamically validates if the token exists and is unexpired
  const isAuthenticated = computed(() => {
    if (!token.value || token.value === 'null' || token.value === 'undefined') {
      return false;
    }

    try {
      const decoded = jwtDecode(token.value);
      const currentTime = Date.now() / 1000;

      if (decoded.exp < currentTime) {
        logout(); // Auto-cleanup if expired
        return false;
      }
      return true;
    } catch (e) {
      logout(); // Auto-cleanup if corrupted
      return false;
    }
  });

  // Action to run when user successfully logs in
  function setToken(newToken) {
    token.value = newToken;
    localStorage.setItem('token', newToken);
  }

  // Action to clear everything on logout or backend rejection
  function logout() {
    token.value = null;
    localStorage.removeItem('token');
  }

  return {
    token,
    isAuthenticated,
    setToken,
    logout
  };
});
