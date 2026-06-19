<template>
  <nav>
    <ul class="nav">
      <li><Paginator /></li>
      <li><router-link to="/leaderboard" class="leaderboard-btn">🏆</router-link></li>
      <li v-if="isAuthenticated"><Profile /></li>
    </ul>
  </nav>
  <div class="mode-toggle">
    <!-- Use a click handler instead of a direct router-link path -->
    <button v-if="!isEditMode" @click="handleEditClick" class="btn">
      ✏️ Edit Page
    </button>

    <router-link v-else :to="`/pages/${id}`" class="btn">
      👁️ View Page
    </router-link>
  </div>

  <div v-show="isDebugMode">
    {{ pageStore.records }}
  </div>

  <div v-if="dbError" class="error-alert">
    ⚠️ Error: {{ dbError }}
  </div>

  <div v-else-if="pageStore.loading && displayedWords.length === 0">
    Loading database values...
  </div>

  <div v-else class="sentence-container">
    <span v-if="isReallyEditing" class="plus-sign">
      <Plus
        :ref="(el) => { if (el) plusRefs['start'] = el }"
        :previous="lastWordIdOfPreviousPage ? Number(lastWordIdOfPreviousPage) : null"
        :next="typeof firstWord?.id === 'number' ? firstWord.id : null"
        :previousLocalId="null"
        :currentPageId="props.id"
        @submit="(data) => handleWordSubmit(data, 0)"
      />
    </span>

    <template v-for="(word, index) in displayedWords" :key="word.viewKey">
      <span :id="word.id">
        <Word
          :data="word"
          @react="sendReactionToWebSocket"
          :isAuthenticated="isAuthenticated"
          :currentPageId="props.id"
          :wordId="word.id"
        />
      </span>

      <span v-if="isReallyEditing && word.showPlus" class="plus-sign">
        <Plus
          :ref="(el) => { if (el) plusRefs[word.viewKey] = el }"
          :previous="typeof word.id === 'number' ? word.id : null"
          :next="typeof displayedWords[index + 1]?.id === 'number' ? displayedWords[index + 1].id : null"
          :previousLocalId="word.localId"
          :currentPageId="props.id"
          @submit="(data) => handleWordSubmit(data, index + 1)"
        />
      </span>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref, computed, watch, nextTick, onBeforeUpdate, watchEffect } from 'vue'; // Added watchEffect
import { useRouter, useRoute } from 'vue-router'; // Add this import

import { usePageStore } from '@/stores/pageStore';
import { useAuthStore } from '@/stores/authStore'; // Import the new store
import Word from '@/components/Word.vue';
import Plus from '@/components/Plus.vue';
import Profile from '@/components/Profile.vue';
import Paginator from "@/components/Pagenator.vue";

const props = defineProps({
  id: { type: Number, required: true },
  isEditMode: { type: Boolean, default: false },
  isDebugMode: { type: Boolean, default: false }
});

const pageStore = usePageStore();
const authStore = useAuthStore();
const dbError = computed(() => pageStore.error);
const displayedWords = ref([]);
const firstWord = ref(null);
const lastWordIdOfPreviousPage = ref(null);
const router = useRouter(); // Initialize router
const route = useRoute();
const plusRefs = ref({});

onBeforeUpdate(() => {
  plusRefs.value = {};
});

const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));

// Use the store's reactive validation state
const isAuthenticated = computed(() => authStore.isAuthenticated);
const isReallyEditing = computed(() => props.isEditMode && isAuthenticated.value);

const handleEditClick = () => {
  if (isAuthenticated.value) {
    router.push(`/pages/${props.id}/edit`);
  } else {
    router.push({
      path: '/login',
      query: { redirectFrom: `/pages/${props.id}/edit` }
    });
  }
};

