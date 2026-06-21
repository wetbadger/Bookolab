// src/stores/pageStore.js
import { defineStore } from 'pinia';
import { Client } from '@stomp/stompjs';
import axios from 'axios';
import { useAuthStore } from './authStore';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;
// Convert http/https to ws/wss dynamically
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
    userReactionCounts: {
      likesReceived: 0,
      dislikesReceived: 0
    },
    userReactionsSubscription: null,
    globalCounter: 0 // TODO: use for id generation
  }),
  actions: {
    initializeTestWebSocket() {
      if (this.stompClient) return;

      // Assuming your JWT is saved in localStorage or another store
      const token = localStorage.getItem('token');
      const authStore = useAuthStore();

      this.stompClient = new Client({
        brokerURL: WS_BASE_URL,
        reconnectDelay: 5000,
        heartbeatIncoming: 7000,
        heartbeatOutgoing: 10000,
        connectHeaders: {
          Authorization: token ? `Bearer ${token}` : ''
        },
        debug: function (str) {
          // console.log('STOMP: ' + str);
        },
      });

      // CATCH THE RATE LIMIT ERROR FRAME BEFORE DISCONNECT
      this.stompClient.onStompError = (frame) => {
        const errorMessage = frame.headers['message'] || '';
        const errorBody = frame.body || '';

        console.error('📡 STOMP Broker Error caught:', errorMessage);
      };

      // HANDLE THE CLOSURE GRACEFULLY
      this.stompClient.onWebSocketClose = (closeEvent) => {
        // FIX: Ignore clean closures (code 1000 means it was intentionally closed)
        if (closeEvent.code === 1000 || closeEvent.wasClean) {
          return;
        }
        // If it's an unclean drop, set the error state
        if (!this.error) {
          this.error = "Connection lost. Attempting to reconnect...";
        }
      };

      this.stompClient.onConnect = (frame) => {
        // console.log('🎉 Connected to Spring STOMP Broker!');
        this.error = null;
        const serverUser = frame.headers['user-name'];
        if (serverUser === 'anonymousUser') {
          localStorage.removeItem('token');
        }

        // 🚀 SUBSCRIBE TO PRIVATE RATE LIMITS & USER ERRORS
        this.stompClient.subscribe('/user/queue/errors', (message) => {
          // Set your Pinia state error message so your UI components can read and render it
          this.error = message.body;
          console.warn("⚠️ Rate limit message received from server:", message.body);

          // Optional: Clear the error notification from the UI after 5 seconds automatically
          setTimeout(() => {
            if (this.error === message.body) {
              this.error = null;
            }
          }, 5000);
        });

        // SUBSCRIBE TO USER'S OWN REACTION STATS
        this.stompClient.subscribe('/user/queue/reaction-stats', (message) => {
          const stats = JSON.parse(message.body);
          this.userReactionCounts = {
            likesReceived: stats.likesReceived || 0,  // Updated field name
            dislikesReceived: stats.dislikesReceived || 0  // Updated field name
          };
        });

        // SUBSCRIBE TO USER'S OWN CREDITS SPENT
        this.stompClient.subscribe('/user/queue/credits-spent', (message) => {
          const creditsSpent = JSON.parse(message.body);
          authStore.user.creditsSpent = creditsSpent;
        });

        // Request initial stats for the authenticated user
        // (This gets the user's OWN stats - how many likes/dislikes they've received)
        if (authStore.user) {
          this.stompClient.publish({
            destination: '/app/get-my-reaction-stats',
            body: JSON.stringify({})
          });
        }

        if (this.records?.id) {
          this.subscribeToPageTopic(this.records.id);
        }

        // SUBSCRIBE TO GLOBAL STRUCTURAL UPDATES
        if (!this.globalUpdatesSubscription) {
          this.globalUpdatesSubscription = this.stompClient.subscribe('/topic/global-updates', (message) => {
            const payload = JSON.parse(message.body);
            if (payload.type === "GLOBAL_REPAGINATION") {
              this.totalPages = payload.totalPages;
              this.truncationEventTrigger++;
            }
          });
        }
      };

      this.stompClient.activate();
    },
    // Safe disconnection action
    disconnectWebSocket() {
      if (this.stompClient) {
        // 1. Only attempt to transmit unsubscribe frames if the client is actively online
        if (this.stompClient.connected) {
          if (this.currentSubscription) this.currentSubscription.unsubscribe();
          if (this.currentReactionsSubscription) this.currentReactionsSubscription.unsubscribe();
          if (this.globalUpdatesSubscription) this.globalUpdatesSubscription.unsubscribe();
          if (this.userReactionsSubscription) this.userReactionsSubscription.unsubscribe();
        }

        // 2. Wipe out the stale references from Pinia memory completely
        this.currentSubscription = null;
        this.currentReactionsSubscription = null;
        this.globalUpdatesSubscription = null;
        this.userReactionsSubscription = null;

        this.stompClient.deactivate();
        this.stompClient = null;
      }
    },

    unsubscribeFromUserReactions() {
      if (this.userReactionsSubscription) {
        this.userReactionsSubscription.unsubscribe();
        this.userReactionsSubscription = null;
      }
    },

    // Forced reconnect action to break out of the early return block
    reconnectWebSocket() {
      // console.log('🔄 Forcing WebSocket reconnection with fresh credentials...');
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
        // console.log("🔥 Incoming WebSocket payload caught:", inboundPayload);

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
        // console.log("❤️ Incoming live reaction caught:", reactionEvent);

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

    // Dispatches reactions to the /app/send-reaction destination
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
        // console.log("📡 Dispatched reaction action to backend:", payload);
      } else {
        // console.error("STOMP Client disconnected. Cannot send reaction.");
      }
    },
    // Processes cross-page linking updates from neighboring tabs
    handleCrossPageBoundaryPatch(patch) {
      if (!this.records) return;

      if (patch.type === "NEXT_PAGE_HEAD_CHANGED") {
        // console.log(`📡 Patching out-bound link pointer -> newNextWordId: ${patch.newNextWordId}`);
        // 1. Update the metadata link tracking pointer
        this.nextPageFirstWordId = patch.newNextWordId;

        // 2. Patch the actual active lastWord state instance object link directly
        if (this.records.lastWord) {
          this.records.lastWord.nextWordId = patch.newNextWordId;
        }
      }

      else if (patch.type === "PREVIOUS_PAGE_TAIL_CHANGED") {
        // console.log(`📡 Patching in-bound fallback anchor -> newLastWordIdOfPreviousPage: ${patch.newLastWordIdOfPreviousPage}`);
        // Update the fallback tracking pointer
        this.records.lastWordIdOfPreviousPage = patch.newLastWordIdOfPreviousPage;
      }
    },

    // Manipulate the linked list records directly inside Pinia
