/*
BUGS:
*/

// src/stores/pageStore.js
import { defineStore } from 'pinia';
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

export const usePageStore = defineStore('pageStore', {
  state: () => ({
    records: null, // Changed from [] to null since it holds an object structure
    loading: false,
    uploading: false,
    error: null
  }),
  actions: {
    async fetchPage(id) {
      this.loading = true;
      this.error = null;
      try {
        const response = await axios.get(`${API_BASE_URL}/pages/${id}`);
        this.records = response.data;
      } catch (err) {
        this.error = err;
        console.error("Failed to fetch page data:", err);
      } finally {
        this.loading = false;
      }
    },
    async addWord(content, currentPageId, previousWordId, nextWordId) {
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

        console.log(previousWordId);
        // Check if the inserted word is explicitly targeting the head of the current visible page
        const isFirstWordOnThisPage = !previousWordId || (nextWordId && Number(nextWordId) === Number(this.records.firstWord?.id));

        // --- CASE 1: Inserting at the very beginning of the current page ---
        if (isFirstWordOnThisPage) {
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

          while (current && Number(current.id) !== Number(previousWordId)) {
            current = current.nextWord;
          }

          if (current) {
            const isAppendedToEnd = (Number(previousWordId) === Number(this.records.lastWord?.id));

            if (isAppendedToEnd) {
              const nextPageWordId = this.records.lastWord?.nextWordId || null;
              const nextPageBridge = nextPageWordId ? { id: nextPageWordId, nextWord: null } : null;

              newWordBackend.nextWord = nextPageBridge;
              current.nextWord = newWordBackend;

              this.records.lastWord = {
                id: newWordBackend.id,
                content: newWordBackend.content,
                nextWordId: nextPageWordId,
                previousWordId: previousWordId
              };
            } else {
              // Standard Middle Insertion
              const localNextWord = current.nextWord;
              newWordBackend.nextWord = localNextWord;
              current.nextWord = newWordBackend;
            }
          } else {
            console.warn(`State desync: Could not find word ID ${previousWordId} in the current page chain.`);
          }
        }

        return newWordBackend;

      } catch (err) {
        this.error = err;
        console.error("Failed to add word:", err);
        throw err;
      } finally {
        this.uploading = false;
      }
    }
  }
});
