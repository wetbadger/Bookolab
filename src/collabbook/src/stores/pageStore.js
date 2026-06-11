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
    async addWord(content, currentPageId, localId, previousWordId, nextWordId, previousLocalId=null) {
      this.uploading = true;
      this.error = null;

      const requestBody = { content: content };
      const config = {
        params: {
          currentPageId: currentPageId,
          localId: localId,
          previousWordId: previousWordId,
          previousLocalId: previousLocalId
        }
      };

      console.log(requestBody);
      console.log(config);

      try {
        const response = await axios.post(
          `${API_BASE_URL}/words`,
          requestBody,
          config
        );

        const newWordBackend = response.data;
        if (!this.records) return newWordBackend;

        // Check if the inserted word is explicitly targeting the head of the current visible page
        const isFirstWordOnThisPage = !previousWordId || (nextWordId && Number(nextWordId) === Number(this.records.firstWord?.id));

        // --- CASE 1: Inserting at the very beginning of the current page ---
// --- CASE 1: Inserting at the very beginning of the current page ---
if (isFirstWordOnThisPage) {
  console.log("case 1");
  
  // Attach our tracking ID onto the backend object so we don't lose it
  newWordBackend.localId = localId;

  // 👇 FIX: Check if someone is already typing a recursive chain at the front.
  // If the current firstWord is a temporary local word, we shouldn't steal its crown!
  if (this.records.firstWord && this.records.firstWord.localId) {
    let current = this.records.firstWord;

    // Fast-forward to the end of the rapid-typing local chain
    while (current.nextWord && current.nextWord.localId) {
      current = current.nextWord;
    }

    // Splice the new word cleanly right after the last typed local word
    const localNextWord = current.nextWord;
    newWordBackend.nextWord = localNextWord;
    current.nextWord = newWordBackend;

    // If we somehow pushed all the way to the end of the page, update lastWord
    if (this.records.lastWord && Number(current.id) === Number(this.records.lastWord.id)) {
      this.records.lastWord = {
        id: newWordBackend.id,
        content: newWordBackend.content,
        nextWordId: this.records.lastWord.nextWordId,
        previousWordId: current.id
      };
    }
  } else {
    // Standard Head Insertion: Page is empty or we are genuinely pushing before a real DB word
    const oldFirstWord = this.records.firstWord;
    newWordBackend.nextWord = oldFirstWord;
    this.records.firstWord = newWordBackend;

    if (!this.records.lastWord) {
      this.records.lastWord = newWordBackend;
    }
  }
}
        // --- CASE 2 & 3: Inserting in the middle or at the end ---
        else {
          console.log("case 2");
          let current = this.records.firstWord;

          // 1. Find the initial anchor node specified by the props
          while (current) {
            const matchByDbId = previousWordId && Number(current.id) === Number(previousWordId);
            const matchByLocalId = previousLocalId && current.localId === previousLocalId;

            if (matchByDbId || matchByLocalId) {
              break; 
            }
            current = current.nextWord;
          }

          if (current) {
            // 2. 👇 CRITICAL TRAVERSAL OVERRIDE: 
            // If we matched a standard database word (like "The"), but the user is rapidly 
            // typing a recursive chain, "current.nextWord" will contain un-saved local words 
            // (words with a string-based localId). We must skip past them to find the true 
            // tail end of this rapid typing burst!
            while (current.nextWord && current.nextWord.localId) {
              current = current.nextWord;
            }

            // Attach our tracking ID onto the backend object so we don't lose it
            newWordBackend.localId = localId;

            // Evaluate if we are appending to the absolute end of the page
            const isAppendedToEnd = (Number(current.id) === Number(this.records.lastWord?.id));

            if (isAppendedToEnd) {
              const nextPageWordId = this.records.lastWord?.nextWordId || null;
              const nextPageBridge = nextPageWordId ? { id: nextPageWordId, nextWord: null } : null;

              newWordBackend.nextWord = nextPageBridge;
              current.nextWord = newWordBackend;

              this.records.lastWord = {
                id: newWordBackend.id,
                content: newWordBackend.content,
                nextWordId: nextPageWordId,
                previousWordId: current.id
              };
            } else {
              // Standard Middle Insertion (safely inserting right after the last typed local word)
              const localNextWord = current.nextWord;
              newWordBackend.nextWord = localNextWord;
              current.nextWord = newWordBackend;
            }
          } else {
            console.warn(`State desync: Could not find anchor word.`);
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
    },
    // A fast, non-crypto UUIDv4 look-alike generator for HTTP
    generateSimpleId() {
      return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
      });
    }
  }
});
