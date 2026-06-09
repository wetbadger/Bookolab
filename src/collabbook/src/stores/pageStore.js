// src/stores/pageStore.js
import { defineStore } from 'pinia';
import axios from 'axios';

export const usePageStore = defineStore('pageStore', {
  state: () => ({
    records: [],
    loading: false,
    uploading: false,
    error: null // Track the error message
  }),
  actions: {
    async fetchPage(id) {
      this.loading = true;
      this.error = null; // Reset error on new attempt
      try {
        const response = await axios.get(`https://automatic-goggles-qp6qq677xjwf995r-8080.app.github.dev/api/pages/${id}`);
        this.records = response.data;
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
          "https://automatic-goggles-qp6qq677xjwf995r-8080.app.github.dev/api/words", 
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
