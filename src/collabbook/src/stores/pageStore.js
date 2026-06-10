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

      const requestBody = { content: content };
      const config = {
        params: {
          currentPageId: currentPageId,
          previousWordId: previousWordId
        }
      };

      try {
        const response = await axios.post(
          `${API_BASE_URL}/words`,
          requestBody,
          config
        );

        const newWordBackend = response.data;

        if (!this.records) return newWordBackend;

        // --- CASE 1: Inserting at the very beginning ---
        if (!previousWordId) {
          const oldFirstWord = this.records.firstWord;
          newWordBackend.nextWord = oldFirstWord;
          this.records.firstWord = newWordBackend;

          if (!this.records.lastWord) {
            this.records.lastWord = newWordBackend;
          }
        }

        // --- CASE 2 & 3: Inserting in the middle or at the end ---
        else {
          let current = this.records.firstWord;

          // Traverse to find the exact node in the main chain
          while (current && current.id !== previousWordId) {
            current = current.nextWord;
          }

          if (current) {
            // Check if we are appending to the very end of this page
            const isAppendedToEnd = (previousWordId === this.records.lastWord?.id);

            if (isAppendedToEnd) {
              // 1. CRITICAL FIX: Look at the existing metadata to find the next page bridge ID
              // Instead of reading current.nextWord (which is null), read from the store's metadata
              const nextPageWordId = this.records.lastWord?.nextWordId || null;

              // 2. Build the bridge object for the main chain if a next page exists
              const nextPageBridge = nextPageWordId ? { id: nextPageWordId, nextWord: null } : null;

              // 3. Attach the bridge to our new word
              newWordBackend.nextWord = nextPageBridge;

              // 4. Link the old last word to our new word in the main chain
              current.nextWord = newWordBackend;

              // 5. Update the lastWord tracking block with the preserved nextWordId!
              this.records.lastWord = {
                id: newWordBackend.id,
                content: newWordBackend.content,
                nextWordId: nextPageWordId, // Preserves 20001 seamlessly!
                previousWordId: previousWordId
              };
            } else {
              // Standard Middle Insertion:
              const localNextWord = current.nextWord;
              newWordBackend.nextWord = localNextWord;
              current.nextWord = newWordBackend;
            }
          }
        }

        return newWordBackend;

      } catch (err) {
        this.error = err;
        console.error(err);
        throw err;
      } finally {
        this.uploading = false;
      }
    }
  }
});