const loadWords = async (streamWordsInRealTime, loadPlusSigns, isInstant = false) => {
  const result = [];
  firstWord.value = pageStore.records?.firstWord || null;
  lastWordIdOfPreviousPage.value = pageStore.records?.lastWordIdOfPreviousPage;
  let currentWord = firstWord.value;

  let delay1 = 30;
  let delay2 = 40;

  while (currentWord) {
    const wordId = currentWord.id ? currentWord.id : lastWordIdOfPreviousPage.value;
    result.push({
      id: Number(wordId),
      content: currentWord.content,
      nextWordId: currentWord?.nextWord?.id ? Number(currentWord.nextWord.id) : null,
      showPlus: isInstant, // If instant mode, show the plus signs immediately!
      localId: currentWord.localId || null, // Keep the tracking ID bound if it exists
      viewKey: currentWord.localId ? currentWord.localId : 'db-' + Number(wordId),
      likeCount: currentWord.likeCount || 0,
      dislikeCount: currentWord.dislikeCount || 0,
      authorName: currentWord.authorName || 'Anonymous',
      userLiked: currentWord.userLiked,
      userDisliked: currentWord.userDisliked
    });

    // If it's an initial view stream and NOT instant mode, add artificial delay
    if (streamWordsInRealTime && !isInstant) {
      displayedWords.value = [...result];
      if (props.isEditMode) {
        // Switched to edit mode mid load
        loadPlusSigns = true;
        delay1 = 0;
      }
      await delay(delay1);
    }
    currentWord = currentWord.nextWord;
  }

  // If we aren't streaming or we are in instant mode, swap the array whole-cloth
  if (!streamWordsInRealTime || isInstant) {
    displayedWords.value = result;
  }

  // Only run the staggered plus sign animation if we aren't doing an instant layout update
  if (loadPlusSigns && !isInstant) {
    for (const word of displayedWords.value) {
      await delay(delay2);
      word.showPlus = true;
    }
  }
};

const handleWordSubmit = async (data, originIndex) => {
  const { id, localId, content, previous, next, previousLocalId } = data;

  const newWordObj = {
    id: id ? Number(id) : null,
    localId: localId,
    content: content,
    nextWordId: next ? Number(next) : null,
    showPlus: true,
    viewKey: localId // For newly typed words, the localId string doubles perfectly as a viewKey
  };

  if (originIndex === 0) {
    displayedWords.value.unshift(newWordObj);
  } else {
    let targetIndex = -1;

    if (previousLocalId) {
      targetIndex = displayedWords.value.findIndex(word => word.localId === previousLocalId);
    }

    if (targetIndex === -1 && previous !== null) {
      targetIndex = displayedWords.value.findIndex(word => Number(word.id) === Number(previous));
    }

    if (targetIndex !== -1) {
      if (displayedWords.value[targetIndex].id) {
        displayedWords.value[targetIndex].nextWordId = newWordObj.id;
      }
      displayedWords.value.splice(targetIndex + 1, 0, newWordObj);
    } else {
      displayedWords.value.push(newWordObj);
    }
  }

  await nextTick();

  // Target focus by our designated viewKey mapping value
  if (plusRefs.value[newWordObj.viewKey]) {
    plusRefs.value[newWordObj.viewKey].focusInnerInput();
  }
};

const initializePage = async () => {
  await pageStore.fetchPage(props.id);

  // CHECK FOR MIGRATION QUERY PARAMS
  // If 'word' is present in the URL, force instant rendering mode (true)
  const hasMigrationParams = !!route.query.word;

  // Arguments mapping:
  // 1. streamWordsInRealTime = !props.isEditMode && !hasMigrationParams
  // 2. loadPlusSigns = props.isEditMode
  // 3. isInstant = hasMigrationParams || standard_default (false for streaming view)
  await loadWords(
    !props.isEditMode && !hasMigrationParams,
    props.isEditMode,
    hasMigrationParams // ⚡ Sets isInstant to true dynamically
  );

  // Fire off the input element targeting block
  applyUrlQueryStates();
};

onMounted(() => {
  pageStore.initializeTestWebSocket();
  initializePage();
});

// Your deep watcher handles all the heavy lifting!
// Anytime pageStore.records changes (via WebSocket state manipulation),
// it sweeps through and instantly updates displayedWords without staggering animations.
watch(
  () => pageStore.records,
  (newRecords) => {
    if (newRecords) {
      // console.log("🔄 Store records mutation detected. Updating display array instantly...");

      // Arguments:
      // 1. streamWords = false
      // 2. loadPlusSigns = false (handled by instant flag)
      // 3. isInstant = true ⚡
      loadWords(false, false, true);
    }
  },
  { deep: true }
);

// Add this right near your other handoff handlers in Page.vue
const sendReactionToWebSocket = (wordId, reactionType) => {
  // Pass along the target word ID, the page scope ID, and the 'LIKE'/'DISLIKE' action string
  pageStore.sendReactionViaWebSocket(wordId, props.id, reactionType);
};

