import axios from 'axios';
import { useAuthStore } from '@/stores/authStore';
import router from '@/router';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;
const api = axios.create({
  baseURL: `${API_BASE_URL}` // Your Spring Boot backend URL base
});

// REQUEST INTERCEPTOR: Automatically attach the JWT token to every request if it exists
api.interceptors.request.use((config) => {
  const authStore = useAuthStore();

  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`;
  }
  return config;
}, (error) => {
  return Promise.reject(error);
});

// RESPONSE INTERCEPTOR: Catch 401s from Spring Boot and boot the user out
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // If Spring Boot says 401, the frontend token is faked, tampered with, or expired
    if (error.response && error.response.status === 401) {
      console.warn("Backend rejected token authentication. Logging out...");

      const authStore = useAuthStore();
      authStore.logout(); // Wipes token from state and localStorage

      // Redirect to login page
      router.push('/login');
    }
    return Promise.reject(error);
  }
);

export default api;
