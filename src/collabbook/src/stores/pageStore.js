// src/stores/pageStore.js
import { defineStore } from 'pinia';
import { Client } from '@stomp/stompjs';
import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;
// Convert http/https to ws/wss dynamically for your Codespace environment
const WS_BASE_URL = API_BASE_URL.replace(/^http/, 'ws') + '/gs-guide-websocket';

export const usePageStore = defineStore('pageStore', {
  state: () => ({
    records: null, // Changed from [] to null since it holds an object structure
    loading: false,
    uploading: false,
    error: null,
    nextPageFirstWordId: null,
    stompClient: null
  }),
  actions: {
    initializeTestWebSocket() {
      if (this.stompClient) return; // Prevent double initialization

      this.stompClient = new Client({
        brokerURL: WS_BASE_URL,
        reconnectDelay: 5000,
        debug: function (str) {
          console.log('STOMP Debug Log: ' + str);
        },
      });

      this.stompClient.onConnect = (frame) => {
        console.log('🎉 Connected to Spring STOMP Broker!');
        
        // 3. Immediately subscribe to our test topic
        this.stompClient.subscribe('/topic/test-greetings', (message) => {
          console.log('🔥 REAL-TIME BROADCAST RECEIVED FROM SERVER:', message.body);
          alert(`Real-Time Update: ${message.body}`);
        });
      };

      this.stompClient.activate();
    },
    sendWordViaWebSocket(content) {
      if (this.stompClient && this.stompClient.connected) {
        this.stompClient.publish({
          destination: '/app/test-word',
          body: content // Sending the raw string stringified or plain text
        });
        console.log(`📡 Sent "${content}" out over WebSocket pipeline.`);
      } else {
        console.error("Cannot send message. STOMP client is not connected.");
      }
    },
    async fetchPage(id) {
      this.loading = true;
      this.error = null;
      try {
        const response = await axios.get(`${API_BASE_URL}/api/pages/${id}`);
        this.records = response.data;
        this.nextPageFirstWordId = this.records.lastWord?.nextWordId;
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

      try {
        const response = await axios.post(
          `${API_BASE_URL}/api/words`,
          requestBody,
          config
        );

        const newWordBackend = response.data;
        if (!this.records) return newWordBackend;

        // Attach tracking ID
        newWordBackend.localId = localId;

        // Convert the linked list to an array for easier manipulation
        const words = this.linkedListToArray();

        // Find insertion index
        let insertIndex = 0; // default to beginning

        if (previousWordId) {
          const prevIndex = words.findIndex(w => w.id === previousWordId);
          if (prevIndex !== -1) insertIndex = prevIndex + 1;
        } else if (previousLocalId) {
          const prevIndex = words.findIndex(w => w.localId === previousLocalId);
          if (prevIndex !== -1) insertIndex = prevIndex + 1;
        }

        // Insert into array
        words.splice(insertIndex, 0, newWordBackend);

        // Rebuild linked list from array
        this.rebuildLinkedList(words);

        return newWordBackend;

      } catch (err) {
        this.error = err;
        console.error("Failed to add word:", err);
        throw err;
      } finally {
        this.uploading = false;
      }
    },

// Helper: Convert linked list to array
    linkedListToArray() {
      const result = [];
      let current = this.records.firstWord;
      while (current) {
        result.push(current);
        current = current.nextWord;
      }
      return result;
    },

// Helper: Rebuild linked list from array
// Helper: Rebuild linked list from array
    rebuildLinkedList(words) {
      if (words.length === 0) {
        this.records.firstWord = null;
        this.records.lastWord = null;
        return;
      }

      // Rebuild the linked list pointers and metadata for all words except the last
      for (let i = 0; i < words.length - 1; i++) {
        const current = words[i];
        const next = words[i + 1];
        const prev = words[i - 1] || null;

        // Update the word object with correct pointers
        current.nextWord = next;
        // We don't need these properties:
        // current.nextWordId = next.id;
        // current.previousWordId = prev ? prev.id : null;
      }

      // Handle the last word separately
      const lastWord = words[words.length - 1];
      const secondToLastWord = words[words.length - 2] || null;
      const secondToLastWordId = secondToLastWord ? secondToLastWord.id : null;

      // Set the last word's pointers
      lastWord.nextWord = null;
      // lastWord.nextWordId = this.nextPageFirstWordId || null; // Preserve cross-page link
      // lastWord.previousWordId = secondToLastWordId;

      this.records.firstWord = words[0];
      this.records.lastWord = {
        id: lastWord.id,
        content: lastWord.content,
        nextWordId: this.nextPageFirstWordId || null,
        previousWordId: secondToLastWordId
      };
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
