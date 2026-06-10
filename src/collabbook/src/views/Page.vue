<template>
  <div class="mode-toggle">
    <router-link v-if="!isEditMode" :to="`/pages/${id}/edit`" class="btn">✏️ Edit Page</router-link>
    <router-link v-else :to="`/pages/${id}`" class="btn">👁️ View Page</router-link>
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
  <span v-if="isEditMode" class="plus-sign">
    <Plus
      :previous="lastWordIdOfPreviousPage"
      :next="displayedWords[0]?.id || firstWord?.id"
      :autoFocus="activePlusId === 'front'"
      @submitWordStream="handleWordStreamSubmit"
      @close="activePlusId = null"
    />
  </span>

  <template v-for="(word, index) in displayedWords" :key="word.id">
    <span :id="word.id">
      <Word 
        :data="word" 
        :disabled="!!word.isPending && isSaving" 
      />
    </span>

    <span v-if="isEditMode && word.showPlus" class="plus-sign">
      <Plus
        :previous="word.id"
        :next="displayedWords[index + 1]?.id || word.nextWordId"
        :autoFocus="activePlusId === word.id"
        @submitWordStream="handleWordStreamSubmit"
        @close="activePlusId = null"
      />
    </span>
  </template>
</div>
</template>

<script setup>
import { onMounted, ref, computed, watch, nextTick } from 'vue';
import { usePageStore } from '@/stores/pageStore';
import Word from '@/components/Word.vue';
import Plus from '@/components/Plus.vue';

// Define the incoming props from Vue Router
const props = defineProps({
  id: { type: String, required: true },
  isEditMode: { type: Boolean, default: false },
  isDebugMode: { type: Boolean, default: false }
});

const pageStore = usePageStore();
const dbError = computed(() => pageStore.error);
// const displayedWords = ref([]);
const firstWord = ref(null);
const lastWordIdOfPreviousPage = ref(null);
const isSaving = ref(false);

const debounceTimer = ref(null);
const streamAnchorPrevious = ref(null); // The original stable ID where the sentence started
const streamAnchorNext = ref(null);     // The original next ID ahead of the sentence
const pendingStream = ref([]);          // Holds the text values typed in this session

const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));

// Tracks which plus component should automatically open up next
const activePlusId = ref(null);

// 🌟 THE SINGLE SOURCE OF TRUTH: 
// This computed array updates automatically whenever pageStore.records changes!
const displayedWords = computed(() => {
  const result = [];
  if (!pageStore.records?.firstWord) return result;

  let current = pageStore.records.firstWord;
  while (current) {
    result.push({
      id: Number(current.id),
      content: current.content,
      nextWordId: current.nextWord?.id ? Number(current.nextWord.id) : null,
      showPlus: true,
      isPending: false // Confirmed stored elements
    });
    current = current.nextWord;
  }

  // 🔄 OPTIMISTIC LAYER: If the user is typing, smoothly inject their temporary words
  // right into the computed display output until the store action returns.
  if (pendingStream.value.length > 0) {
    const tempWordsFormatted = pendingStream.value.map((content, idx) => ({
      id: `temp_stream_${idx}`,
      content: content,
      showPlus: true,
      isPending: true
    }));

    if (!streamAnchorPrevious.value) {
      result.unshift(...tempWordsFormatted);
    } else {
      const anchorIdx = result.findIndex(w => Number(w.id) === Number(streamAnchorPrevious.value));
      if (anchorIdx !== -1) {
        result.splice(anchorIdx + 1, 0, ...tempWordsFormatted);
      }
    }
  }

  return result;
});

const handleWordStreamSubmit = (data) => {
  const { content, previous, next } = data;

  if (pendingStream.value.length === 0) {
    streamAnchorPrevious.value = previous;
    streamAnchorNext.value = next;
  }

  // Pushing to this array instantly re-calculates the computed 'displayedWords' layout!
  pendingStream.value.push(content);

  // Set the next active input focus destination indicator
  activePlusId.value = `temp_stream_${pendingStream.value.length - 1}`;

  if (debounceTimer.value) clearTimeout(debounceTimer.value);
  debounceTimer.value = setTimeout(() => {
    sendStreamToBackend();
  }, 2000);
};

const sendStreamToBackend = async () => {
  if (pendingStream.value.length === 0) return;

  const wordsToSend = [...pendingStream.value];
  const previousAnchor = streamAnchorPrevious.value;

  try {
    isSaving.value = true;

    // This updates the central Pinia data model cache directly
    const savedWords = await pageStore.addBulkWords({
      pageId: props.id,
      previousWordId: previousAnchor,
      words: wordsToSend
    });

    // Reset local queue positions
    pendingStream.value = [];
    streamAnchorPrevious.value = null;
    streamAnchorNext.value = null;

    // Set focus to the last saved word
    if (savedWords.length > 0) {
      activePlusId.value = savedWords[savedWords.length - 1].id;
    }

  } catch (error) {
    console.error("Failed stream synchronization:", error);
  } finally {
    isSaving.value = false;
  }
};

// Instantly load all words into the array for immediate editing
const loadWords = async (streamWordsInRealTime, loadPlusSigns) => {
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
    showPlus: false,
    isPending: false
  });

    if (streamWordsInRealTime) {
      displayedWords.value = [...result];
      await delay(30);
    }
    
    currentWord = currentWord.nextWord;
  }

  if (!streamWordsInRealTime) {
    displayedWords.value = result;
  }

  if (loadPlusSigns) {
    for (const word of displayedWords.value) {
      await delay(40);
      word.showPlus = true;
    }
  }
};

// Central logic initializer
const initializePage = async () => {
  // Use props.id dynamically instead of hardcoding page one
  await pageStore.fetchPage(props.id);

  if (props.isEditMode) {
    loadWords(false, true);
  } else {
    loadWords(true, false);
  }
};

onMounted(() => {
  initializePage();
});

// Watch for route switches (e.g., clicking 'Edit' while viewing)
watch(() => props.isEditMode, () => {
  if (props.isEditMode) {
    loadWords(false, true);
  } else {
    loadWords(true, false);
  }
});
</script>

<style scoped>
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
