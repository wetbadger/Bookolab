// src/stores/pageStore.js
import { defineStore } from 'pinia';
import axios from 'axios';
// Access the environment variable
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

export const usePageStore = defineStore('pageStore', {
  state: () => ({
    records: [],
    previousPageLastWordId: null,
    loading: false,
    uploading: false,
    error: null // Track the error message
  }),
  actions: {
    async fetchPage(id) {
      this.loading = true;
      this.error = null; // Reset error on new attempt
      try {
        const response = await axios.get(`${API_BASE_URL}/pages/${id}`);
        this.records = response.data;

        const pageNumber = Number(id);
        if (pageNumber > 1) {
          try {
            const prevPageResponse = await axios.get(`${API_BASE_URL}/pages/${pageNumber - 1}`);
            // Extract the last word's ID from the preceding page if it exists
            this.previousPageLastWordId = prevPageResponse.data?.lastWord?.id || null;
          } catch (peekError) {
            console.warn("Could not fetch the preceding page's boundary word:", peekError);
            this.previousPageLastWordId = null;
          }
        }
      } catch (err) {
        // Capture the error message from the database or API failure
        this.error = err;
        console.error(err);
      } finally {
        this.loading = false;
      }
    },
    async addWord(content, currentPageId, previousWordId) {
      this.uploading = true;
      this.error = null;

      // 1. Only include what matches the Java @RequestBody Word object
      const requestBody = {
        content: content
      };

      // 2. Build out the query parameter config block for Axios
      const config = {
        params: {
          currentPageId: currentPageId,
          previousWordId: previousWordId // Axios appends this as ?previousWordId=value
        }
      };

      try {
        // Pass the URL, the body data, and then the configuration object
        await axios.post(
          `${API_BASE_URL}/words`,
          requestBody,
          config
        );
      } catch (err) {
        this.error = err;
        console.error(err);
      } finally {
        this.uploading = false;
      }
    }
  }
});