// Manipulate the linked list records directly inside Pinia
    insertWordIntoRecords(newWord) {
      if (!this.records) return;

      // 1. Conflict Resolution: Prevent duplicate insertions
      if (this.findWordInRecords(newWord.id || newWord.localId)) {
        return;
      }

      // 2. Build a nested-compatible node, but keep nextWordId flat for the UI components
      const newNode = {
        id: newWord.id,
        localId: newWord.localId || null, // Preserve client-side temporary ID if it exists
        content: newWord.content,
        nextWord: null, // Will hold the nested object link
        nextWordId: newWord.nextWordId, // Will hold the flat ID for Vue props
        authorName: newWord.authorName || newWord.authorUsername || 'Anonymous',
        likeCount: newWord.likeCount || 0,
        dislikeCount: newWord.dislikeCount || 0
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
      if (newWord.nextWordId && Number(this.records.firstWord.id) === Number(newWord.nextWordId)) {
        newNode.nextWord = this.records.firstWord;
        this.records.firstWord = newNode;
        return;
      }

      // 5. CASE C: Append to the very end of THIS page
      const isPointingToNextPage = newWord.nextWordId &&
        Number(newWord.nextWordId) === Number(this.nextPageFirstWordId);

      if (!newWord.nextWordId || isPointingToNextPage) {
        let tail = this.records.firstWord;
        while (tail.nextWord) {
          tail = tail.nextWord;
        }

        // Nest the object link
        tail.nextWord = newNode;
        // Also update the flat pointer so the template doesn't miss it
        tail.nextWordId = newNode.id;

        this.records.lastWord = {
          id: newNode.id,
          content: newNode.content,
          nextWordId: this.nextPageFirstWordId || null,
          previousWordId: tail.id
        };
        return;
      }

      // 6. CASE D: Mid-chain Splicing (Splicing between existing words)
      let current = this.records.firstWord;
      while (current) {
        // Trace using the nested .nextWord object pointer, matching against the new flat nextWordId
        if (current.nextWord && Number(current.nextWord.id) === Number(newWord.nextWordId)) {
          const oldNext = current.nextWord;

          // Splice the new node in natively
          current.nextWord = newNode;
          current.nextWordId = newNode.id; // Sync flat pointer

          newNode.nextWord = oldNext;
          break;
        }
        current = current.nextWord;
      }
    },

    deleteWordFromRecords(payload) {
      if (!this.records || !this.records.firstWord) {
        return;
      }

      const { previousWordId, nextWord } = payload;
      const firstWordOfNextPage = this.records.lastWord?.nextWordId ?? null;

      // Case 1: Deleting the very first word of this page
      if (this.records.lastWordIdOfPreviousPage === previousWordId) {
        this.records.firstWord = this.records.firstWord.nextWord;

        // Edge case: page is now completely empty
        if (!this.records.firstWord) {
          this.records.lastWord = null;
        }
        return;
      }

      // Case 2: Deleting a word inside the page or the last word
      let current = this.records.firstWord;
      let prev = null;

      while (current) {
        // Find the word BEFORE the one we want to delete
        if (current.id === previousWordId) {

          // Target the word to be deleted
          let wordToDelete = current.nextWord;

          if (wordToDelete) {
            // Link current word to the word AFTER the deleted one, preserving the chain
            current.nextWord = wordToDelete.nextWord;
          } else {
            // Edge case: nothing was after current anyway
            current.nextWord = null;
          }

          // Rebuild lastWord if we just deleted the end of the list
          if (current.nextWord === null) {
            this.records.lastWord = {
              "id": current.id,
              "content": current.content,
              "nextWordId": firstWordOfNextPage,
              "previousWordId": prev ? prev.id : this.records.lastWordIdOfPreviousPage,
              "dislikeCount": current.dislikeCount,
              "likeCount": current.likeCount
            };
          }
          break;
        }
        prev = current;
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
          destination: '/app/send-word',
          body: JSON.stringify(payload) // Map becomes standard JSON string
        });
        // console.log("📡 Dispatched object to backend:", payload);
      } else {
        // console.error("STOMP Client disconnected. Cannot send payload.");
      }
    },
    // Sends a structured JSON payload
    deleteWordViaWebSocket(payload) {
      if (this.stompClient && this.stompClient.connected) {
        this.stompClient.publish({
          destination: '/app/delete-word',
          body: JSON.stringify(payload) // Map becomes standard JSON string
        });
        // console.log("📡 Dispatched object for deletion to backend:", payload);
      } else {
        // console.error("STOMP Client disconnected. Cannot send payload.");
      }
    },

    async fetchPage(id) {
      this.loading = true;
      this.error = null;
      try {
        const token = localStorage.getItem('token');
        const config = {};
        if (token) {
          config.headers = { Authorization: `Bearer ${token}` };
        }

        const response = await axios.get(`${API_BASE_URL}/api/pages/${id}`, config);

        this.records = response.data;
        this.nextPageFirstWordId = this.records.lastWord?.nextWordId;

        if (response.data.totalPages) {
          this.totalPages = response.data.totalPages;
        }

        this.subscribeToPageTopic(id);

      } catch (err) {
        console.error("Failed to fetch page data:", err);

        // Check if the server returned a 429 status code
        if (err.response && err.response.status === 429) {
          this.error = "Too many requests. Please slow down and try again later.";
        } else {
          this.error = "Database Connection Failed";
        }
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
    getCredits() {
      const authStore = useAuthStore(); // ✅ Resolve directly during evaluation

      const likes = this.userReactionCounts.likesReceived || 0;
      const dislikes = this.userReactionCounts.dislikesReceived || 0;
      const spent = authStore.user?.creditsSpent || 0;
      return likes - dislikes - spent;
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
