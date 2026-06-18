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
    currentReactionsSubscription: null,
    onRemoteWordAddedCallback: null,
    totalPages: 1,
    truncationEventTrigger: 0,
    globalUpdatesSubscription: null,
    globalCounter: 0 // TODO: use for id generation
  }),
  actions: {
    initializeTestWebSocket() {
      if (this.stompClient) return;

      // Assuming your JWT is saved in localStorage or another store
      const token = localStorage.getItem('token');

      this.stompClient = new Client({
        brokerURL: WS_BASE_URL,
        reconnectDelay: 5000,
        // Add the JWT token to the connection headers
        connectHeaders: {
          Authorization: token ? `Bearer ${token}` : ''
        },
        debug: function (str) { console.log('STOMP: ' + str); },
      });

      this.stompClient.onConnect = (frame) => {
        console.log('🎉 Connected to Spring STOMP Broker!');
        const serverUser = frame.headers['user-name'];
        if (serverUser === 'anonymousUser') {
          // Clear your frontend storage so it stops sending the bad token on refresh
          localStorage.removeItem('token');
        }
        if (this.records?.id) {
          this.subscribeToPageTopic(this.records.id);
        }
        // 🚀 SUBSCRIBE TO GLOBAL STRUCTURAL UPDATES
        if (!this.globalUpdatesSubscription) {
          this.globalUpdatesSubscription = this.stompClient.subscribe('/topic/global-updates', (message) => {
            const payload = JSON.parse(message.body);
            console.log("🌍 Global update received:", payload);

            if (payload.type === "GLOBAL_REPAGINATION") {
              this.totalPages = payload.totalPages;
              this.truncationEventTrigger++; // Bump counter to alert watching components
            }
          });
        }

      };

      this.stompClient.activate();
    },
    // Safe disconnection action
    disconnectWebSocket() {
      if (this.stompClient) {
        if (this.currentSubscription) this.currentSubscription.unsubscribe();
        if (this.currentReactionsSubscription) this.currentReactionsSubscription.unsubscribe();
        if (this.globalUpdatesSubscription) { // 👈 Clean up
          this.globalUpdatesSubscription.unsubscribe();
          this.globalUpdatesSubscription = null;
        }
        this.stompClient.deactivate();
        this.stompClient = null;
      }
    },

// 🚀 ADD THIS: Forced reconnect action to break out of the early return block
    reconnectWebSocket() {
      console.log('🔄 Forcing WebSocket reconnection with fresh credentials...');
      this.disconnectWebSocket(); // Kill the unauthenticated connection
      this.initializeTestWebSocket(); // Fire up a completely pristine authenticated one
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
        if (inboundPayload.type === "PREVIOUS_PAGE_TAIL_CHANGED" ||
            inboundPayload.type === "NEXT_PAGE_HEAD_CHANGED") {
          this.handleCrossPageBoundaryPatch(inboundPayload);
        } else {
          // Otherwise, it's a standard word item for this page!
          if (inboundPayload.type === "CREATE_WORD")
            this.insertWordIntoRecords(inboundPayload.word);
          else if (inboundPayload.type === "DELETE_WORD")
            this.deleteWordFromRecords(inboundPayload);
        }
      });

      // --- CHANNEL 2: Live Reaction Updates 👈 ADD THIS ---
      this.currentReactionsSubscription = this.stompClient.subscribe(`/topic/page/${pageId}/reactions`, (message) => {
        const reactionEvent = JSON.parse(message.body);
        console.log("❤️ Incoming live reaction caught:", reactionEvent);

        this.updateWordReactionCount(reactionEvent.wordId, reactionEvent.reactionType, reactionEvent.totalCount);
      });
    },
    updateWordReactionCount(wordId, reactionType, totalCount) {
      if (!this.records) return;

      let current = this.records.firstWord;
      while (current) {
        if (Number(current.id) === Number(wordId)) {
          if (reactionType === 'LIKE') {
            current.likeCount = totalCount;
          } else if (reactionType === 'DISLIKE') {
            current.dislikeCount = totalCount;
          }
          break;
        }
        current = current.nextWord;
      }

      this.records = { ...this.records };
    },

    // 👈 ADD THIS: Dispatches reactions to the /app/send-reaction destination
    sendReactionViaWebSocket(wordId, currentPageId, reactionType) {
      if (this.stompClient && this.stompClient.connected) {
        const payload = {
          wordId: wordId,
          currentPageId: currentPageId,
          reactionType: reactionType
        };
        this.stompClient.publish({
          destination: '/app/send-reaction',
          body: JSON.stringify(payload)
        });
        console.log("📡 Dispatched reaction action to backend:", payload);
      } else {
        console.error("STOMP Client disconnected. Cannot send reaction.");
      }
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
        nextWord: null,
        authorName: newWord.author ? newWord.author.username : 'Anonymous',
        likeCount: 0,
        dislikeCount: 0
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

    deleteWordFromRecords(payload) {
      if (!this.records) {
        return;
      }

      let previousWordId = payload.previousWordId;
      let nextWord = payload.nextWord;

      let firstWordOfNextPage = this.records.lastWord?.nextWordId;
      if (nextWord == null) {
        // This is the last word of the current page.
        this.records.lastWord = null;
      }

      if (this.records.lastWordIdOfPreviousPage === previousWordId) {
        this.records.firstWord = this.records.firstWord.nextWord;
      } else {
        let current = this.records.firstWord;
        let prev = null;
        while (current) {
          if (current.id === previousWordId) {
            // const temp = current?.nextWord?.nextWord;
            current.nextWord = nextWord;
            // nextWord.nextWord = temp?.nextWord;
            if (!this.records.lastWord) {
              this.records.lastWord = {
                "id": current.id,
                "content": current.content,
                "nextWordId": firstWordOfNextPage,
                "previousWordId": prev.id
              }
            }
            break;
          }
          prev = current;
          current = current.nextWord;
        }
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
          destination: '/app/send-word',
          body: JSON.stringify(payload) // Map becomes standard JSON string
        });
        console.log("📡 Dispatched object to backend:", payload);
      } else {
        console.error("STOMP Client disconnected. Cannot send payload.");
      }
    },
    // Sends a structured JSON payload
    deleteWordViaWebSocket(payload) {
      if (this.stompClient && this.stompClient.connected) {
        this.stompClient.publish({
          destination: '/app/delete-word',
          body: JSON.stringify(payload) // Map becomes standard JSON string
        });
        console.log("📡 Dispatched object for deletion to backend:", payload);
      } else {
        console.error("STOMP Client disconnected. Cannot send payload.");
      }
    },

    async fetchPage(id) {
      this.loading = true;
      this.error = null;
      try {
        // 1. Grab the token exactly like you do for WebSockets
        const token = localStorage.getItem('token');

        // 2. Set up headers dynamically if the token exists
        const config = {};
        if (token) {
          config.headers = {
            Authorization: `Bearer ${token}`
          };
        }

        // 3. Pass the config with headers to your GET request
        const response = await axios.get(`${API_BASE_URL}/api/pages/${id}`, config);

        this.records = response.data;
        this.nextPageFirstWordId = this.records.lastWord?.nextWordId;

        if (response.data.totalPages) {
          this.totalPages = response.data.totalPages;
        }

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
    // 🚀 ADD THIS ACTION INSIDE YOUR ACTIONS BLOCK
    async findMigratedWordPage(wordId) {
      try {
        const token = localStorage.getItem('token');
        const config = {};
        if (token) {
          config.headers = {
            Authorization: `Bearer ${token}`
          };
        }

        const response = await axios.get(
          `${API_BASE_URL}/api/words/${wordId}/page`,
          config
        );

        // Return the target page ID directly to the calling component
        return response.data.pageId;
      } catch (err) {
        console.error("Could not trace migrated word anchor location:", err);
        throw err;
      }
    },
    // A fast, non-crypto UUIDv4 look-alike generator for HTTP
    // TODO: make collisions less likely somehow
    generateSimpleId() {
      return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
      });
    }
  }
});
