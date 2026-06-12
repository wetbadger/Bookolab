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
    stompClient: null,
    currentWebSocketSubscription: null,
    onRemoteWordAddedCallback: null
  }),
  actions: {
    initializeTestWebSocket() {
      if (this.stompClient) return;

      this.stompClient = new Client({
        brokerURL: WS_BASE_URL,
        reconnectDelay: 5000,
        debug: function (str) { console.log('STOMP: ' + str); },
      });

      this.stompClient.onConnect = (frame) => {
        console.log('🎉 Connected to Spring STOMP Broker!');
        // If we already loaded a page before the socket connected, subscribe to it immediately
        if (this.records?.id) {
          this.subscribeToPageTopic(this.records.id);
        }
      };

      this.stompClient.activate();
    },
    subscribeToPageTopic(pageId) {
      if (!this.stompClient || !this.stompClient.connected) return;

      if (this.currentSubscription) {
        this.currentSubscription.unsubscribe();
      }

      this.currentSubscription = this.stompClient.subscribe(`/topic/page/${pageId}`, (message) => {
        // Fix: Parse the message body into our inboundPayload variable
        const inboundPayload = JSON.parse(message.body);
        console.log("🔥 Incoming WebSocket payload caught:", inboundPayload);

        // Check if the payload is a cross-page boundary patch command
        if (inboundPayload.type) {
          this.handleCrossPageBoundaryPatch(inboundPayload);
        } else {
          // Otherwise, it's a standard word item for this page!
          this.insertWordIntoRecords(inboundPayload);
        }
      });
    },
    // Processes cross-page linking updates from neighboring tabs
    handleCrossPageBoundaryPatch(patch) {
      if (!this.records) return;

      if (patch.type === "NEXT_PAGE_HEAD_CHANGED") {
        console.log(`📡 Patching out-bound link pointer -> newNextWordId: ${patch.newNextWordId}`);
        // 1. Update the metadata link tracking pointer
        this.nextPageFirstWordId = patch.newNextWordId;
        
        // 2. Patch the actual active lastWord state instance object link directly
        if (this.records.lastWord) {
          this.records.lastWord.nextWordId = patch.newNextWordId;
        }
      } 
      
      else if (patch.type === "PREVIOUS_PAGE_TAIL_CHANGED") {
        console.log(`📡 Patching in-bound fallback anchor -> newLastWordIdOfPreviousPage: ${patch.newLastWordIdOfPreviousPage}`);
        // Update the fallback tracking pointer
        this.records.lastWordIdOfPreviousPage = patch.newLastWordIdOfPreviousPage;
      }
    },

    // Manipulate the linked list records directly inside Pinia
    insertWordIntoRecords(newWord) {
      if (!this.records) return;

      // 1. Conflict Resolution: Prevent duplicate insertions
      if (this.findWordInRecords(newWord.id || newWord.localId)) {
        console.log(`✅ Word "${newWord.content}" already accounted for in store.`);
        return;
      }

      // 2. Build the correct node structure matching Spring's entity
      const newNode = {
        id: newWord.id,
        localId: newWord.localId,
        content: newWord.content,
        nextWord: null
      };

      // 3. CASE A: List is completely empty
      if (!this.records.firstWord) {
        this.records.firstWord = newNode;
        this.records.lastWord = {
          id: newNode.id,
          content: newNode.content,
          nextWordId: this.nextPageFirstWordId || null,
          previousWordId: null
        };
        return;
      }

      // 4. CASE B: Prepend to the beginning of the page
      if (this.records.firstWord && newWord.nextWord && this.records.firstWord.id === newWord.nextWord.id) {
        newNode.nextWord = this.records.firstWord;
        this.records.firstWord = newNode;
        return;
      }

      // 5. CASE C: Appended to the very end of THIS page
      // Triggered if nextWord is completely null OR if it points to the start of the NEXT page
      const isPointingToNextPage = newWord.nextWord && 
                                   Number(newWord.nextWord.id) === Number(this.nextPageFirstWordId);
                                   
      if (!newWord.nextWord || isPointingToNextPage) {
        // Find the current tail node on this page by navigating the active memory pointers
        let tail = this.records.firstWord;
        while (tail.nextWord) {
          tail = tail.nextWord;
        }

        // Attach our new node to the end of the current page chain link
        tail.nextWord = newNode;

        // Synchronize the top-level page metadata boundaries safely
        this.records.lastWord = {
          id: newNode.id,
          content: newNode.content,
          nextWordId: this.nextPageFirstWordId || null,
          previousWordId: tail.id
        };
        return;
      }

      // 6. CASE D: Mid-chain Splicing (Strictly for items falling between existing words)
      let current = this.records.firstWord;
      while (current) {
        if (current.nextWord && current.nextWord.id === newWord.nextWord.id) {
          const oldNext = current.nextWord;
          current.nextWord = newNode;
          newNode.nextWord = oldNext;
          break;
        }
        current = current.nextWord;
      }
    },

    // Helper to scan our current memory tree for duplicates
    findWordInRecords(identifier) {
      let current = this.records?.firstWord;
      while (current) {
        if (current.id === identifier || current.localId === identifier) return current;
        current = current.nextWord;
      }
      return null;
    },
    // Sends a structured JSON payload
    sendWordViaWebSocket(payload) {
      if (this.stompClient && this.stompClient.connected) {
        this.stompClient.publish({
          destination: '/app/test-word',
          body: JSON.stringify(payload) // Map becomes standard JSON string
        });
        console.log("📡 Dispatched object to backend:", payload);
      } else {
        console.error("STOMP Client disconnected. Cannot send payload.");
      }
    },

    async fetchPage(id) {
      this.loading = true;
      this.error = null;
      try {
        const response = await axios.get(`${API_BASE_URL}/api/pages/${id}`);
        this.records = response.data;
        this.nextPageFirstWordId = this.records.lastWord?.nextWordId;

        // Every time you navigate to or load a fresh page, switch the WebSocket room!
        this.subscribeToPageTopic(id);

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
