<template>
  <nav>
    <ul class="nav">
      <li><Paginator /></li>
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
import { useRouter } from 'vue-router'; // Add this import

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
      await delay(30);
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
      await delay(40);
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
  loadWords(!props.isEditMode, props.isEditMode);
};

const initializeSocketSafely = () => {
  // If the user is authenticated and we have a token, connect right away
  if (authStore.isAuthenticated && authStore.token) {
    pageStore.initializeTestWebSocket();
  } else {
    // If we're hitting a race condition, wait for the token to arrive
    const unwatch = watch(
      () => authStore.token,
      (newToken) => {
        if (newToken) {
          console.log("🔑 Valid token detected! Initializing WebSocket safely...");
          pageStore.initializeTestWebSocket();
          unwatch(); // Destroy the watcher so we don't connect multiple times
        }
      }
    );
  }
};

onMounted(() => {
  initializeSocketSafely(); // Protects against the authentication race condition
  initializePage();
});

// Your deep watcher handles all the heavy lifting!
// Anytime pageStore.records changes (via WebSocket state manipulation),
// it sweeps through and instantly updates displayedWords without staggering animations.
watch(
  () => pageStore.records,
  (newRecords) => {
    if (newRecords) {
      console.log("🔄 Store records mutation detected. Updating display array instantly...");

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

watch(() => props.isEditMode, () => {
  loadWords(!props.isEditMode, props.isEditMode);
});

// 2. WATCH THE ROUTE ID: Triggered when user clicks a pagination number
watch(() => props.id, () => {
  initializePage();
});
</script>

<style scoped>
.nav {
  display: flex;
  justify-content: space-around;
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
</style>