const applyUrlQueryStates = async () => {
  const targetWordId = route.query.word;
  const initialContent = route.query.content;

  // If there are no migration query keys present, skip everything safely
  if (!targetWordId) return;

  // 1. Wait for Vue to physically render the newly modified array layout elements
  await nextTick();

  // console.log(`🎯 URL query detected! Attempting to restore input behind word: ${targetWordId}`);

  // 2. Scan your active layout refs to locate the matching box tracking position
  for (const key in plusRefs.value) {
    const plusInstance = plusRefs.value[key];

    if (plusInstance) {
      const anchorId = plusInstance.getPrevious();

      // Check if this plus component matches our target anchor word ID
      if (anchorId && String(anchorId) === String(targetWordId)) {
        // 3. Force-open the component layout input field and focus it
        plusInstance.focusInnerInput();

        // 4. Fill the input field with your preserved text content string
        if (initialContent) {
          plusInstance.setInputValue(decodeURIComponent(initialContent));
        }

        // Clean up the URL query parameters so refreshing doesn't keep force-opening it
        router.replace({ path: route.path, query: {} });
        break;
      }
    }
  }
};

watch(() => props.isEditMode, () => {
  loadWords(!props.isEditMode, props.isEditMode);
  if (props.isEditMode) {
    applyUrlQueryStates();
  }
});

// WATCH THE ROUTE ID: Triggered when user clicks a pagination number
watch(() => props.id, () => {
  initializePage();
});

// WATCH FOR GLOBAL CRON RE-PAGINATION EVENTS
watch(
  () => pageStore.truncationEventTrigger,
  async () => {
    // console.log("⚡ Database repagination detected! Relocating active focus rules...");

    let activelyEditingWordId = null;
    let unsavedText = '';

    for (const key in plusRefs.value) {
      const plusInstance = plusRefs.value[key];

      if (plusInstance && plusInstance.isComponentEditing) {
        activelyEditingWordId = plusInstance.getPrevious();
        unsavedText = plusInstance.getInputValue();
        break;
      }
    }

    // 2. Fetch the updated state profile for our current position
    await pageStore.fetchPage(props.id);

    // 3. CASE A: User has an active edit box open. Follow the word!
    if (activelyEditingWordId) {
      // console.log(`Searching for newly displaced editing anchor word: ${activelyEditingWordId}`);

      const stillOnCurrentPage = pageStore.findWordInRecords(activelyEditingWordId);

      if (!stillOnCurrentPage) {
        // console.log("Word moved pages. Fetching new location from store...");
        try {
          // DISPATCH TO PINIA STORE ACTION
          const targetPageId = await pageStore.findMigratedWordPage(activelyEditingWordId);

          if (targetPageId) {
            router.push(`/pages/${targetPageId}/edit?word=${activelyEditingWordId}&content=${encodeURIComponent(unsavedText)}`);
            return;
          }
        } catch (err) {
          console.error("Fallback mitigation failed during page tracking trace.");
        }
      }
    }

    // 4. CASE B: No edit box open, but the total pages shrank below our current view index
    if (props.id > pageStore.totalPages) {
      // console.log(`Page ${props.id} no longer exists. Redirecting down to final tail page: ${pageStore.totalPages}`);
      const suffix = props.isEditMode ? '/edit' : '';
      router.push(`/pages/${pageStore.totalPages}${suffix}`);
    } else {
      loadWords(!props.isEditMode, props.isEditMode, true);
    }
  }
);
</script>

<style scoped>
.nav {
  display: flex;
  justify-content: space-around;
  align-items: center;
  list-style: none;
  padding: 0;
  margin: 0;
}

.nav li {
  display: inline-block;
}

.leaderboard-btn {
  display: inline-block;
  padding: 8px 16px;
  color: #1a1a1a;
  text-decoration: none;
  border-radius: 20px;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(247, 151, 30, 0.3);
}

.leaderboard-btn:hover {
  box-shadow: 0 4px 12px rgba(247, 151, 30, 0.4);
}

.mode-toggle {
  margin-bottom: 20px;
}
.btn {
  display: inline-block;
  padding: 8px 16px;
  background-color: #007bff;
  color: white;
  text-decoration: none;
  border-radius: 4px;
}
.sentence-container {
  font-size: 1.2rem;
  line-height: 1.5;
  padding: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.word-input {
  width: 70px;
  padding: 4px;
  font-size: 1.1rem;
  text-align: center;
}
.error-alert {
  color: #721c24;
  background-color: #f8d7da;
  border: 1px solid #f5c6cb;
  padding: 10px;
  border-radius: 4px;
}

/* Responsive adjustments */
@media (max-width: 600px) {
  .nav {
    flex-wrap: wrap;
    gap: 8px;
    justify-content: center;
  }

  .leaderboard-btn {
    font-size: 0.9rem;
    padding: 6px 12px;
  }
}
</style>
